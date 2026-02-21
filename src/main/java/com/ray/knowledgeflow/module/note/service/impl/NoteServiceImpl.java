package com.ray.knowledgeflow.module.note.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ray.knowledgeflow.common.exception.BusinessException;
import com.ray.knowledgeflow.common.utils.MinioUtil;
import com.ray.knowledgeflow.module.content.entity.ContentItem;
import com.ray.knowledgeflow.module.content.mapper.ContentItemMapper;
import com.ray.knowledgeflow.module.content.mapper.ContentSourceMapper;
import com.ray.knowledgeflow.module.note.dto.*;
import com.ray.knowledgeflow.module.note.entity.NoteAttachment;
import com.ray.knowledgeflow.module.note.entity.NoteEntry;
import com.ray.knowledgeflow.module.note.entity.NoteFolder;
import com.ray.knowledgeflow.module.note.entity.NoteVersion;
import com.ray.knowledgeflow.module.note.mapper.NoteAttachmentMapper;
import com.ray.knowledgeflow.module.note.mapper.NoteEntryMapper;
import com.ray.knowledgeflow.module.note.mapper.NoteFolderMapper;
import com.ray.knowledgeflow.module.note.mapper.NoteVersionMapper;
import com.ray.knowledgeflow.module.note.service.INoteService;
import com.ray.knowledgeflow.module.note.service.NoteEngagementService;
import com.ray.knowledgeflow.module.ai.message.AiProcessProducer;
import com.ray.knowledgeflow.module.knowledge.service.GraphService;
import com.ray.knowledgeflow.module.search.message.IndexUpdateProducer;
import com.ray.knowledgeflow.module.search.service.ISearchService;
import com.ray.knowledgeflow.module.user.entity.SysUser;
import com.ray.knowledgeflow.module.user.mapper.UserMapper;
import com.ray.knowledgeflow.module.user.mapper.UserProfileMapper;
import com.ray.knowledgeflow.module.user.dto.UserPublicProfileDto;
import com.ray.knowledgeflow.module.user.entity.SysUserProfile;
import com.ray.knowledgeflow.module.user.service.IUserRelationService;
import com.ray.knowledgeflow.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 笔记服务实现类
 *
 * @author Ray
 * @since 2024-01-16
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NoteServiceImpl implements INoteService {

    private final NoteEntryMapper noteEntryMapper;
    private final NoteFolderMapper folderMapper;
    private final NoteVersionMapper versionMapper;
    private final NoteAttachmentMapper attachmentMapper;
    private final ContentItemMapper contentItemMapper;
    private final ContentSourceMapper contentSourceMapper;
    private final MinioUtil minioUtil;
    private final UserMapper userMapper;
    private final UserProfileMapper userProfileMapper;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    // 消息生产者
    private final AiProcessProducer aiProcessProducer;
    private final IndexUpdateProducer indexUpdateProducer;
    private final GraphService graphService;
    private final NoteEngagementService engagementService;
    private final ISearchService searchService;
    private final IUserRelationService userRelationService;

    // 笔记链接正则: [[笔记标题]]
    private static final Pattern LINK_PATTERN = Pattern.compile("\\[\\[([^!][^\\]]*?)\\]\\]");
    // 内容引用正则: [[!article:123]] 或 [[!article:123|标题]]
    private static final Pattern CONTENT_LINK_PATTERN = Pattern.compile("\\[\\[!article:(\\d+)(?:\\|([^\\]]+))?\\]\\]");
    private static final int MAX_VERSIONS = 20;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NoteDetailDto createNote(Long userId, CreateNoteDto dto) {
        // 验证文件夹
        if (dto.getFolderId() != null && dto.getFolderId() > 0) {
            NoteFolder folder = folderMapper.selectById(dto.getFolderId());
            if (folder == null || !folder.getUserId().equals(userId)) {
                throw new BusinessException(404, "文件夹不存在");
            }
        }

        NoteEntry note = new NoteEntry();
        note.setUserId(userId);
        note.setTitle(dto.getTitle());
        note.setContentMd(dto.getContentMd());
        note.setFolderId(dto.getFolderId());
        note.setType(dto.getType() != null ? dto.getType() : "NOTE");
        note.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        note.setIsPublic(dto.getIsPublic() != null ? dto.getIsPublic() : 0);

        // 生成摘要
        if (dto.getContentMd() != null && !dto.getContentMd().isEmpty()) {
            String plainText = Jsoup.parse(dto.getContentMd()).text();
            note.setSummary(plainText.length() > 200 ? plainText.substring(0, 200) + "..." : plainText);
        }

        noteEntryMapper.insert(note);

        // 发布保存事件（保留现有事件机制）
        eventPublisher.publishEvent(new com.ray.knowledgeflow.module.search.event.NoteSavedEvent(this, note));

        // 异步触发 AI 处理
        if (dto.getContentMd() != null && !dto.getContentMd().isEmpty()) {
            aiProcessProducer.sendSummaryTask(note.getId(), userId, note.getTitle(), dto.getContentMd());
            aiProcessProducer.sendTagExtractionTask(note.getId(), userId, note.getTitle(), dto.getContentMd());
            aiProcessProducer.sendGraphBuildTask(note.getId(), userId, note.getTitle(), dto.getContentMd());
        }

        // 异步更新搜索索引
        indexUpdateProducer.sendCreateIndex("NOTE", note.getId(), userId, note.getTitle(), dto.getContentMd());

        // 异步: 同步到知识图谱
        graphService.syncNoteToGraph(note);

        return getNoteDetail(userId, note.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NoteDetailDto updateNote(Long userId, Long noteId, UpdateNoteDto dto) {
        NoteEntry note = getAndCheckOwnership(userId, noteId);

        // 保存当前版本到历史
        saveVersion(note);

        // 更新笔记
        if (dto.getTitle() != null) {
            note.setTitle(dto.getTitle());
        }
        if (dto.getContentMd() != null) {
            note.setContentMd(dto.getContentMd());

            // 更新摘要
            String plainText = Jsoup.parse(dto.getContentMd()).text();
            note.setSummary(plainText.length() > 200 ? plainText.substring(0, 200) + "..." : plainText);
        }
        if (dto.getFolderId() != null) {
            note.setFolderId(dto.getFolderId());
        }
        if (dto.getType() != null) {
            note.setType(dto.getType());
        }
        if (dto.getStatus() != null) {
            note.setStatus(dto.getStatus());
        }
        if (dto.getIsPublic() != null) {
            note.setIsPublic(dto.getIsPublic());
        }

        noteEntryMapper.updateById(note);

        // 发布保存事件（保留现有事件机制）
        eventPublisher.publishEvent(new com.ray.knowledgeflow.module.search.event.NoteSavedEvent(this, note));

        // 异步触发 AI 处理（如果内容有更新）
        if (dto.getContentMd() != null && !dto.getContentMd().isEmpty()) {
            aiProcessProducer.sendSummaryTask(note.getId(), userId, note.getTitle(), note.getContentMd());
            aiProcessProducer.sendTagExtractionTask(note.getId(), userId, note.getTitle(), note.getContentMd());
            aiProcessProducer.sendGraphBuildTask(note.getId(), userId, note.getTitle(), note.getContentMd());
        }

        // 异步更新搜索索引
        indexUpdateProducer.sendUpdateIndex("NOTE", note.getId(), userId, note.getTitle(), note.getContentMd());

        // 异步: 同步到知识图谱
        graphService.syncNoteToGraph(note);

        return getNoteDetail(userId, noteId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteNote(Long userId, Long noteId) {
        NoteEntry note = getAndCheckOwnership(userId, noteId);

        // 删除版本历史
        versionMapper.delete(new LambdaQueryWrapper<NoteVersion>()
                .eq(NoteVersion::getNoteId, noteId));

        // 删除附件记录（实际文件保留在MinIO）
        attachmentMapper.delete(new LambdaQueryWrapper<NoteAttachment>()
                .eq(NoteAttachment::getNoteId, noteId));

        // 删除笔记
        noteEntryMapper.deleteById(noteId);

        // 发布删除事件（保留现有事件机制）
        eventPublisher.publishEvent(new com.ray.knowledgeflow.module.search.event.NoteDeletedEvent(this, noteId));

        // 异步删除搜索索引
        indexUpdateProducer.sendDeleteIndex("NOTE", noteId, userId);
    }

    @Override
    public NoteDetailDto getNoteDetail(Long userId, Long noteId) {
        NoteEntry note = getAndCheckOwnership(userId, noteId);

        NoteDetailDto dto = new NoteDetailDto();
        BeanUtils.copyProperties(note, dto);

        // 获取文件夹名称
        if (note.getFolderId() != null && note.getFolderId() > 0) {
            NoteFolder folder = folderMapper.selectById(note.getFolderId());
            if (folder != null) {
                dto.setFolderName(folder.getName());
            }
        }

        // 解析双向链接
        dto.setOutgoingLinks(parseOutgoingLinks(userId, note.getContentMd()));
        dto.setIncomingLinks(findIncomingLinks(userId, noteId, note.getTitle()));

        // 解析内容引用链接
        dto.setContentLinks(parseContentLinks(userId, note.getContentMd()));

        // 获取附件列表
        List<NoteAttachment> attachments = attachmentMapper.selectList(new LambdaQueryWrapper<NoteAttachment>()
                .eq(NoteAttachment::getNoteId, noteId)
                .orderByDesc(NoteAttachment::getCreatedTime));

        dto.setAttachments(attachments.stream()
                .map(this::toAttachmentDto)
                .collect(Collectors.toList()));

        return dto;
    }

    @Override
    public IPage<NoteListDto> listNotes(Long userId, Long folderId, Integer page, Integer size) {
        Page<NoteEntry> pageParam = new Page<>(page, size);

        LambdaQueryWrapper<NoteEntry> wrapper = new LambdaQueryWrapper<NoteEntry>()
                .eq(NoteEntry::getUserId, userId)
                .orderByDesc(NoteEntry::getUpdatedTime);

        if (folderId != null) {
            wrapper.eq(NoteEntry::getFolderId, folderId);
        }

        IPage<NoteEntry> notePage = noteEntryMapper.selectPage(pageParam, wrapper);

        // Convert to DTO without filling folder name yet
        IPage<NoteListDto> dtoPage = notePage.convert(this::toBasicListDto);

        // Batch fill folder names
        fillFolderNames(dtoPage.getRecords());

        return dtoPage;
    }

    @Override
    public IPage<NoteListDto> listPublicNotes(Long userId, Integer page, Integer size) {
        Page<NoteEntry> pageParam = new Page<>(page, size);

        LambdaQueryWrapper<NoteEntry> wrapper = new LambdaQueryWrapper<NoteEntry>()
                .eq(NoteEntry::getUserId, userId)
                .eq(NoteEntry::getIsPublic, 1) // Must be public
                .eq(NoteEntry::getStatus, 2) // Must be published
                .orderByDesc(NoteEntry::getUpdatedTime);

        IPage<NoteEntry> notePage = noteEntryMapper.selectPage(pageParam, wrapper);

        IPage<NoteListDto> dtoPage = notePage.convert(this::toBasicListDto);
        fillFolderNames(dtoPage.getRecords());

        return dtoPage;
    }

    @Override
    public IPage<NoteListDto> searchNotes(Long userId, String keyword, Integer page, Integer size) {
        Page<NoteEntry> pageParam = new Page<>(page, size);

        LambdaQueryWrapper<NoteEntry> wrapper = new LambdaQueryWrapper<NoteEntry>()
                .eq(NoteEntry::getUserId, userId)
                .and(w -> w.like(NoteEntry::getTitle, keyword)
                        .or()
                        .like(NoteEntry::getContentMd, keyword))
                .orderByDesc(NoteEntry::getUpdatedTime);

        IPage<NoteEntry> notePage = noteEntryMapper.selectPage(pageParam, wrapper);

        IPage<NoteListDto> dtoPage = notePage.convert(this::toBasicListDto);
        fillFolderNames(dtoPage.getRecords());

        return dtoPage;
    }

    @Override
    public IPage<NoteListDto> listCommunityNotes(String keyword, Integer page, Integer size) {
        Page<NoteEntry> pageParam = new Page<>(page, size);

        LambdaQueryWrapper<NoteEntry> wrapper = new LambdaQueryWrapper<NoteEntry>()
                .eq(NoteEntry::getIsPublic, 1)
                .eq(NoteEntry::getStatus, 2) // Published
                .orderByDesc(NoteEntry::getUpdatedTime);

        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(NoteEntry::getTitle, keyword)
                    .or()
                    .like(NoteEntry::getContentMd, keyword));
        }

        IPage<NoteEntry> notePage = noteEntryMapper.selectPage(pageParam, wrapper);
        IPage<NoteListDto> dtoPage = notePage.convert(this::toBasicListDto);

        fillFolderNames(dtoPage.getRecords());
        fillAuthorInfo(dtoPage.getRecords());
        fillEngagementStats(dtoPage.getRecords());

        return dtoPage;
    }

    @Override
    public IPage<NoteListDto> listFollowingNotes(Long userId, Integer page, Integer size) {
        Page<NoteEntry> pageParam = new Page<>(page, size);

        // 利用子查询获取关注的博主ID：SELECT following_id FROM user_relation WHERE follower_id =
        // userId
        LambdaQueryWrapper<NoteEntry> wrapper = new LambdaQueryWrapper<NoteEntry>()
                .eq(NoteEntry::getIsPublic, 1)
                .eq(NoteEntry::getStatus, 2) // Published
                .inSql(NoteEntry::getUserId, "SELECT following_id FROM user_relation WHERE follower_id = " + userId)
                .orderByDesc(NoteEntry::getUpdatedTime);

        IPage<NoteEntry> notePage = noteEntryMapper.selectPage(pageParam, wrapper);
        IPage<NoteListDto> dtoPage = notePage.convert(this::toBasicListDto);

        fillFolderNames(dtoPage.getRecords());
        fillAuthorInfo(dtoPage.getRecords());
        fillEngagementStats(dtoPage.getRecords());

        return dtoPage;
    }

    @Override
    public NoteDetailDto getPublicNoteDetail(Long noteId) {
        NoteEntry note = noteEntryMapper.selectById(noteId);
        if (note == null || note.getIsPublic() != 1 || note.getStatus() != 2) {
            throw new BusinessException(404, "笔记不存在或未公开");
        }

        NoteDetailDto dto = new NoteDetailDto();
        BeanUtils.copyProperties(note, dto);
        dto.setAuthorId(note.getUserId());

        // Fill author info (name and avatar)
        SysUser author = userMapper.selectById(note.getUserId());
        if (author != null) {
            dto.setAuthorName(author.getUsername());
            dto.setAuthorAvatar(author.getAvatar());
        }

        // Fill Folder Name
        if (note.getFolderId() != null && note.getFolderId() > 0) {
            NoteFolder folder = folderMapper.selectById(note.getFolderId());
            if (folder != null) {
                dto.setFolderName(folder.getName());
            }
        }

        // Fill Author Info
        SysUser user = userMapper.selectById(note.getUserId());
        if (user != null) {
            dto.setAuthorName(user.getUsername());
            dto.setAuthorAvatar(user.getAvatar());
        }

        // Parse links (using author's context)
        dto.setOutgoingLinks(parseOutgoingLinks(note.getUserId(), note.getContentMd()));
        dto.setIncomingLinks(findIncomingLinks(note.getUserId(), noteId, note.getTitle()));

        // Attachments
        List<NoteAttachment> attachments = attachmentMapper.selectList(new LambdaQueryWrapper<NoteAttachment>()
                .eq(NoteAttachment::getNoteId, noteId)
                .orderByDesc(NoteAttachment::getCreatedTime));
        dto.setAttachments(attachments.stream().map(this::toAttachmentDto).collect(Collectors.toList()));

        // Fill engagement stats
        dto.setViewCount(engagementService.getViewCount(noteId));
        dto.setLikeCount(engagementService.getLikeCount(noteId));

        return dto;
    }

    @Override
    public NoteDetailDto getPublicNoteDetail(Long noteId, Long currentUserId) {
        NoteDetailDto dto = getPublicNoteDetail(noteId);

        // Fill isLiked if user is logged in
        if (currentUserId != null) {
            dto.setIsLiked(engagementService.isLiked(currentUserId, noteId));
        } else {
            dto.setIsLiked(false);
        }

        return dto;
    }

    @Override
    public List<NoteListDto> getRelatedPublicNotes(Long noteId, int limit) {
        NoteEntry note = noteEntryMapper.selectById(noteId);
        if (note == null) {
            return java.util.Collections.emptyList();
        }

        // 构造供搜索的文本：标题 + 摘要 + 标签
        StringBuilder textBuilder = new StringBuilder(note.getTitle());
        if (note.getSummary() != null && !note.getSummary().isEmpty()) {
            textBuilder.append(" ").append(note.getSummary());
        } else if (note.getContentMd() != null) {
            String content = Jsoup.parse(note.getContentMd()).text();
            textBuilder.append(" ").append(content.length() > 500 ? content.substring(0, 500) : content);
        }
        if (note.getTags() != null) {
            textBuilder.append(" ").append(note.getTags());
        }

        List<Long> similarIds = searchService.searchSimilarNoteIds(textBuilder.toString(), limit + 2);

        if (similarIds.isEmpty()) {
            return java.util.Collections.emptyList();
        }

        // 移除自身
        similarIds.remove(noteId);
        if (similarIds.isEmpty()) {
            return java.util.Collections.emptyList();
        }

        List<Long> targetIds = similarIds.stream().limit(limit).collect(Collectors.toList());

        List<NoteEntry> relatedNotes = noteEntryMapper.selectList(new LambdaQueryWrapper<NoteEntry>()
                .in(NoteEntry::getId, targetIds)
                .eq(NoteEntry::getIsPublic, 1)
                .eq(NoteEntry::getStatus, 2));

        if (relatedNotes.isEmpty()) {
            return java.util.Collections.emptyList();
        }

        java.util.Map<Long, NoteEntry> noteMap = relatedNotes.stream()
                .collect(Collectors.toMap(NoteEntry::getId, n -> n));
        List<NoteEntry> sortedNotes = new ArrayList<>();
        for (Long id : targetIds) {
            if (noteMap.containsKey(id)) {
                sortedNotes.add(noteMap.get(id));
            }
        }

        List<NoteListDto> dtoList = sortedNotes.stream().map(n -> {
            NoteListDto dto = new NoteListDto();
            BeanUtils.copyProperties(n, dto);
            dto.setAuthorId(n.getUserId());
            return dto;
        }).collect(Collectors.toList());

        fillAuthorInfo(dtoList);
        fillEngagementStats(dtoList);

        return dtoList;
    }

    @Override
    public java.util.List<NoteListDto> getHotNotes(int limit) {
        // 抓取近期发布的公开笔记作为候选（基于规模上限，限制 200 篇避免全表扫描内存爆炸）
        List<NoteEntry> candidateNotes = noteEntryMapper.selectList(new LambdaQueryWrapper<NoteEntry>()
                .eq(NoteEntry::getIsPublic, 1)
                .eq(NoteEntry::getStatus, 2)
                .orderByDesc(NoteEntry::getUpdatedTime)
                .last("LIMIT 200"));

        if (candidateNotes.isEmpty()) {
            return new ArrayList<>();
        }

        List<NoteListDto> dtoList = candidateNotes.stream()
                .map(this::toBasicListDto)
                .collect(Collectors.toList());

        fillAuthorInfo(dtoList);
        fillEngagementStats(dtoList);

        // 在内存中根据 Redis 里最新的点赞与阅读量进行大顶堆加权排序 (1 个赞 = 5次阅读)
        dtoList.sort((a, b) -> {
            long scoreA = (a.getLikeCount() != null ? a.getLikeCount() : 0) * 5 +
                    (a.getViewCount() != null ? a.getViewCount() : 0);
            long scoreB = (b.getLikeCount() != null ? b.getLikeCount() : 0) * 5 +
                    (b.getViewCount() != null ? b.getViewCount() : 0);
            return Long.compare(scoreB, scoreA);
        });

        // 截取 Top N
        return dtoList.stream().limit(limit).collect(Collectors.toList());
    }

    @Override
    public UserPublicProfileDto getUserPublicProfile(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }

        UserPublicProfileDto dto = new UserPublicProfileDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setAvatar(user.getAvatar());

        // 获取 Profile
        SysUserProfile profile = userProfileMapper.selectById(userId);
        if (profile != null) {
            dto.setBio(profile.getBio());
        }

        // 统计公开笔记数
        Long publicNoteCount = noteEntryMapper.selectCount(new LambdaQueryWrapper<NoteEntry>()
                .eq(NoteEntry::getUserId, userId)
                .eq(NoteEntry::getIsPublic, 1)
                .eq(NoteEntry::getStatus, 2));
        dto.setPublicNoteCount(publicNoteCount);

        // 获取用户所有公开笔记的ID，用于统计总浏览和点赞
        List<NoteEntry> publicNotes = noteEntryMapper.selectList(new LambdaQueryWrapper<NoteEntry>()
                .select(NoteEntry::getId)
                .eq(NoteEntry::getUserId, userId)
                .eq(NoteEntry::getIsPublic, 1)
                .eq(NoteEntry::getStatus, 2));

        long totalViews = 0L;
        long totalLikes = 0L;

        if (!publicNotes.isEmpty()) {
            List<Long> noteIds = publicNotes.stream().map(NoteEntry::getId).collect(Collectors.toList());
            java.util.Map<Long, NoteEngagementService.EngagementStats> statsMap = engagementService
                    .batchGetEngagement(noteIds);
            for (NoteEngagementService.EngagementStats stats : statsMap.values()) {
                if (stats != null) {
                    totalViews += (stats.getViewCount() != null ? stats.getViewCount() : 0L);
                    totalLikes += (stats.getLikeCount() != null ? stats.getLikeCount() : 0L);
                }
            }
        }

        dto.setTotalViews(totalViews);
        dto.setTotalLikes(totalLikes);

        // 统计关注数据
        dto.setFollowerCount(userRelationService.getFollowerCount(userId));
        dto.setFollowingCount(userRelationService.getFollowingCount(userId));

        // 尝试获取当前用户是否关注了该博主
        try {
            Long currentUserId = SecurityUtils.getCurrentUserId();
            dto.setIsFollowing(userRelationService.isFollowing(currentUserId, userId));
        } catch (Exception e) {
            dto.setIsFollowing(false);
        }

        return dto;
    }

    @Override
    public String uploadImage(Long userId, MultipartFile file) {
        return minioUtil.uploadFile(file, "note-images/" + userId);
    }

    // ========== 私有方法 ==========

    private void fillFolderNames(List<NoteListDto> dtos) {
        if (dtos == null || dtos.isEmpty())
            return;

        // Collect distinct folder IDs
        java.util.Set<Long> folderIds = dtos.stream()
                .map(NoteListDto::getFolderId)
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toSet());

        if (folderIds.isEmpty())
            return;

        // Batch fetch folders
        // Batch fetch folders
        List<NoteFolder> folders = folderMapper.selectList(new LambdaQueryWrapper<NoteFolder>()
                .in(NoteFolder::getId, folderIds));
        java.util.Map<Long, String> folderMap = folders.stream()
                .collect(Collectors.toMap(NoteFolder::getId, NoteFolder::getName));

        // Fill names
        dtos.forEach(dto -> {
            if (dto.getFolderId() != null && folderMap.containsKey(dto.getFolderId())) {
                dto.setFolderName(folderMap.get(dto.getFolderId()));
            }
        });
    }

    private void fillAuthorInfo(List<NoteListDto> dtos) {
        if (dtos == null || dtos.isEmpty())
            return;

        java.util.Set<Long> userIds = dtos.stream()
                .map(NoteListDto::getAuthorId)
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toSet());

        if (userIds.isEmpty())
            return;

        List<SysUser> users = userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .in(SysUser::getId, userIds));
        java.util.Map<Long, SysUser> userMap = users.stream()
                .collect(Collectors.toMap(SysUser::getId, u -> u));

        dtos.forEach(dto -> {
            if (dto.getAuthorId() != null && userMap.containsKey(dto.getAuthorId())) {
                SysUser u = userMap.get(dto.getAuthorId());
                dto.setAuthorName(u.getUsername());
                dto.setAuthorAvatar(u.getAvatar());
            }
        });
    }

    private void fillEngagementStats(List<NoteListDto> dtos) {
        if (dtos == null || dtos.isEmpty())
            return;

        List<Long> noteIds = dtos.stream()
                .map(NoteListDto::getId)
                .collect(Collectors.toList());

        java.util.Map<Long, NoteEngagementService.EngagementStats> statsMap = engagementService
                .batchGetEngagement(noteIds);

        dtos.forEach(dto -> {
            NoteEngagementService.EngagementStats stats = statsMap.get(dto.getId());
            if (stats != null) {
                dto.setViewCount(stats.getViewCount());
                dto.setLikeCount(stats.getLikeCount());
            } else {
                dto.setViewCount(0L);
                dto.setLikeCount(0L);
            }
        });
    }

    private NoteListDto toBasicListDto(NoteEntry note) {
        NoteListDto dto = new NoteListDto();
        BeanUtils.copyProperties(note, dto);
        // Manually map userId to authorId (field names don't match)
        dto.setAuthorId(note.getUserId());
        return dto;
    }

    /**
     * @deprecated Use toBasicListDto and fillFolderNames instead
     */
    @Deprecated
    private NoteListDto toListDto(NoteEntry note) {
        return toBasicListDto(note);
    }

    @Override
    public int rebuildNoteIndex(Long userId) {
        log.info("开始重建用户笔记向量索引: userId={}", userId);

        // 查询用户所有笔记
        List<NoteEntry> notes = noteEntryMapper.selectList(
                new LambdaQueryWrapper<NoteEntry>()
                        .eq(NoteEntry::getUserId, userId)
                        .isNotNull(NoteEntry::getContentMd)
                        .ne(NoteEntry::getContentMd, ""));

        int count = 0;
        for (NoteEntry note : notes) {
            try {
                // 发送索引消息到 RabbitMQ
                indexUpdateProducer.sendCreateIndex(
                        "NOTE",
                        note.getId(),
                        userId,
                        note.getTitle(),
                        note.getContentMd());
                count++;
            } catch (Exception e) {
                log.warn("笔记索引失败: noteId={}, error={}", note.getId(), e.getMessage());
            }
        }

        log.info("笔记向量索引任务已提交: userId={}, count={}", userId, count);
        return count;
    }

    private NoteAttachmentDto toAttachmentDto(NoteAttachment attachment) {
        NoteAttachmentDto dto = new NoteAttachmentDto();
        BeanUtils.copyProperties(attachment, dto);
        return dto;
    }

    private NoteEntry getAndCheckOwnership(Long userId, Long noteId) {
        NoteEntry note = noteEntryMapper.selectById(noteId);
        if (note == null) {
            throw new BusinessException(404, "笔记不存在");
        }
        if (!note.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权访问该笔记");
        }
        return note;
    }

    private void saveVersion(NoteEntry note) {
        NoteVersion version = new NoteVersion();
        BeanUtils.copyProperties(note, version);
        version.setId(null);
        version.setNoteId(note.getId());
        version.setCreatedTime(java.time.LocalDateTime.now());

        versionMapper.insert(version);

        // Count and delete old versions
        Long count = versionMapper.selectCount(new LambdaQueryWrapper<NoteVersion>()
                .eq(NoteVersion::getNoteId, note.getId()));

        if (count > MAX_VERSIONS) {
            List<NoteVersion> versions = versionMapper.selectList(new LambdaQueryWrapper<NoteVersion>()
                    .eq(NoteVersion::getNoteId, note.getId())
                    .orderByDesc(NoteVersion::getCreatedTime));

            if (versions.size() > MAX_VERSIONS) {
                List<Long> idsToDelete = versions.stream()
                        .skip(MAX_VERSIONS)
                        .map(NoteVersion::getId)
                        .collect(Collectors.toList());
                if (!idsToDelete.isEmpty()) {
                    versionMapper.delete(new LambdaQueryWrapper<NoteVersion>()
                            .in(NoteVersion::getId, idsToDelete));
                }
            }
        }
    }

    private List<NoteLinkDto> parseOutgoingLinks(Long userId, String content) {
        if (content == null || content.isEmpty()) {
            return new ArrayList<>();
        }

        List<NoteLinkDto> links = new ArrayList<>();
        Matcher matcher = LINK_PATTERN.matcher(content);

        while (matcher.find()) {
            String title = matcher.group(1);
            // Search for note with this title
            List<NoteEntry> targets = noteEntryMapper.selectList(new LambdaQueryWrapper<NoteEntry>()
                    .eq(NoteEntry::getUserId, userId)
                    .eq(NoteEntry::getTitle, title)
                    .last("LIMIT 1"));

            if (!targets.isEmpty()) {
                NoteEntry target = targets.get(0);
                NoteLinkDto link = new NoteLinkDto();
                link.setId(target.getId());
                link.setTitle(target.getTitle());
                link.setSummary(target.getSummary());
                links.add(link);
            }
        }
        return links;
    }

    private List<NoteLinkDto> findIncomingLinks(Long userId, Long noteId, String title) {
        if (title == null || title.isEmpty()) {
            return new ArrayList<>();
        }

        // Find notes containing [[title]]
        String searchStr = "[[" + title + "]]";
        List<NoteEntry> referringNotes = noteEntryMapper.selectList(new LambdaQueryWrapper<NoteEntry>()
                .eq(NoteEntry::getUserId, userId)
                .like(NoteEntry::getContentMd, searchStr));

        return referringNotes.stream().map(note -> {
            NoteLinkDto link = new NoteLinkDto();
            link.setId(note.getId());
            link.setTitle(note.getTitle());
            link.setSummary(note.getSummary());
            return link;
        }).collect(Collectors.toList());
    }

    private List<ContentLinkDto> parseContentLinks(Long userId, String content) {
        if (content == null || content.isEmpty()) {
            return new ArrayList<>();
        }

        List<ContentLinkDto> links = new ArrayList<>();
        Matcher matcher = CONTENT_LINK_PATTERN.matcher(content);

        while (matcher.find()) {
            try {
                Long contentId = Long.parseLong(matcher.group(1));
                ContentItem item = contentItemMapper.selectById(contentId);
                if (item != null) {
                    ContentLinkDto link = new ContentLinkDto();
                    link.setId(item.getId());
                    link.setTitle(item.getTitle());
                    link.setUrl(item.getUrl());
                    links.add(link);
                }
            } catch (NumberFormatException e) {
                // ignore
            }
        }
        return links;
    }
}

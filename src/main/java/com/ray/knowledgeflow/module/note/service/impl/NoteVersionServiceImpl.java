package com.ray.knowledgeflow.module.note.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ray.knowledgeflow.common.exception.BusinessException;
import com.ray.knowledgeflow.module.note.dto.NoteVersionDto;
import com.ray.knowledgeflow.module.note.entity.NoteEntry;
import com.ray.knowledgeflow.module.note.entity.NoteVersion;
import com.ray.knowledgeflow.module.note.mapper.NoteEntryMapper;
import com.ray.knowledgeflow.module.note.mapper.NoteVersionMapper;
import com.ray.knowledgeflow.module.note.service.INoteVersionService;
import com.ray.knowledgeflow.module.search.message.IndexUpdateProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 笔记版本历史服务实现类
 *
 * @author Ray
 * @since 2024-01-16
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NoteVersionServiceImpl implements INoteVersionService {

    private static final int MAX_VERSIONS = 20;

    private final NoteVersionMapper versionMapper;
    private final NoteEntryMapper noteEntryMapper;
    private final IndexUpdateProducer indexUpdateProducer;

    @Override
    public List<NoteVersionDto> listVersions(Long userId, Long noteId) {
        checkNoteOwnership(userId, noteId);

        List<NoteVersion> versions = versionMapper.selectList(new LambdaQueryWrapper<NoteVersion>()
                .eq(NoteVersion::getNoteId, noteId)
                .orderByDesc(NoteVersion::getCreatedTime));

        return versions.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public NoteVersionDto getVersion(Long userId, Long noteId, Long versionId) {
        checkNoteOwnership(userId, noteId);

        NoteVersion version = versionMapper.selectById(versionId);
        if (version == null) {
            throw new BusinessException(404, "版本不存在");
        }
        if (!version.getNoteId().equals(noteId)) {
            throw new BusinessException(400, "版本不属于该笔记");
        }

        return toDto(version);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void restoreVersion(Long userId, Long noteId, Long versionId) {
        NoteEntry note = checkNoteOwnership(userId, noteId);

        NoteVersion version = versionMapper.selectById(versionId);
        if (version == null) {
            throw new BusinessException(404, "版本不存在");
        }
        if (!version.getNoteId().equals(noteId)) {
            throw new BusinessException(400, "版本不属于该笔记");
        }

        // 1. 【容错防丢】回溯前先把当前内容保存为一个快照版本，防止用户误操作丢失刚写的内容
        saveCurrentAsVersion(note);

        // 2. 将笔记内容覆盖为目标历史版本
        note.setTitle(version.getTitle());
        note.setContentMd(version.getContentMd());
        note.setUpdatedTime(LocalDateTime.now());

        // 3. 同步更新摘要（供搜索索引使用）
        if (version.getContentMd() != null && !version.getContentMd().isEmpty()) {
            String plainText = Jsoup.parse(version.getContentMd()).text();
            note.setSummary(plainText.length() > 200 ? plainText.substring(0, 200) + "..." : plainText);
        }

        noteEntryMapper.updateById(note);
        log.info("笔记版本回溯成功: noteId={}, versionId={}, userId={}", noteId, versionId, userId);

        // 4. 【架构联动】发布 RabbitMQ 消息，异步触发 Elasticsearch 索引和 Qdrant 向量库更新
        indexUpdateProducer.sendUpdateIndex(
                "NOTE", note.getId(), userId,
                note.getTitle(), note.getContentMd(),
                note.getSummary(), note.getTags());
    }

    // ========== 私有方法 ==========

    private NoteEntry checkNoteOwnership(Long userId, Long noteId) {
        NoteEntry note = noteEntryMapper.selectById(noteId);
        if (note == null) {
            throw new BusinessException(404, "笔记不存在");
        }
        if (!note.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权操作此笔记");
        }
        return note;
    }

    private NoteVersionDto toDto(NoteVersion version) {
        NoteVersionDto dto = new NoteVersionDto();
        BeanUtils.copyProperties(version, dto);
        return dto;
    }

    private void saveCurrentAsVersion(NoteEntry note) {
        NoteVersion version = new NoteVersion();
        version.setNoteId(note.getId());
        version.setTitle(note.getTitle());
        version.setContentMd(note.getContentMd());
        version.setCreatedTime(LocalDateTime.now());
        versionMapper.insert(version);

        // 版本数量上限裁剪，保持与 NoteServiceImpl.saveVersion 一致（最多保留 MAX_VERSIONS 条）
        Long count = versionMapper.selectCount(new LambdaQueryWrapper<NoteVersion>()
                .eq(NoteVersion::getNoteId, note.getId()));
        if (count > MAX_VERSIONS) {
            List<NoteVersion> allVersions = versionMapper.selectList(new LambdaQueryWrapper<NoteVersion>()
                    .eq(NoteVersion::getNoteId, note.getId())
                    .orderByDesc(NoteVersion::getCreatedTime));
            if (allVersions.size() > MAX_VERSIONS) {
                List<Long> idsToDelete = allVersions.stream()
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
}

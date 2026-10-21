package com.ray.knowledgeflow.module.note.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ray.knowledgeflow.module.note.dto.NoteLinkDto;
import com.ray.knowledgeflow.module.note.entity.NoteEntry;
import com.ray.knowledgeflow.module.note.entity.NoteLink;
import com.ray.knowledgeflow.module.note.mapper.NoteEntryMapper;
import com.ray.knowledgeflow.module.note.mapper.NoteLinkMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 双向链接事实服务。
 *
 * <p>新语法使用稳定 ID：{@code [[note:123|显示标题]]}。旧的
 * {@code [[标题]]}/{@code [[标题|别名]]} 仍可解析，并在保存时固化为 note_link 关系。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NoteLinkService {

    private static final Pattern STABLE_LINK_PATTERN =
            Pattern.compile("\\[\\[note:(\\d+)(?:\\|([^\\]]+))?\\]\\]", Pattern.CASE_INSENSITIVE);
    private static final Pattern LEGACY_LINK_PATTERN =
            Pattern.compile("\\[\\[(?!note:|!)([^\\]|]+)(?:\\|[^\\]]+)?\\]\\]", Pattern.CASE_INSENSITIVE);

    private final NoteLinkMapper noteLinkMapper;
    private final NoteEntryMapper noteEntryMapper;

    /**
     * 用当前正文原子替换一篇笔记的全部显式出链。
     */
    @Transactional(rollbackFor = Exception.class)
    public void syncExplicitLinks(NoteEntry source) {
        if (source == null || source.getId() == null || source.getUserId() == null) {
            return;
        }

        LinkedHashSet<Long> targetIds = parseTargetIds(source.getUserId(), source.getContentMd());
        targetIds.remove(source.getId());

        noteLinkMapper.delete(new LambdaQueryWrapper<NoteLink>()
                .eq(NoteLink::getSourceNoteId, source.getId())
                .eq(NoteLink::getLinkType, NoteLink.TYPE_EXPLICIT));

        for (Long targetId : targetIds) {
            NoteLink link = new NoteLink();
            link.setUserId(source.getUserId());
            link.setSourceNoteId(source.getId());
            link.setTargetNoteId(targetId);
            link.setLinkType(NoteLink.TYPE_EXPLICIT);
            noteLinkMapper.insert(link);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteLinksForNote(Long noteId) {
        if (noteId == null) {
            return;
        }
        noteLinkMapper.delete(new LambdaQueryWrapper<NoteLink>()
                .and(w -> w.eq(NoteLink::getSourceNoteId, noteId)
                        .or()
                        .eq(NoteLink::getTargetNoteId, noteId)));
    }

    public List<NoteLinkDto> getOutgoingLinks(Long userId, Long noteId, boolean onlyPublic) {
        List<NoteLink> links = noteLinkMapper.selectList(new LambdaQueryWrapper<NoteLink>()
                .eq(NoteLink::getUserId, userId)
                .eq(NoteLink::getSourceNoteId, noteId)
                .eq(NoteLink::getLinkType, NoteLink.TYPE_EXPLICIT)
                .orderByAsc(NoteLink::getId));
        return resolveNotes(links.stream().map(NoteLink::getTargetNoteId).toList(), onlyPublic);
    }

    public List<NoteLinkDto> getIncomingLinks(Long userId, Long noteId, boolean onlyPublic) {
        List<NoteLink> links = noteLinkMapper.selectList(new LambdaQueryWrapper<NoteLink>()
                .eq(NoteLink::getUserId, userId)
                .eq(NoteLink::getTargetNoteId, noteId)
                .eq(NoteLink::getLinkType, NoteLink.TYPE_EXPLICIT)
                .orderByAsc(NoteLink::getId));
        return resolveNotes(links.stream().map(NoteLink::getSourceNoteId).toList(), onlyPublic);
    }

    public List<Long> getOutgoingTargetIds(Long userId, Long noteId) {
        return noteLinkMapper.selectList(new LambdaQueryWrapper<NoteLink>()
                        .eq(NoteLink::getUserId, userId)
                        .eq(NoteLink::getSourceNoteId, noteId)
                        .eq(NoteLink::getLinkType, NoteLink.TYPE_EXPLICIT)
                        .orderByAsc(NoteLink::getId))
                .stream()
                .map(NoteLink::getTargetNoteId)
                .distinct()
                .toList();
    }

    /**
     * 为历史笔记回填稳定关系。旧标题语法只在标题唯一时迁移，歧义引用保留原文并记录日志。
     */
    @Transactional(rollbackFor = Exception.class)
    public int rebuildUserLinks(Long userId) {
        List<NoteEntry> notes = noteEntryMapper.selectList(new LambdaQueryWrapper<NoteEntry>()
                .eq(NoteEntry::getUserId, userId)
                .orderByAsc(NoteEntry::getId));
        for (NoteEntry note : notes) {
            syncExplicitLinks(note);
        }
        return notes.size();
    }

    LinkedHashSet<Long> parseTargetIds(Long userId, String content) {
        LinkedHashSet<Long> ids = new LinkedHashSet<>();
        if (content == null || content.isBlank()) {
            return ids;
        }

        Matcher stableMatcher = STABLE_LINK_PATTERN.matcher(content);
        while (stableMatcher.find()) {
            try {
                Long targetId = Long.valueOf(stableMatcher.group(1));
                NoteEntry target = noteEntryMapper.selectById(targetId);
                if (target != null && userId.equals(target.getUserId())) {
                    ids.add(targetId);
                } else {
                    log.warn("忽略无权访问或不存在的稳定笔记引用: userId={}, targetId={}", userId, targetId);
                }
            } catch (NumberFormatException ignored) {
                // 正则已经限制为数字，这里仅作防御。
            }
        }

        Matcher legacyMatcher = LEGACY_LINK_PATTERN.matcher(content);
        while (legacyMatcher.find()) {
            String title = legacyMatcher.group(1).trim();
            if (title.isEmpty()) {
                continue;
            }
            List<NoteEntry> matches = noteEntryMapper.selectList(new LambdaQueryWrapper<NoteEntry>()
                    .eq(NoteEntry::getUserId, userId)
                    .eq(NoteEntry::getTitle, title)
                    .last("LIMIT 2"));
            if (matches.size() == 1) {
                ids.add(matches.get(0).getId());
            } else if (matches.size() > 1) {
                log.warn("旧式标题引用存在歧义，已跳过: userId={}, title={}", userId, title);
            }
        }
        return ids;
    }

    private List<NoteLinkDto> resolveNotes(Collection<Long> ids, boolean onlyPublic) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<Long> orderedIds = new ArrayList<>(new LinkedHashSet<>(ids));
        List<NoteEntry> notes = noteEntryMapper.selectList(new LambdaQueryWrapper<NoteEntry>()
                .in(NoteEntry::getId, orderedIds));
        Map<Long, NoteEntry> noteMap = notes.stream()
                .filter(n -> !onlyPublic || (Integer.valueOf(1).equals(n.getIsPublic())
                        && Integer.valueOf(2).equals(n.getStatus())))
                .collect(Collectors.toMap(NoteEntry::getId, Function.identity()));

        return orderedIds.stream()
                .map(noteMap::get)
                .filter(java.util.Objects::nonNull)
                .map(this::toDto)
                .toList();
    }

    private NoteLinkDto toDto(NoteEntry note) {
        NoteLinkDto dto = new NoteLinkDto();
        dto.setId(note.getId());
        dto.setTitle(note.getTitle());
        dto.setSummary(note.getSummary());
        return dto;
    }
}

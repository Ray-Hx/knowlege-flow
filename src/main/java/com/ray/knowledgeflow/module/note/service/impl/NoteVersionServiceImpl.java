package com.ray.knowledgeflow.module.note.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ray.knowledgeflow.common.exception.BusinessException;
import com.ray.knowledgeflow.module.note.dto.NoteVersionDto;
import com.ray.knowledgeflow.module.note.entity.NoteEntry;
import com.ray.knowledgeflow.module.note.entity.NoteVersion;
import com.ray.knowledgeflow.module.note.mapper.NoteEntryMapper;
import com.ray.knowledgeflow.module.note.mapper.NoteVersionMapper;
import com.ray.knowledgeflow.module.note.service.INoteVersionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    private final NoteVersionMapper versionMapper;
    private final NoteEntryMapper noteEntryMapper;

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

        // 保存当前状态为新版本（如果需要保留恢复前的状态）
        // 这里简化处理，直接覆盖当前笔记内容
        // 建议：在覆盖前也可以自动保存一次当前版本
        saveCurrentAsVersion(note);

        // 恢复内容
        note.setTitle(version.getTitle());
        note.setContentMd(version.getContentMd());
        note.setUpdatedTime(LocalDateTime.now());

        noteEntryMapper.updateById(note);
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
        versionMapper.insert(version);
    }
}

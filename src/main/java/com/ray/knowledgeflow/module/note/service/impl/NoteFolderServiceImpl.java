package com.ray.knowledgeflow.module.note.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ray.knowledgeflow.common.exception.BusinessException;
import com.ray.knowledgeflow.module.note.dto.CreateFolderDto;
import com.ray.knowledgeflow.module.note.dto.NoteFolderDto;
import com.ray.knowledgeflow.module.note.entity.NoteEntry;
import com.ray.knowledgeflow.module.note.entity.NoteFolder;
import com.ray.knowledgeflow.module.note.mapper.NoteEntryMapper;
import com.ray.knowledgeflow.module.note.mapper.NoteFolderMapper;
import com.ray.knowledgeflow.module.note.service.INoteFolderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 笔记文件夹服务实现类
 *
 * @author Ray
 * @since 2024-01-16
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NoteFolderServiceImpl implements INoteFolderService {

    private final NoteFolderMapper folderMapper;
    private final NoteEntryMapper noteEntryMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NoteFolderDto createFolder(Long userId, CreateFolderDto dto) {
        // 检查同名文件夹
        long count = folderMapper.selectCount(new LambdaQueryWrapper<NoteFolder>()
                .eq(NoteFolder::getUserId, userId)
                .eq(NoteFolder::getName, dto.getName())
                .eq(NoteFolder::getParentId, dto.getParentId() != null ? dto.getParentId() : 0));

        if (count > 0) {
            throw new BusinessException(400, "同名文件夹已存在");
        }

        NoteFolder folder = new NoteFolder();
        folder.setUserId(userId);
        folder.setName(dto.getName());
        folder.setParentId(dto.getParentId() != null ? dto.getParentId() : 0L);
        folder.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);

        folderMapper.insert(folder);

        return toDto(folder);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NoteFolderDto updateFolder(Long userId, Long folderId, CreateFolderDto dto) {
        NoteFolder folder = getAndCheckOwnership(userId, folderId);

        if (dto.getName() != null) {
            folder.setName(dto.getName());
        }
        if (dto.getSortOrder() != null) {
            folder.setSortOrder(dto.getSortOrder());
        }

        folderMapper.updateById(folder);

        return toDto(folder);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFolder(Long userId, Long folderId) {
        getAndCheckOwnership(userId, folderId);

        // 递归删除子文件夹
        deleteSubFolders(userId, folderId);

        // 删除该文件夹下的所有笔记
        noteEntryMapper.delete(new LambdaQueryWrapper<NoteEntry>()
                .eq(NoteEntry::getUserId, userId)
                .eq(NoteEntry::getFolderId, folderId));

        // 删除文件夹
        folderMapper.deleteById(folderId);
    }

    @Override
    public List<NoteFolderDto> getFolderTree(Long userId) {
        List<NoteFolder> folders = folderMapper.selectList(new LambdaQueryWrapper<NoteFolder>()
                .eq(NoteFolder::getUserId, userId)
                .orderByAsc(NoteFolder::getSortOrder));

        return folders.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void moveNote(Long userId, Long noteId, Long folderId) {
        // 检查笔记所有权
        NoteEntry note = noteEntryMapper.selectById(noteId);
        if (note == null || !note.getUserId().equals(userId)) {
            throw new BusinessException(404, "笔记不存在");
        }

        // 检查目标文件夹
        if (folderId != null && folderId > 0) {
            NoteFolder folder = folderMapper.selectById(folderId);
            if (folder == null || !folder.getUserId().equals(userId)) {
                throw new BusinessException(404, "目标文件夹不存在");
            }
        }

        note.setFolderId(folderId);
        noteEntryMapper.updateById(note);
    }

    // ========== 私有方法 ==========

    private NoteFolder getAndCheckOwnership(Long userId, Long folderId) {
        NoteFolder folder = folderMapper.selectById(folderId);
        if (folder == null) {
            throw new BusinessException(404, "文件夹不存在");
        }
        if (!folder.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权操作此文件夹");
        }
        return folder;
    }

    private void deleteSubFolders(Long userId, Long parentId) {
        List<NoteFolder> subFolders = folderMapper.selectList(new LambdaQueryWrapper<NoteFolder>()
                .eq(NoteFolder::getUserId, userId)
                .eq(NoteFolder::getParentId, parentId));

        for (NoteFolder subFolder : subFolders) {
            deleteSubFolders(userId, subFolder.getId());
            noteEntryMapper.delete(new LambdaQueryWrapper<NoteEntry>()
                    .eq(NoteEntry::getUserId, userId)
                    .eq(NoteEntry::getFolderId, subFolder.getId()));
            folderMapper.deleteById(subFolder.getId());
        }
    }

    private NoteFolderDto toDto(NoteFolder folder) {
        NoteFolderDto dto = new NoteFolderDto();
        BeanUtils.copyProperties(folder, dto);

        // 统计子文件夹数量
        long subFolderCount = folderMapper.selectCount(new LambdaQueryWrapper<NoteFolder>()
                .eq(NoteFolder::getUserId, folder.getUserId())
                .eq(NoteFolder::getParentId, folder.getId()));
        dto.setSubFolderCount((int) subFolderCount);

        // 统计笔记数量
        long noteCount = noteEntryMapper.selectCount(new LambdaQueryWrapper<NoteEntry>()
                .eq(NoteEntry::getUserId, folder.getUserId())
                .eq(NoteEntry::getFolderId, folder.getId()));
        dto.setNoteCount((int) noteCount);

        return dto;
    }
}

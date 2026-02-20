package com.ray.knowledgeflow.module.note.service;

import com.ray.knowledgeflow.module.note.dto.CreateFolderDto;
import com.ray.knowledgeflow.module.note.dto.NoteFolderDto;

import java.util.List;

/**
 * 笔记文件夹服务接口
 *
 * @author Ray
 * @since 2024-01-16
 */
public interface INoteFolderService {

    /**
     * 创建文件夹
     *
     * @param userId 用户ID
     * @param dto    创建请求
     * @return 文件夹DTO
     */
    NoteFolderDto createFolder(Long userId, CreateFolderDto dto);

    /**
     * 更新文件夹
     *
     * @param userId   用户ID
     * @param folderId 文件夹ID
     * @param dto      更新请求
     * @return 文件夹DTO
     */
    NoteFolderDto updateFolder(Long userId, Long folderId, CreateFolderDto dto);

    /**
     * 删除文件夹（级联删除子文件夹和笔记）
     *
     * @param userId   用户ID
     * @param folderId 文件夹ID
     */
    void deleteFolder(Long userId, Long folderId);

    /**
     * 获取文件夹树
     *
     * @param userId 用户ID
     * @return 文件夹树列表
     */
    List<NoteFolderDto> getFolderTree(Long userId);

    /**
     * 移动笔记到其他文件夹
     *
     * @param userId   用户ID
     * @param noteId   笔记ID
     * @param folderId 目标文件夹ID
     */
    void moveNote(Long userId, Long noteId, Long folderId);
}

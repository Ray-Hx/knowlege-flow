package com.ray.knowledgeflow.module.note.service;

import com.ray.knowledgeflow.module.note.dto.NoteVersionDto;

import java.util.List;

/**
 * 笔记版本历史服务接口
 *
 * @author Ray
 * @since 2024-01-16
 */
public interface INoteVersionService {

    /**
     * 获取笔记的历史版本列表
     *
     * @param userId 用户ID
     * @param noteId 笔记ID
     * @return 版本列表
     */
    List<NoteVersionDto> listVersions(Long userId, Long noteId);

    /**
     * 获取特定版本内容
     *
     * @param userId    用户ID
     * @param noteId    笔记ID
     * @param versionId 版本ID
     * @return 版本详情
     */
    NoteVersionDto getVersion(Long userId, Long noteId, Long versionId);

    /**
     * 恢复到某个历史版本
     *
     * @param userId    用户ID
     * @param noteId    笔记ID
     * @param versionId 版本ID
     */
    void restoreVersion(Long userId, Long noteId, Long versionId);
}

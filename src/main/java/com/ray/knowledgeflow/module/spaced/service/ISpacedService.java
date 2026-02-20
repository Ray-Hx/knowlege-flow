package com.ray.knowledgeflow.module.spaced.service;

import com.ray.knowledgeflow.module.spaced.dto.*;

import java.util.List;

/**
 * 间隔重复学习服务接口 (基于笔记)
 *
 * @author Ray
 * @since 2024-01-17
 */
public interface ISpacedService {

    /**
     * 获取待复习笔记列表
     *
     * @param userId 用户ID
     * @return 待复习笔记列表
     */
    List<ReviewNoteDto> getDueNotes(Long userId);

    /**
     * 提交复习结果
     *
     * @param userId 用户ID
     * @param noteId 笔记ID
     * @param dto    复习结果
     */
    void submitReview(Long userId, Long noteId, ReviewResultDto dto);

    /**
     * 获取学习统计
     *
     * @param userId 用户ID
     * @return 统计数据
     */
    SpacedStatsDto getStats(Long userId);

    /**
     * 为笔记生成复习问题 (AI)
     *
     * @param noteId 笔记ID
     * @return 生成的问题列表
     */
    List<ReviewNoteDto.QuestionDto> generateQuestions(Long noteId);

    /**
     * 初始化用户的笔记复习队列 (将所有笔记加入)
     *
     * @param userId 用户ID
     */
    void initReviewQueue(Long userId);

    /**
     * 将单个笔记加入复习队列
     *
     * @param userId 用户ID
     * @param noteId 笔记ID
     */
    void addNoteToReview(Long userId, Long noteId);
}

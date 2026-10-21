package com.ray.knowledgeflow.module.spaced.controller;

import com.ray.knowledgeflow.common.result.Result;
import com.ray.knowledgeflow.security.SecurityUtils;
import com.ray.knowledgeflow.module.spaced.dto.*;
import com.ray.knowledgeflow.module.spaced.service.ISpacedService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 间隔重复学习控制器 (基于笔记)
 *
 * @author Ray
 * @since 2024-01-17
 */
@RestController
@RequestMapping("/spaced")
@RequiredArgsConstructor
public class SpacedController {

    private final ISpacedService spacedService;

    /**
     * 获取待复习笔记列表
     */
    @GetMapping("/due")
    public Result<List<ReviewNoteDto>> getDueNotes() {
        Long userId = SecurityUtils.getCurrentUserId();
        List<ReviewNoteDto> notes = spacedService.getDueNotes(userId);
        return Result.success(notes);
    }

    /**
     * 提交复习结果
     */
    @PostMapping("/review/{noteId}")
    public Result<Void> submitReview(
            @PathVariable Long noteId,
            @Valid @RequestBody ReviewResultDto dto) {
        Long userId = SecurityUtils.getCurrentUserId();
        spacedService.submitReview(userId, noteId, dto);
        return Result.success(null);
    }

    /**
     * 获取学习统计
     */
    @GetMapping("/stats")
    public Result<SpacedStatsDto> getStats() {
        Long userId = SecurityUtils.getCurrentUserId();
        SpacedStatsDto stats = spacedService.getStats(userId);
        return Result.success(stats);
    }

    /**
     * 初始化复习队列（将所有笔记加入复习）
     */
    @PostMapping("/init")
    public Result<Void> initReviewQueue() {
        Long userId = SecurityUtils.getCurrentUserId();
        spacedService.initReviewQueue(userId);
        return Result.success(null);
    }

    /**
     * 为笔记重新生成复习问题
     */
    @PostMapping("/questions/{noteId}/regenerate")
    public Result<List<ReviewNoteDto.QuestionDto>> regenerateQuestions(
            @PathVariable Long noteId) {
        Long userId = SecurityUtils.getCurrentUserId();
        List<ReviewNoteDto.QuestionDto> questions = spacedService.generateQuestions(userId, noteId);
        return Result.success(questions);
    }
}

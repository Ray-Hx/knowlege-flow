package com.ray.knowledgeflow.module.note.controller;

import com.ray.knowledgeflow.common.result.Result;
import com.ray.knowledgeflow.module.note.dto.CreateCommentDto;
import com.ray.knowledgeflow.module.note.dto.NoteCommentDto;
import com.ray.knowledgeflow.module.note.service.INoteCommentService;
import com.ray.knowledgeflow.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 笔记评论控制器
 *
 * @author Ray
 * @since 2026-02-20
 */
@Tag(name = "笔记评论")
@RestController
@RequestMapping("/community/note/{noteId}/comments")
@RequiredArgsConstructor
public class NoteCommentController {

    private final INoteCommentService commentService;

    @GetMapping
    @Operation(summary = "获取笔记评论列表")
    public Result<List<NoteCommentDto>> getComments(@PathVariable Long noteId) {
        return Result.success(commentService.getComments(noteId));
    }

    @PostMapping
    @Operation(summary = "发表评论")
    public Result<NoteCommentDto> addComment(
            @PathVariable Long noteId,
            @Valid @RequestBody CreateCommentDto dto) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(commentService.addComment(userId, noteId, dto));
    }

    @DeleteMapping("/{commentId}")
    @Operation(summary = "删除评论")
    public Result<Void> deleteComment(
            @PathVariable Long noteId,
            @PathVariable Long commentId) {
        Long userId = SecurityUtils.getCurrentUserId();
        commentService.deleteComment(userId, commentId);
        return Result.success();
    }
}

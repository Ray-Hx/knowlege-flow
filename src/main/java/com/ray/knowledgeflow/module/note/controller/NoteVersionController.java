package com.ray.knowledgeflow.module.note.controller;

import com.ray.knowledgeflow.common.result.Result;
import com.ray.knowledgeflow.module.note.dto.NoteVersionDto;
import com.ray.knowledgeflow.module.note.service.INoteVersionService;
import com.ray.knowledgeflow.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 笔记版本历史控制器
 *
 * @author Ray
 * @since 2024-01-16
 */
@Tag(name = "笔记版本管理")
@RestController
@RequestMapping("/notes/{noteId}/versions")
@RequiredArgsConstructor
public class NoteVersionController {

    private final INoteVersionService versionService;

    @GetMapping
    @Operation(summary = "获取版本列表")
    public Result<List<NoteVersionDto>> listVersions(@PathVariable Long noteId) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(versionService.listVersions(userId, noteId));
    }

    @GetMapping("/{versionId}")
    @Operation(summary = "获取特定版本详情")
    public Result<NoteVersionDto> getVersion(@PathVariable Long noteId, @PathVariable Long versionId) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(versionService.getVersion(userId, noteId, versionId));
    }

    @PostMapping("/{versionId}/restore")
    @Operation(summary = "恢复到指定版本")
    public Result<Void> restoreVersion(@PathVariable Long noteId, @PathVariable Long versionId) {
        Long userId = SecurityUtils.getCurrentUserId();
        versionService.restoreVersion(userId, noteId, versionId);
        return Result.success();
    }
}

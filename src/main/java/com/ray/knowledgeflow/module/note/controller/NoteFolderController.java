package com.ray.knowledgeflow.module.note.controller;

import com.ray.knowledgeflow.common.result.Result;
import com.ray.knowledgeflow.module.note.dto.CreateFolderDto;
import com.ray.knowledgeflow.module.note.dto.NoteFolderDto;
import com.ray.knowledgeflow.module.note.service.INoteFolderService;
import com.ray.knowledgeflow.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 笔记文件夹管理控制器
 *
 * @author Ray
 * @since 2024-01-16
 */
@Tag(name = "笔记文件夹管理")
@RestController
@RequestMapping("/folders")
@RequiredArgsConstructor
public class NoteFolderController {

    private final INoteFolderService folderService;

    @PostMapping
    @Operation(summary = "创建文件夹")
    public Result<NoteFolderDto> createFolder(@Validated @RequestBody CreateFolderDto dto) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(folderService.createFolder(userId, dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新文件夹")
    public Result<NoteFolderDto> updateFolder(@PathVariable Long id, @RequestBody CreateFolderDto dto) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(folderService.updateFolder(userId, id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除文件夹")
    public Result<Void> deleteFolder(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        folderService.deleteFolder(userId, id);
        return Result.success();
    }

    @GetMapping("/tree")
    @Operation(summary = "获取文件夹树")
    public Result<List<NoteFolderDto>> getFolderTree() {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(folderService.getFolderTree(userId));
    }

    @PutMapping("/notes/{noteId}/move")
    @Operation(summary = "移动笔记")
    public Result<Void> moveNote(@PathVariable Long noteId, @RequestParam Long folderId) {
        Long userId = SecurityUtils.getCurrentUserId();
        folderService.moveNote(userId, noteId, folderId);
        return Result.success();
    }
}

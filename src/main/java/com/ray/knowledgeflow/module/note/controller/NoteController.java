package com.ray.knowledgeflow.module.note.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ray.knowledgeflow.common.result.Result;
import com.ray.knowledgeflow.module.note.dto.*;
import com.ray.knowledgeflow.module.note.service.INoteService;
import com.ray.knowledgeflow.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 笔记管理控制器
 *
 * @author Ray
 * @since 2024-01-16
 */
@Tag(name = "笔记管理")
@RestController
@RequestMapping("/notes")
@RequiredArgsConstructor
public class NoteController {

    private final INoteService noteService;

    @PostMapping
    @Operation(summary = "创建笔记")
    public Result<NoteDetailDto> createNote(@Validated @RequestBody CreateNoteDto dto) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(noteService.createNote(userId, dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新笔记")
    public Result<NoteDetailDto> updateNote(@PathVariable Long id, @RequestBody UpdateNoteDto dto) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(noteService.updateNote(userId, id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除笔记")
    public Result<Void> deleteNote(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        noteService.deleteNote(userId, id);
        return Result.success();
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取笔记详情")
    public Result<NoteDetailDto> getNoteDetail(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(noteService.getNoteDetail(userId, id));
    }

    @GetMapping
    @Operation(summary = "分页查询笔记列表")
    public Result<IPage<NoteListDto>> listNotes(
            @RequestParam(required = false) Long folderId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(noteService.listNotes(userId, folderId, page, size));
    }

    @GetMapping("/search")
    @Operation(summary = "搜索笔记")
    public Result<IPage<NoteListDto>> searchNotes(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(noteService.searchNotes(userId, keyword, page, size));
    }

    @PostMapping("/upload-image")
    @Operation(summary = "上传图片")
    public Result<String> uploadImage(@RequestParam("file") MultipartFile file) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(noteService.uploadImage(userId, file));
    }

    @PostMapping("/rebuild-index")
    @Operation(summary = "同步所有笔记到知识库")
    public Result<Integer> rebuildNoteIndex() {
        Long userId = SecurityUtils.getCurrentUserId();
        int count = noteService.rebuildNoteIndex(userId);
        return Result.success(count);
    }
}

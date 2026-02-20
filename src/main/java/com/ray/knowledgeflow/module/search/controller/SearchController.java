package com.ray.knowledgeflow.module.search.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ray.knowledgeflow.common.result.Result;
import com.ray.knowledgeflow.module.note.dto.NoteListDto;
import com.ray.knowledgeflow.module.search.service.ISearchService;
import com.ray.knowledgeflow.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 搜索控制器
 *
 * @author Ray
 * @since 2024-01-16
 */
@Tag(name = "搜索服务")
@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
public class SearchController {

    private final ISearchService searchService;

    @GetMapping("/notes")
    @Operation(summary = "全文搜索笔记")
    public Result<Page<NoteListDto>> searchNotes(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(searchService.searchNotes(keyword, userId, page, size));
    }

    @GetMapping("/semantic")
    @Operation(summary = "语义搜索内容")
    public Result<Page<com.ray.knowledgeflow.module.content.dto.ContentItemDto>> searchContentSemantic(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(searchService.searchContentSemantic(keyword, userId, page, size));
    }

    @GetMapping("/hybrid")
    @Operation(summary = "混合搜索内容")
    public Result<Page<com.ray.knowledgeflow.module.content.dto.ContentItemDto>> searchContentHybrid(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(searchService.searchContentHybrid(keyword, userId, page, size));
    }
}

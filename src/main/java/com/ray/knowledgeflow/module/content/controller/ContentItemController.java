package com.ray.knowledgeflow.module.content.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ray.knowledgeflow.common.result.Result;
import com.ray.knowledgeflow.module.content.dto.ContentItemDto;
import com.ray.knowledgeflow.module.content.service.IContentItemService;
import com.ray.knowledgeflow.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 内容项控制器
 *
 * @author Ray
 * @since 2024-01-16
 */
@RestController
@RequestMapping("/content/items")
@RequiredArgsConstructor
@Validated
public class ContentItemController {

    private final IContentItemService contentItemService;

    @GetMapping
    public Result<IPage<ContentItemDto>> listItems(
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long sourceId,
            @RequestParam(required = false) Boolean isUpload,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(contentItemService.pageByUserId(userId, status, sourceId, isUpload, keyword, page, size));
    }

    /**
     * 获取内容详情
     */
    @GetMapping("/{id}")
    public Result<ContentItemDto> getDetail(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(contentItemService.getDetail(userId, id));
    }

    /**
     * 更新内容状态
     */
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam @Min(0) @Max(4) Integer status) {
        Long userId = SecurityUtils.getCurrentUserId();
        contentItemService.updateStatus(userId, id, status);
        return Result.success();
    }

    /**
     * 批量更新状态
     */
    @PutMapping("/batch/status")
    public Result<Void> batchUpdateStatus(@RequestBody @Size(min = 1, max = 500) Long[] ids,
            @RequestParam @Min(0) @Max(4) Integer status) {
        Long userId = SecurityUtils.getCurrentUserId();
        contentItemService.batchUpdateStatus(userId, ids, status);
        return Result.success();
    }

    /**
     * 删除内容
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteItem(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        contentItemService.deleteItem(userId, id);
        return Result.success();
    }
}

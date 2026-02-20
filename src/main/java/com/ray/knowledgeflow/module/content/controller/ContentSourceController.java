package com.ray.knowledgeflow.module.content.controller;

import com.ray.knowledgeflow.common.result.Result;
import com.ray.knowledgeflow.module.content.dto.ContentSourceDto;
import com.ray.knowledgeflow.module.content.service.IContentSourceService;
import com.ray.knowledgeflow.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 内容源控制器
 *
 * @author Ray
 * @since 2024-01-16
 */
@RestController
@RequestMapping("/content/sources")
@RequiredArgsConstructor
public class ContentSourceController {

    private final IContentSourceService contentSourceService;

    /**
     * 获取用户的所有内容源
     */
    @GetMapping
    public Result<List<ContentSourceDto>> listSources() {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(contentSourceService.listByUserId(userId));
    }

    /**
     * 添加内容源
     */
    @PostMapping
    public Result<ContentSourceDto> addSource(@RequestBody ContentSourceDto dto) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(contentSourceService.addSource(userId, dto));
    }

    /**
     * 更新内容源
     */
    @PutMapping("/{id}")
    public Result<ContentSourceDto> updateSource(@PathVariable Long id, @RequestBody ContentSourceDto dto) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(contentSourceService.updateSource(userId, id, dto));
    }

    /**
     * 删除内容源
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteSource(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        contentSourceService.deleteSource(userId, id);
        return Result.success();
    }

    /**
     * 启用/禁用内容源
     */
    @PutMapping("/{id}/status")
    public Result<Void> toggleStatus(@PathVariable Long id, @RequestParam Integer status) {
        Long userId = SecurityUtils.getCurrentUserId();
        contentSourceService.toggleStatus(userId, id, status);
        return Result.success();
    }

    /**
     * 立即同步内容源 (异步)
     */
    @PostMapping("/{id}/sync")
    public Result<String> syncNow(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        contentSourceService.syncNowAsync(userId, id);
        return Result.success("同步任务已提交，正在后台处理");
    }

    /**
     * 同步抓取内容源 (同步执行，用于测试和调试)
     */
    @PostMapping("/{id}/sync-now")
    public Result<String> syncNowSync(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        contentSourceService.syncNow(userId, id);
        return Result.success("同步完成");
    }
}

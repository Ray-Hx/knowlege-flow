package com.ray.knowledgeflow.module.system.controller;

import com.ray.knowledgeflow.common.result.Result;
import com.ray.knowledgeflow.module.system.service.BackupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 系统管理控制器
 *
 * @author Ray
 * @since 2024-01-17
 */
@Tag(name = "系统管理")
@RestController
@RequestMapping("/system")
@RequiredArgsConstructor
public class SystemController {

    private final BackupService backupService;

    @GetMapping("/tasks")
    @Operation(summary = "获取定时任务列表")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Object> listTasks() {
        // Mock data for scheduled tasks
        Map<String, String> tasks = new HashMap<>();
        tasks.put("indexUpdate", "Running: Every 5s (when message arrives)");
        tasks.put("spacedReview", "Next Run: 00:00:00");
        tasks.put("rssSync", "Next Run: 02:00:00");
        return Result.success(tasks);
    }

    @GetMapping("/config")
    @Operation(summary = "获取系统配置")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Map<String, Object>> getConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("siteName", "KnowledgeFlow");
        config.put("allowRegistration", true);
        config.put("maxFileSize", "50MB");
        return Result.success(config);
    }

    @PutMapping("/config")
    @Operation(summary = "更新系统配置")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> updateConfig(@RequestBody Map<String, Object> config) {
        // In a real app, save to DB or Redis
        return Result.success();
    }

    @PostMapping("/backup")
    @Operation(summary = "触发手动备份")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<String> triggerBackup() {
        boolean success = backupService.performBackup();
        return success ? Result.success("备份成功") : Result.failed("备份失败");
    }
}

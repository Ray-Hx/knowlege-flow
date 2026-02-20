package com.ray.knowledgeflow.module.output.controller;

import com.ray.knowledgeflow.common.result.Result;
import com.ray.knowledgeflow.module.output.dto.HomepageConfigDto;
import com.ray.knowledgeflow.module.output.dto.PublicHomepageDto;
import com.ray.knowledgeflow.module.output.dto.VisitorStatsDto;
import com.ray.knowledgeflow.module.output.service.IHomepageService;
import com.ray.knowledgeflow.module.output.service.IVisitorService;
import com.ray.knowledgeflow.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 个人主页控制器
 *
 * @author Ray
 * @since 2024-01-26
 */
@Tag(name = "个人主页")
@RestController
@RequestMapping("/homepage")
@RequiredArgsConstructor
public class HomepageController {

    private final IHomepageService homepageService;
    private final IVisitorService visitorService;

    @GetMapping("/config")
    @Operation(summary = "获取主页配置")
    public Result<HomepageConfigDto> getConfig() {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(homepageService.getConfig(userId));
    }

    @PutMapping("/config")
    @Operation(summary = "更新主页配置")
    public Result<HomepageConfigDto> updateConfig(@RequestBody HomepageConfigDto dto) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(homepageService.updateConfig(userId, dto));
    }

    @GetMapping("/public/{slug}")
    @Operation(summary = "访问公开主页")
    public Result<PublicHomepageDto> getPublicHomepage(
            @PathVariable String slug,
            HttpServletRequest request) {
        PublicHomepageDto homepage = homepageService.getPublicHomepage(slug);

        // 记录访问
        visitorService.recordVisit(homepage.getUsername() != null ? SecurityUtils.getCurrentUserId() : null,
                "homepage", null, request);

        return Result.success(homepage);
    }

    @GetMapping("/slug/check")
    @Operation(summary = "检查slug是否可用")
    public Result<Boolean> checkSlug(@RequestParam String slug) {
        return Result.success(homepageService.isSlugAvailable(slug));
    }

    @GetMapping("/stats")
    @Operation(summary = "获取访客统计")
    public Result<VisitorStatsDto> getStats() {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(visitorService.getStats(userId));
    }
}

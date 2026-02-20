package com.ray.knowledgeflow.module.search.controller;

import com.ray.knowledgeflow.common.result.Result;
import com.ray.knowledgeflow.module.search.dto.RecommendationDto;
import com.ray.knowledgeflow.module.search.service.IRecommendationService;
import com.ray.knowledgeflow.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 智能推荐控制器
 */
@Tag(name = "推荐服务")
@RestController
@RequestMapping("/search/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final IRecommendationService recommendationService;

    @GetMapping
    @Operation(summary = "获取个性化推荐")
    public Result<List<RecommendationDto>> getRecommendations(
            @RequestParam(defaultValue = "10") Integer limit) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(recommendationService.getRecommendations(userId, limit));
    }
}

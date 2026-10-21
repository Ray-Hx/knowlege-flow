package com.ray.knowledgeflow.module.note.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ray.knowledgeflow.common.result.Result;
import com.ray.knowledgeflow.module.dashboard.dto.DailyStudyDto;
import com.ray.knowledgeflow.module.dashboard.service.IDashboardService;
import com.ray.knowledgeflow.module.graph.entity.NoteNode;
import com.ray.knowledgeflow.module.graph.service.IGraphService;
import com.ray.knowledgeflow.module.note.dto.NoteDetailDto;
import com.ray.knowledgeflow.module.note.dto.NoteListDto;
import com.ray.knowledgeflow.module.note.service.INoteService;
import com.ray.knowledgeflow.module.note.service.NoteEngagementService;
import com.ray.knowledgeflow.module.user.dto.UserPublicProfileDto;
import com.ray.knowledgeflow.module.user.service.IUserRelationService;
import com.ray.knowledgeflow.module.user.service.IUserProfileService;
import com.ray.knowledgeflow.common.exception.BusinessException;
import com.ray.knowledgeflow.security.SecurityUtils;
import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 社区分享控制器
 *
 * @author Ray
 * @since 2024-01-29
 */
@Tag(name = "社区分享")
@RestController
@RequestMapping("/community")
@RequiredArgsConstructor
@Validated
public class CommunityController {

    private final INoteService noteService;
    private final NoteEngagementService engagementService;
    private final IDashboardService dashboardService;
    private final IGraphService graphService;
    private final IUserRelationService userRelationService;
    private final IUserProfileService userProfileService;

    @GetMapping("/feed")
    @Operation(summary = "获取社区笔记流")
    public Result<IPage<NoteListDto>> getCommunityFeed(
            @RequestParam(required = false) @Size(max = 100) String keyword,
            @RequestParam(defaultValue = "1") @Min(1) Integer page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) Integer size) {
        return Result.success(noteService.listCommunityNotes(keyword, page, size));
    }

    @GetMapping("/feed/following")
    @Operation(summary = "获取我关注的用户的社区笔记流")
    public Result<IPage<NoteListDto>> getFollowingFeed(
            @RequestParam(defaultValue = "1") @Min(1) Integer page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) Integer size) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(noteService.listFollowingNotes(userId, page, size));
    }

    @GetMapping("/note/{id}")
    @Operation(summary = "获取公开笔记详情")
    public Result<NoteDetailDto> getPublicNoteDetail(@PathVariable Long id) {
        // 获取当前用户ID（可能未登录）
        Long currentUserId = null;
        try {
            currentUserId = SecurityUtils.getCurrentUserId();
        } catch (Exception ignored) {
        }

        // 先取详情：笔记不存在或未公开时会抛出 404，
        // 避免未登录用户对任意 ID（含私有笔记）刷浏览量
        NoteDetailDto detail = noteService.getPublicNoteDetail(id, currentUserId);

        engagementService.incrementViewCount(id);
        // 详情在自增前取出，回填以包含本次访问
        detail.setViewCount(engagementService.getViewCount(id));

        return Result.success(detail);
    }

    @PostMapping("/note/{id}/like")
    @Operation(summary = "点赞/取消点赞笔记")
    public Result<NoteEngagementService.LikeResult> toggleLike(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(engagementService.toggleLike(userId, id));
    }

    @PostMapping("/users/{userId}/follow")
    @Operation(summary = "关注用户")
    public Result<Void> followUser(@PathVariable Long userId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        userRelationService.follow(currentUserId, userId);
        return Result.success();
    }

    @DeleteMapping("/users/{userId}/unfollow")
    @Operation(summary = "取消关注用户")
    public Result<Void> unfollowUser(@PathVariable Long userId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        userRelationService.unfollow(currentUserId, userId);
        return Result.success();
    }

    @GetMapping("/users/{userId}/profile")
    @Operation(summary = "获取用户公开主页资料")
    public Result<UserPublicProfileDto> getUserPublicProfile(@PathVariable Long userId) {
        requirePublicProfile(userId);
        return Result.success(noteService.getUserPublicProfile(userId));
    }

    @GetMapping("/users/{userId}/notes")
    @Operation(summary = "获取用户的公开笔记")
    public Result<IPage<NoteListDto>> getUserPublicNotes(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") @Min(1) Integer page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) Integer size) {
        return Result.success(noteService.listPublicNotes(userId, page, size));
    }

    @GetMapping("/users/{userId}/heatmap")
    @Operation(summary = "获取用户全年学习热力图")
    public Result<List<DailyStudyDto>> getUserHeatmap(@PathVariable Long userId) {
        if (!userProfileService.isPublicStats(userId)) {
            // 使用 404 避免向匿名请求暴露用户是否存在或其隐私设置。
            throw new BusinessException(404, "用户不存在或未公开学习统计");
        }
        return Result.success(dashboardService.getYearlyHeatmap(userId));
    }

    @GetMapping("/note/{id}/related")
    @Operation(summary = "获取推荐的相关公开笔记")
    public Result<List<NoteListDto>> getRelatedPublicNotes(
            @PathVariable Long id,
            @RequestParam(defaultValue = "5") @Min(1) @Max(50) Integer limit) {
        return Result.success(noteService.getRelatedPublicNotes(id, limit));
    }

    @GetMapping("/galaxy")
    @Operation(summary = "获取知识星空")
    public Result<List<NoteNode>> getGalaxyGraph(
            @Parameter(description = "节点数量限制", example = "300")
            @RequestParam(defaultValue = "300") @Min(1) @Max(500) Integer limit) {
        return Result.success(graphService.getCommunityGraph(limit));
    }

    @GetMapping("/ranking")
    @Operation(summary = "获取社区热力排行榜")
    public Result<List<NoteListDto>> getRanking(
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) Integer limit) {
        return Result.success(noteService.getHotNotes(limit));
    }

    private void requirePublicProfile(Long userId) {
        if (!userProfileService.isPublicProfile(userId)) {
            throw new BusinessException(404, "用户不存在或个人主页未公开");
        }
    }
}

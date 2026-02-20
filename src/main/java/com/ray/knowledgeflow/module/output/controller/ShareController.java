package com.ray.knowledgeflow.module.output.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ray.knowledgeflow.common.result.Result;
import com.ray.knowledgeflow.module.output.dto.ShareLinkDto;
import com.ray.knowledgeflow.module.output.service.IShareService;
import com.ray.knowledgeflow.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 分享控制器
 *
 * @author Ray
 * @since 2024-01-26
 */
@Tag(name = "分享管理")
@RestController
@RequestMapping("/share")
@RequiredArgsConstructor
public class ShareController {

    private final IShareService shareService;

    @PostMapping("/create")
    @Operation(summary = "创建分享链接")
    public Result<ShareLinkDto> createShare(
            @RequestParam String resourceType,
            @RequestParam Long resourceId,
            @RequestParam(required = false) String password,
            @RequestParam(defaultValue = "0") Integer days) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(shareService.createShareLink(userId, resourceType, resourceId, password, days));
    }

    @GetMapping("/{shareCode}")
    @Operation(summary = "访问分享内容")
    public Result<Object> getSharedContent(
            @PathVariable String shareCode,
            @RequestParam(required = false) String password) {
        return Result.success(shareService.getSharedContent(shareCode, password));
    }

    @GetMapping("/list")
    @Operation(summary = "我的分享列表")
    public Result<IPage<ShareLinkDto>> listMyShares(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(shareService.listMyShares(userId, page, size));
    }

    @DeleteMapping("/{shareCode}")
    @Operation(summary = "删除分享")
    public Result<Void> deleteShare(@PathVariable String shareCode) {
        Long userId = SecurityUtils.getCurrentUserId();
        shareService.deleteShare(userId, shareCode);
        return Result.success();
    }

    @PutMapping("/{shareCode}/status")
    @Operation(summary = "更新分享状态")
    public Result<Void> updateStatus(
            @PathVariable String shareCode,
            @RequestParam Integer status) {
        Long userId = SecurityUtils.getCurrentUserId();
        shareService.updateStatus(userId, shareCode, status);
        return Result.success();
    }
}

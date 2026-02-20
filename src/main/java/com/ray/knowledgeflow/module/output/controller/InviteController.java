package com.ray.knowledgeflow.module.output.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ray.knowledgeflow.common.result.Result;
import com.ray.knowledgeflow.module.output.dto.InviteCodeDto;
import com.ray.knowledgeflow.module.output.service.IInviteService;
import com.ray.knowledgeflow.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 邀请码控制器
 *
 * @author Ray
 * @since 2024-01-26
 */
@Tag(name = "邀请码管理")
@RestController
@RequestMapping("/invite")
@RequiredArgsConstructor
public class InviteController {

    private final IInviteService inviteService;

    @PostMapping("/generate")
    @Operation(summary = "生成邀请码")
    public Result<InviteCodeDto> generateCode(@RequestParam(defaultValue = "0") Integer days) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(inviteService.generateCode(userId, days));
    }

    @PostMapping("/use")
    @Operation(summary = "使用邀请码")
    public Result<Boolean> useCode(@RequestParam String code) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(inviteService.useCode(code, userId));
    }

    @GetMapping("/list")
    @Operation(summary = "我的邀请码列表")
    public Result<IPage<InviteCodeDto>> listMyCodes(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(inviteService.listMyCodes(userId, page, size));
    }

    @GetMapping("/stats")
    @Operation(summary = "邀请统计")
    public Result<IInviteService.InviteStatsDto> getStats() {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(inviteService.getStats(userId));
    }
}

package com.ray.knowledgeflow.module.user.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ray.knowledgeflow.common.result.Result;
import com.ray.knowledgeflow.module.user.dto.message.MessageConversationDto;
import com.ray.knowledgeflow.module.user.dto.message.SendMessageDto;
import com.ray.knowledgeflow.module.user.dto.message.UserMessageDto;
import com.ray.knowledgeflow.module.user.service.IMessageService;
import com.ray.knowledgeflow.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.util.List;

/**
 * 用户消息中心接口
 */
@Tag(name = "消息接口")
@RestController
@RequestMapping("/messages")
@RequiredArgsConstructor
@Validated
public class MessageController {

    private final IMessageService messageService;

    @PostMapping("/send")
    @Operation(summary = "发送私信")
    public Result<Void> sendMessage(@Valid @RequestBody SendMessageDto dto) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        messageService.sendMessage(currentUserId, dto);
        return Result.success(null);
    }

    @GetMapping("/history/{contactId}")
    @Operation(summary = "获取与某人的聊天历史")
    public Result<Page<UserMessageDto>> getChatHistory(
            @PathVariable @Min(1) Long contactId,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        return Result.success(messageService.getChatHistory(currentUserId, contactId, page, size));
    }

    @GetMapping("/conversations")
    @Operation(summary = "获取当前用户的会话列表")
    public Result<List<MessageConversationDto>> getConversations() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        return Result.success(messageService.getConversations(currentUserId));
    }

    @PostMapping("/read/{contactId}")
    @Operation(summary = "将来自某人的消息全部标为已读")
    public Result<Void> markAsRead(@PathVariable Long contactId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        messageService.markAsRead(currentUserId, contactId);
        return Result.success(null);
    }

    @GetMapping("/unread")
    @Operation(summary = "获取全局未读消息总数")
    public Result<Long> getUnreadTotal() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        return Result.success(messageService.getUnreadTotal(currentUserId));
    }
}

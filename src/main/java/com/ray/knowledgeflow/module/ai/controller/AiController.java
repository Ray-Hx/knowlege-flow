package com.ray.knowledgeflow.module.ai.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ray.knowledgeflow.common.result.Result;
import com.ray.knowledgeflow.module.ai.dto.ConversationDto;
import com.ray.knowledgeflow.module.ai.dto.MessageDto;
import com.ray.knowledgeflow.module.ai.dto.SendMessageDto;
import com.ray.knowledgeflow.module.ai.service.IAiService;
import com.ray.knowledgeflow.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AI助手控制器
 *
 * @author Ray
 * @since 2024-01-17
 */
@Tag(name = "AI助手")
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AiController {

    private final IAiService aiService;

    @PostMapping("/conversations")
    @Operation(summary = "创建对话")
    public Result<ConversationDto> createConversation(@RequestParam(required = false) String title) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(aiService.createConversation(userId, title));
    }

    @GetMapping("/conversations")
    @Operation(summary = "获取对话列表")
    public Result<IPage<ConversationDto>> listConversations(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(aiService.listConversations(userId, page, size));
    }

    @DeleteMapping("/conversations/{id}")
    @Operation(summary = "删除对话")
    public Result<Void> deleteConversation(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        aiService.deleteConversation(userId, id);
        return Result.success();
    }

    @GetMapping("/conversations/{id}/messages")
    @Operation(summary = "获取消息记录")
    public Result<List<MessageDto>> getMessages(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(aiService.getMessages(userId, id));
    }

    @PostMapping("/chat/send")
    @Operation(summary = "发送消息")
    public Result<MessageDto> sendMessage(@Valid @RequestBody SendMessageDto dto) {
        Long userId = SecurityUtils.getCurrentUserId();

        return Result.success(aiService.sendMessage(userId, dto.getConversationId(), dto));
    }

    @PostMapping(value = "/chat/send/stream", produces = org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "流式发送消息")
    public org.springframework.web.servlet.mvc.method.annotation.SseEmitter streamSendMessage(
            @Valid @RequestBody SendMessageDto dto, jakarta.servlet.http.HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("X-Accel-Buffering", "no"); // 禁用 Nginx 缓冲
        Long userId = SecurityUtils.getCurrentUserId();
        return aiService.streamChat(userId, dto.getConversationId(), dto);
    }

    @PostMapping("/analyze")
    @Operation(summary = "生成深度解析")
    public Result<com.ray.knowledgeflow.module.ai.dto.AiAnalysisResultDto> generateDeepAnalysis(
            @RequestBody java.util.Map<String, Object> request) {
        String content = (String) request.get("content");
        String targetType = (String) request.get("targetType"); // NOTE, CONCEPT, SOURCE
        Long targetId = request.get("targetId") != null ? Long.valueOf(request.get("targetId").toString()) : null;

        if (content == null || content.trim().isEmpty()) {
            return Result.fail("内容不能为空");
        }
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(aiService.generateDeepAnalysis(userId, content, targetType, targetId));
    }

    @PostMapping(value = "/analyze/stream")
    @Operation(summary = "流式生成深度解析")
    public org.springframework.web.servlet.mvc.method.annotation.SseEmitter streamDeepAnalysis(
            @RequestBody java.util.Map<String, Object> request) {
        String content = (String) request.get("content");
        String targetType = (String) request.get("targetType");
        Long targetId = request.get("targetId") != null ? Long.valueOf(request.get("targetId").toString()) : null;

        Long userId = SecurityUtils.getCurrentUserId();
        return aiService.streamDeepAnalysis(userId, content, targetType, targetId);
    }

    @PostMapping("/text/polish")
    @Operation(summary = "文本润色")
    public Result<String> polishText(@RequestBody java.util.Map<String, String> request) {
        String text = request.get("text");
        if (text == null || text.trim().isEmpty()) {
            return Result.fail("文本不能为空");
        }
        return Result.success(aiService.polishText(text));
    }

    @PostMapping("/text/translate")
    @Operation(summary = "文本翻译")
    public Result<String> translateText(@RequestBody java.util.Map<String, String> request) {
        String text = request.get("text");
        String targetLang = request.getOrDefault("targetLang", "en");
        if (text == null || text.trim().isEmpty()) {
            return Result.fail("文本不能为空");
        }
        return Result.success(aiService.translateText(text, targetLang));
    }

    @PostMapping("/text/expand")
    @Operation(summary = "文本扩写")
    public Result<String> expandText(@RequestBody java.util.Map<String, String> request) {
        String text = request.get("text");
        if (text == null || text.trim().isEmpty()) {
            return Result.fail("文本不能为空");
        }
        return Result.success(aiService.expandText(text));
    }
}

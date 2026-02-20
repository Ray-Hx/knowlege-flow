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
    public Result<MessageDto> sendMessage(@RequestBody SendMessageDto dto) {
        Long userId = SecurityUtils.getCurrentUserId();
        // Assuming conversationId is passed in dto for simplicity in this endpoint,
        // or we could use path param. defined in plan as /ai/chat/send
        // The service method signature is: MessageDto sendMessage(Long userId, Long
        // conversationId, SendMessageDto dto);
        // But SendMessageDto usually also contains conversationId? Let's check
        // SendMessageDto.
        // Actually, looking at previous view of AiServiceImpl:
        // public MessageDto sendMessage(Long userId, Long conversationId,
        // SendMessageDto dto)
        // Frontend passes conversationId in body: { conversationId, message, enableRags
        // }
        // Let's assume SendMessageDto has conversationId based on frontend api.ts:
        // sendMessage(conversationId: number, message: string) { return request.post...
        // { conversationId, message... } }

        return Result.success(aiService.sendMessage(userId, dto.getConversationId(), dto));
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
        return Result.success(aiService.generateDeepAnalysis(content, targetType, targetId));
    }

    @PostMapping(value = "/analyze/stream")
    @Operation(summary = "流式生成深度解析")
    public org.springframework.web.servlet.mvc.method.annotation.SseEmitter streamDeepAnalysis(
            @RequestBody java.util.Map<String, Object> request) {
        String content = (String) request.get("content");
        String targetType = (String) request.get("targetType");
        Long targetId = request.get("targetId") != null ? Long.valueOf(request.get("targetId").toString()) : null;

        return aiService.streamDeepAnalysis(content, targetType, targetId);
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

package com.ray.knowledgeflow.module.ai.dto;

import lombok.Data;

/**
 * AI 聊天请求参数
 *
 * @author Ray
 * @since 2024-01-21
 */
@Data
public class ChatRequestDto {

    /**
     * 用户消息
     */
    private String message;

    /**
     * 对话ID (首次对话可为空)
     */
    private Long conversationId;
}

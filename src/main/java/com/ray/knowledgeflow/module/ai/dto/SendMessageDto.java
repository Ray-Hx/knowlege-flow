package com.ray.knowledgeflow.module.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 发送消息DTO
 *
 * @author Ray
 * @since 2024-01-17
 */
@Data
public class SendMessageDto {
    @NotBlank(message = "消息内容不能为空")
    private String content;

    /**
     * 是否使用RAG检索增强
     */
    private Boolean useRag = true;

    /**
     * 对话ID
     */
    private Long conversationId;
}

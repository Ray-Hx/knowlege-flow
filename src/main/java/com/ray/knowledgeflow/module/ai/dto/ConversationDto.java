package com.ray.knowledgeflow.module.ai.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 对话DTO
 *
 * @author Ray
 * @since 2024-01-17
 */
@Data
public class ConversationDto {
    private Long id;
    private String title;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;

    /**
     * 最后一条消息（用于列表展示）
     */
    private MessageDto lastMessage;
}

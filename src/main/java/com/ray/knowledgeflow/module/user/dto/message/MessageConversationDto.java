package com.ray.knowledgeflow.module.user.dto.message;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户会话响应 DTO
 */
@Data
public class MessageConversationDto {

    private Long contactId;
    private String contactName;
    private String contactAvatar;

    private String lastMessage;
    private LocalDateTime lastTime;
    private Integer unreadCount;
}

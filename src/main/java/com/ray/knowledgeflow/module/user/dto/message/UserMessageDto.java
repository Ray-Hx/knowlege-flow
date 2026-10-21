package com.ray.knowledgeflow.module.user.dto.message;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户消息响应 DTO
 */
@Data
public class UserMessageDto {

    private Long id;

    private Long senderId;
    private String senderName;
    private String senderAvatar;

    private Long receiverId;
    private String receiverName;
    private String receiverAvatar;

    private String content;

    private Integer isRead;

    private LocalDateTime createdTime;
}

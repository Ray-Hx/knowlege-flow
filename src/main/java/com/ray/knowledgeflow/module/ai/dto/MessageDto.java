package com.ray.knowledgeflow.module.ai.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 消息DTO
 *
 * @author Ray
 * @since 2024-01-17
 */
@Data
public class MessageDto {
    private Long id;
    private String role;
    private String content;
    private List<NoteReferenceDto> references;
    private LocalDateTime createdTime;
}

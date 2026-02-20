package com.ray.knowledgeflow.module.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 最近笔记 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecentNoteDto {

    private Long id;

    private String title;

    private String summary;

    private LocalDateTime updatedTime;

    /**
     * 更新时间的友好显示 (如: 2小时前)
     */
    private String updatedTimeText;
}

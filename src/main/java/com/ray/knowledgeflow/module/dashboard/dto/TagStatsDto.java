package com.ray.knowledgeflow.module.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 标签统计数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TagStatsDto {

    /**
     * 标签名称
     */
    private String tag;

    /**
     * 该标签下的笔记数量
     */
    private Integer noteCount;
}

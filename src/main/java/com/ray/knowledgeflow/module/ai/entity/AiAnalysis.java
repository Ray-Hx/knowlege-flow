package com.ray.knowledgeflow.module.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ray.knowledgeflow.module.ai.enums.AiAnalysisTargetType;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 深度解析记录
 *
 * @author Ray
 * @since 2024-01-27
 */
@Data
@TableName("ai_analysis")
public class AiAnalysis {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 目标类型 (NOTE, CONCEPT, SOURCE)
     */
    private AiAnalysisTargetType targetType;

    /**
     * 目标ID
     */
    private Long targetId;

    /**
     * 解析内容 (Markdown)
     */
    private String content;

    /**
     * 创建时间
     */
    private LocalDateTime createdTime;

    /**
     * 更新时间
     */
    private LocalDateTime updatedTime;
}

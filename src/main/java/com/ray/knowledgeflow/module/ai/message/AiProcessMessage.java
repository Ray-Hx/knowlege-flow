package com.ray.knowledgeflow.module.ai.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * AI 处理消息 DTO
 *
 * @author Ray
 * @since 2024-01-20
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiProcessMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 任务类型: SUMMARY(摘要生成), TAG(标签提取), GRAPH(图谱关系)
     */
    private String taskType;

    /**
     * 笔记ID
     */
    private Long noteId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 笔记标题
     */
    private String title;

    /**
     * 笔记内容
     */
    private String content;
}

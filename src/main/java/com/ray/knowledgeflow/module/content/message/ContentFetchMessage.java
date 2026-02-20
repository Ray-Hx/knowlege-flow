package com.ray.knowledgeflow.module.content.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 内容抓取消息 DTO
 *
 * @author Ray
 * @since 2024-01-20
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContentFetchMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 内容源类型: RSS, GITHUB, ARXIV, FILE
     */
    private String type;

    /**
     * 内容源ID
     */
    private Long sourceId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 额外参数 (JSON字符串)
     */
    private String extraParams;
}

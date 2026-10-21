package com.ray.knowledgeflow.module.search.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 索引更新消息 DTO
 *
 * @author Ray
 * @since 2024-01-20
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IndexUpdateMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 操作类型: CREATE, UPDATE, DELETE
     */
    private String action;

    /**
     * 文档类型: NOTE, CONTENT_ITEM
     */
    private String documentType;

    /**
     * 文档ID
     */
    private Long documentId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 文档标题
     */
    private String title;

    /**
     * 文档内容
     */
    private String content;

    /**
     * AI生成的摘要
     */
    private String summary;

    /**
     * 标签
     */
    private String tags;
}

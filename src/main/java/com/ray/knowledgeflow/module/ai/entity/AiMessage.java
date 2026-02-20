package com.ray.knowledgeflow.module.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * AI消息实体类
 * </p>
 *
 * @author Ray
 * @since 2024-01-17
 */
@Data
@TableName(value = "ai_message", autoResultMap = true)
public class AiMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 对话ID
     */
    private Long conversationId;

    /**
     * 角色 (user/assistant)
     */
    private String role;

    /**
     * 消息内容
     */
    @TableField("content")
    private String content;

    /**
     * 引用来源 (JSON)
     */
    @TableField(value = "`references`", typeHandler = JacksonTypeHandler.class)
    private String references;

    /**
     * 创建时间
     */
    private LocalDateTime createdTime;
}

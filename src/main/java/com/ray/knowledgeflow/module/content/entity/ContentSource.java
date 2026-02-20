package com.ray.knowledgeflow.module.content.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 内容源实体类 (RSS, API等)
 * </p>
 *
 * @author Ray
 * @since 2024-01-14
 */
@Data
@TableName("content_source")
public class ContentSource implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 所属用户ID
     */
    private Long userId;

    /**
     * 源类型: RSS, GITHUB, ARXIV 等
     */
    private String type;

    /**
     * 源名称
     */
    private String name;

    /**
     * 源URL
     */
    private String url;

    /**
     * 额外配置参数 (JSON格式)
     */
    private String config;

    /**
     * 同步间隔 (分钟)
     */
    private Integer syncInterval;

    /**
     * 上次同步时间
     */
    private LocalDateTime lastSyncTime;

    /**
     * 状态: 1-启用, 0-禁用
     */
    private Integer status;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    /**
     * 逻辑删除
     */
    @TableLogic
    private Integer deleted;
}

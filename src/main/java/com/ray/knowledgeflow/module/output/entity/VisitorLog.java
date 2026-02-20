package com.ray.knowledgeflow.module.output.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 访客记录实体类
 *
 * @author Ray
 * @since 2024-01-26
 */
@Data
@TableName("visitor_log")
public class VisitorLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 被访问用户ID
     */
    private Long userId;

    /**
     * 访客IP
     */
    private String visitorIp;

    /**
     * 访客UserAgent
     */
    private String visitorUa;

    /**
     * 来源
     */
    private String referer;

    /**
     * 访问资源类型
     */
    private String resourceType;

    /**
     * 访问资源ID
     */
    private Long resourceId;

    /**
     * 访问时间
     */
    private LocalDateTime visitTime;
}

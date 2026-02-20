package com.ray.knowledgeflow.module.output.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 分享链接DTO
 *
 * @author Ray
 * @since 2024-01-26
 */
@Data
public class ShareLinkDto {

    /**
     * 分享码
     */
    private String shareCode;

    /**
     * 资源类型
     */
    private String resourceType;

    /**
     * 资源ID
     */
    private Long resourceId;

    /**
     * 资源标题
     */
    private String resourceTitle;

    /**
     * 是否有密码
     */
    private Boolean hasPassword;

    /**
     * 过期时间
     */
    private LocalDateTime expireTime;

    /**
     * 查看次数
     */
    private Integer viewCount;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createdTime;
}

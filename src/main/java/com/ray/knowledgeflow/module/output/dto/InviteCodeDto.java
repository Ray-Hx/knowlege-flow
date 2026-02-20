package com.ray.knowledgeflow.module.output.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 邀请码DTO
 *
 * @author Ray
 * @since 2024-01-26
 */
@Data
public class InviteCodeDto {

    /**
     * 邀请码
     */
    private String code;

    /**
     * 状态：0-未使用，1-已使用，2-已过期
     */
    private Integer status;

    /**
     * 过期时间
     */
    private LocalDateTime expireTime;

    /**
     * 使用者用户名
     */
    private String usedByUsername;

    /**
     * 使用时间
     */
    private LocalDateTime usedTime;

    /**
     * 创建时间
     */
    private LocalDateTime createdTime;
}

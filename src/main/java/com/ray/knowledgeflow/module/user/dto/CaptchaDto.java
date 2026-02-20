package com.ray.knowledgeflow.module.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 验证码响应 DTO
 *
 * @author Ray
 * @since 2024-01-17
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CaptchaDto {
    /**
     * 验证码唯一标识，登录时需要带回
     */
    private String captchaKey;

    /**
     * Base64 编码的验证码图片
     */
    private String captchaImage;
}

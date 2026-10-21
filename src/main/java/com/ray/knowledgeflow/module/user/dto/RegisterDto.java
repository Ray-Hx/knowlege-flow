package com.ray.knowledgeflow.module.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 注册请求参数
 *
 * @author Ray
 * @since 2024-01-14
 */
@Data
public class RegisterDto {
    /**
     * 用户名
     */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 1, max = 20, message = "用户名长度必须在1-20个字符之间")
    private String username;

    /**
     * 密码
     */
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 50, message = "密码长度必须在6-50个字符之间")
    private String password;

    /**
     * 邮箱
     */
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    /**
     * 验证码 key
     */
    @NotBlank(message = "验证码标识不能为空")
    private String captchaKey;

    /**
     * 验证码
     */
    @NotBlank(message = "验证码不能为空")
    private String captchaCode;
}

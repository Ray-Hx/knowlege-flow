package com.ray.knowledgeflow.module.user.controller;

import com.ray.knowledgeflow.common.result.Result;
import com.ray.knowledgeflow.module.user.dto.CaptchaDto;
import com.ray.knowledgeflow.module.user.dto.LoginDto;
import com.ray.knowledgeflow.module.user.dto.RegisterDto;
import com.ray.knowledgeflow.module.user.entity.SysUser;
import com.ray.knowledgeflow.module.user.service.CaptchaService;
import com.ray.knowledgeflow.module.user.service.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 *
 * @author Ray
 * @since 2024-01-14
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final IUserService userService;
    private final CaptchaService captchaService;

    /**
     * 获取图形验证码
     */
    @GetMapping("/captcha")
    public Result<CaptchaDto> captcha() {
        CaptchaDto captcha = captchaService.generate();
        return Result.success(captcha);
    }

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public Result<SysUser> register(@RequestBody @Validated RegisterDto registerDto) {
        SysUser user = userService.register(registerDto);
        return Result.success(user);
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result<String> login(@RequestBody @Validated LoginDto loginDto) {
        String token = userService.login(loginDto);
        return Result.success(token);
    }
}

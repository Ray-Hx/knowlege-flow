package com.ray.knowledgeflow.module.user.service;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import cn.hutool.core.util.IdUtil;
import com.ray.knowledgeflow.module.user.dto.CaptchaDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 验证码服务
 *
 * @author Ray
 * @since 2024-01-17
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CaptchaService {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 验证码 Redis key 前缀
     */
    private static final String CAPTCHA_KEY_PREFIX = "captcha:";

    /**
     * 验证码过期时间（分钟）
     */
    private static final long CAPTCHA_EXPIRE_MINUTES = 5;

    /**
     * 验证码图片宽度
     */
    private static final int CAPTCHA_WIDTH = 130;

    /**
     * 验证码图片高度
     */
    private static final int CAPTCHA_HEIGHT = 48;

    /**
     * 验证码字符数
     */
    private static final int CAPTCHA_CODE_COUNT = 4;

    /**
     * 干扰线数量
     */
    private static final int CAPTCHA_LINE_COUNT = 100;

    /**
     * 生成验证码
     *
     * @return 验证码 DTO (包含 key 和 base64 图片)
     */
    public CaptchaDto generate() {
        // 生成验证码
        LineCaptcha captcha = CaptchaUtil.createLineCaptcha(
                CAPTCHA_WIDTH, CAPTCHA_HEIGHT, CAPTCHA_CODE_COUNT, CAPTCHA_LINE_COUNT);

        // 获取验证码文本
        String code = captcha.getCode();

        // 生成唯一 key
        String captchaKey = IdUtil.fastSimpleUUID();

        // 存储到 Redis
        String redisKey = CAPTCHA_KEY_PREFIX + captchaKey;
        redisTemplate.opsForValue().set(redisKey, code.toLowerCase(), CAPTCHA_EXPIRE_MINUTES, TimeUnit.MINUTES);

        log.debug("生成验证码: key={}, code={}", captchaKey, code);

        // 返回 base64 图片
        String base64Image = "data:image/png;base64," + captcha.getImageBase64();

        return new CaptchaDto(captchaKey, base64Image);
    }

    /**
     * 验证验证码
     *
     * @param captchaKey  验证码 key
     * @param captchaCode 用户输入的验证码
     * @return 验证是否成功
     */
    public boolean verify(String captchaKey, String captchaCode) {
        if (captchaKey == null || captchaCode == null) {
            return false;
        }

        String redisKey = CAPTCHA_KEY_PREFIX + captchaKey;
        Object storedCode = redisTemplate.opsForValue().get(redisKey);

        if (storedCode == null) {
            log.debug("验证码已过期或不存在: key={}", captchaKey);
            return false;
        }

        // 删除验证码（一次性使用）
        redisTemplate.delete(redisKey);

        // 忽略大小写比较
        boolean result = captchaCode.toLowerCase().equals(storedCode.toString());
        log.debug("验证码校验: key={}, input={}, result={}", captchaKey, captchaCode, result);

        return result;
    }
}

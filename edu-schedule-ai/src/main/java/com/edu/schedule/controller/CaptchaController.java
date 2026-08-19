package com.edu.schedule.controller;

import com.edu.schedule.common.Result;
import com.edu.schedule.service.CaptchaService;
import com.edu.schedule.service.RateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 图形验证码接口（公开，无需登录）。
 *
 * 设计选择：返回 JSON（{captchaKey, imageBase64, expireSeconds}），
 *         而非直接 image/png 字节流。
 * 原因：
 *   · 避免 <img src="/api/captcha/generate"> 被 CORS / 302 重定向 / 浏览器缓存 坑（经验 #216926）
 *   · 前端可以 axios 拉 → URL.createObjectURL 或 data:image/png;base64,xxx → <img> 通用
 *   · 前后端契约统一走 application/json，与其他 /api/* 保持一致
 */
@RestController
@RequestMapping("/api/captcha")
public class CaptchaController {

    private final CaptchaService captchaService;
    private final RateLimitService rateLimitService;

    public CaptchaController(CaptchaService captchaService, RateLimitService rateLimitService) {
        this.captchaService = captchaService;
        this.rateLimitService = rateLimitService;
    }

    /**
     * 生成图形验证码（GET，幂等，每次都是新的）。
     * 前端 <img :src="'data:image/png;base64,' + r.data.imageBase64" />
     * 注册提交时把 captchaKey + captchaCode 一起带到 /api/auth/register
     */
    @GetMapping("/generate")
    public Result<Map<String, Object>> generate(HttpServletRequest req) {
        // 先限流（画图占 CPU，单 IP 60s 30 次上限），防止被打成 100% CPU
        rateLimitService.checkAndCountCaptcha(req);
        return Result.success(captchaService.generate());
    }
}
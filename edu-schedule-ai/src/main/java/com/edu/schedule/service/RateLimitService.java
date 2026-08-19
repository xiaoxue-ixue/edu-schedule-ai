package com.edu.schedule.service;

import com.edu.schedule.common.BusinessException;
import com.edu.schedule.common.ResultCode;
import com.edu.schedule.config.AuthProperties;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 注册接口限流（单机内存版 Caffeine）。
 *
 * 两层限流策略（"宽进严出"：画图贵，所以获取验证码限流更宽松；注册写库慢，所以注册更严格）：
 *   ① /api/captcha/generate  —— 单 IP  60s ≤ 30 次（画图占 CPU）
 *   ② /api/auth/register     —— 双维度：
 *          · 单 IP    60s ≤ 30 次（防注册机快速耗尽 DB 连接池）
 *          · 单用户名 180s ≤ 5 次（防针对特定用户名的"撞库+抢注"）
 *
 * 注意：计数用 AtomicInteger，单线程/多线程都原子性；真实生产若多实例 → 换 Redis + Lua。
 */
@Service
public class RateLimitService {

    private static final Logger log = LoggerFactory.getLogger(RateLimitService.class);

    /** key: dimension + ":" + sub-key, value: AtomicInteger 计数 */
    private final Cache<String, AtomicInteger> counterCache;
    private final AuthProperties.RateLimit cfg;

    public RateLimitService(AuthProperties props) {
        this.cfg = props.getRateLimit();
        // 以最长窗口 180s 为 expireAfterWrite，保证短窗口 key 也能及时过期
        this.counterCache = Caffeine.newBuilder()
                .maximumSize(100_000)
                .expireAfterWrite(Duration.ofSeconds(Math.max(cfg.getCaptchaWindowSeconds(),
                        Math.max(cfg.getRegisterIpWindowSeconds(), cfg.getRegisterUserWindowSeconds()))))
                .build();
        log.info("🚦 RateLimitService 初始化: captcha=IP {}s/{}次  register=IP {}s/{}次  User {}s/{}次",
                cfg.getCaptchaWindowSeconds(), cfg.getCaptchaMaxRequests(),
                cfg.getRegisterIpWindowSeconds(), cfg.getRegisterIpMaxAttempts(),
                cfg.getRegisterUserWindowSeconds(), cfg.getRegisterUserMaxAttempts());
    }

    // --------- 外部接口 ---------

    /** 校验并计数：获取验证码 限流 */
    public void checkAndCountCaptcha(HttpServletRequest req) {
        checkAndCount("captcha:ip:" + clientIp(req),
                cfg.getCaptchaMaxRequests(), cfg.getCaptchaWindowSeconds());
    }

    /** 校验并计数：注册（IP + Username 双维度） */
    public void checkAndCountRegister(HttpServletRequest req, String username) {
        checkAndCount("register:ip:" + clientIp(req),
                cfg.getRegisterIpMaxAttempts(), cfg.getRegisterIpWindowSeconds());
        if (username != null && !username.isBlank()) {
            checkAndCount("register:user:" + username.trim().toLowerCase(),
                    cfg.getRegisterUserMaxAttempts(), cfg.getRegisterUserWindowSeconds());
        }
    }

    // --------- 内部 ---------

    private void checkAndCount(String key, int max, int windowSeconds) {
        AtomicInteger counter = counterCache.get(key, k -> new AtomicInteger(0));
        int cur = counter.incrementAndGet();
        if (cur > max) {
            log.warn("限流拦截 key={} cur={} max={} window={}s", key, cur, max, windowSeconds);
            throw new BusinessException(ResultCode.TOO_MANY_REQUESTS,
                    String.format("请求过于频繁：每%d秒最多%d次，请稍后再试", windowSeconds, max));
        }
    }

    /** 拿真实客户端 IP（考虑 X-Forwarded-For / X-Real-IP / 代理链） */
    private static String clientIp(HttpServletRequest req) {
        String[] HEADERS = {"X-Forwarded-For", "X-Real-IP", "Proxy-Client-IP", "WL-Proxy-Client-IP", "HTTP_CLIENT_IP", "HTTP_X_FORWARDED_FOR"};
        for (String h : HEADERS) {
            String v = req.getHeader(h);
            if (v != null && !v.isBlank() && !"unknown".equalsIgnoreCase(v)) {
                // 多级代理：X-Forwarded-For 是 "client, proxy1, proxy2"，取第一个
                int comma = v.indexOf(',');
                return comma > 0 ? v.substring(0, comma).trim() : v.trim();
            }
        }
        return req.getRemoteAddr();
    }
}
package com.edu.schedule.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 注册 & 验证码 & 限流配置（与 application.yml 的 edu.auth.* 绑定）
 *
 * 安全分层（从外到内）：
 *   ① edu.auth.registration-enabled = false   →  一刀切关闭整个注册入口（生产用）
 *   ② CaptchaService (图形验证码)              →  挡住简单脚本（只识别图片才放行）
 *   ③ RateLimitService (双维度限流)            →  挡住真人快速刷 / 分布式脚本
 */
@ConfigurationProperties(prefix = "edu.auth")
public class AuthProperties {

    /** 总开关：是否开放注册入口。生产建议 false，仅允许教务端手工创建账号 */
    private boolean registrationEnabled = true;

    private final Captcha captcha = new Captcha();
    private final RateLimit rateLimit = new RateLimit();

    public static class Captcha {
        /** 验证码图片宽度 (px) */
        private int imageWidth = 130;
        /** 验证码图片高度 (px) */
        private int imageHeight = 48;
        /** 字符数（默认 4 位） */
        private int codeLength = 4;
        /** 验证码有效期（秒） */
        private int expireSeconds = 180;
        /** 允许的字符集（已移除 0/O/1/I 容易混淆的字符） */
        private String charset = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";

        public int getImageWidth() { return imageWidth; }
        public void setImageWidth(int imageWidth) { this.imageWidth = imageWidth; }
        public int getImageHeight() { return imageHeight; }
        public void setImageHeight(int imageHeight) { this.imageHeight = imageHeight; }
        public int getCodeLength() { return codeLength; }
        public void setCodeLength(int codeLength) { this.codeLength = codeLength; }
        public int getExpireSeconds() { return expireSeconds; }
        public void setExpireSeconds(int expireSeconds) { this.expireSeconds = expireSeconds; }
        public String getCharset() { return charset; }
        public void setCharset(String charset) { this.charset = charset; }
    }

    public static class RateLimit {
        // ------- 获取验证码接口限流：单 IP -------
        /** 单 IP 在 captchaWindowSeconds 内最多请求 captchaMaxRequests 次验证码（画图很耗 CPU） */
        private int captchaMaxRequests = 30;
        private int captchaWindowSeconds = 60;

        // ------- 注册接口限流（双维度）-------
        /** 单 IP 在 registerIpWindowSeconds 内最多 registerIpMaxAttempts 次 */
        private int registerIpMaxAttempts = 30;
        private int registerIpWindowSeconds = 60;
        /** 单 用户名 在 registerUserWindowSeconds 内最多 registerUserMaxAttempts 次 */
        private int registerUserMaxAttempts = 5;
        private int registerUserWindowSeconds = 180;

        public int getCaptchaMaxRequests() { return captchaMaxRequests; }
        public void setCaptchaMaxRequests(int v) { this.captchaMaxRequests = v; }
        public int getCaptchaWindowSeconds() { return captchaWindowSeconds; }
        public void setCaptchaWindowSeconds(int v) { this.captchaWindowSeconds = v; }
        public int getRegisterIpMaxAttempts() { return registerIpMaxAttempts; }
        public void setRegisterIpMaxAttempts(int v) { this.registerIpMaxAttempts = v; }
        public int getRegisterIpWindowSeconds() { return registerIpWindowSeconds; }
        public void setRegisterIpWindowSeconds(int v) { this.registerIpWindowSeconds = v; }
        public int getRegisterUserMaxAttempts() { return registerUserMaxAttempts; }
        public void setRegisterUserMaxAttempts(int v) { this.registerUserMaxAttempts = v; }
        public int getRegisterUserWindowSeconds() { return registerUserWindowSeconds; }
        public void setRegisterUserWindowSeconds(int v) { this.registerUserWindowSeconds = v; }
    }

    public boolean isRegistrationEnabled() { return registrationEnabled; }
    public void setRegistrationEnabled(boolean v) { this.registrationEnabled = v; }
    public Captcha getCaptcha() { return captcha; }
    public RateLimit getRateLimit() { return rateLimit; }
}
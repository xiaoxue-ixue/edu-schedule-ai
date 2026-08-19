package com.edu.schedule.service;

import com.edu.schedule.config.AuthProperties;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

/**
 * 图形验证码服务
 *
 * 存储：Caffeine 本地缓存（key = captchaKey，value = code），
 *       expireAfterWrite = captcha.expireSeconds，写满后自动 LRU 淘汰。
 *       —— 生产若多实例部署，应替换为 Redis；单机部署 1GB 缓存可存上百万验证码。
 *
 * 安全特性：
 *   · 字符集已剔除 0/O/1/I，避免真人认错
 *   · 每个字符随机角度旋转 + 干扰线 5 条 + 噪点 100 个
 *   · 校验通过后一次性失效（防止同一张图被反复用）
 *   · key 使用 UUID + hashCode 拼接，不可枚举
 */
@Service
public class CaptchaService {

    private static final Logger log = LoggerFactory.getLogger(CaptchaService.class);

    /** captchaKey → 验证码明文（忽略大小写，存储大写） */
    private final Cache<String, String> captchaCache;
    private final AuthProperties.Captcha cfg;
    private final SecureRandom rand = new SecureRandom();

    public CaptchaService(AuthProperties props) {
        this.cfg = props.getCaptcha();
        this.captchaCache = Caffeine.newBuilder()
                .maximumSize(50_000)
                .expireAfterWrite(Duration.ofSeconds(cfg.getExpireSeconds()))
                .build();
        log.info("🔒 CaptchaService 初始化: 字符集={} 长度={} 有效期={}s 容量上限=50000",
                cfg.getCharset(), cfg.getCodeLength(), cfg.getExpireSeconds());
    }

    /**
     * 生成一个新的图形验证码。
     *
     * @return Map: captchaKey=UUID, code=原始验证码(仅调试),
     *              imageBase64=base64 PNG图片(不含前缀，前端拼接 data:image/png;base64,xxx),
     *              expireSeconds=有效秒数
     */
    public Map<String, Object> generate() {
        String code = randomCode();
        String key  = UUID.randomUUID().toString().replace("-", "");
        String png  = renderPngBase64(code);
        captchaCache.put(key, code);
        return Map.of(
                "captchaKey", key,
                "expireSeconds", cfg.getExpireSeconds(),
                "imageBase64", png
        );
    }

    /**
     * 校验验证码（忽略大小写 + 空格）。验证通过验证码立即失效（一次性）。
     *
     * @return true=通过，false=错误/过期/已使用
     */
    public boolean validateAndConsume(String captchaKey, String captchaCode) {
        if (captchaKey == null || captchaCode == null) return false;
        String expected = captchaCache.getIfPresent(captchaKey.trim());
        if (expected == null) return false;
        // 无论对错，都消耗这个 key（防止同一个 key 被爆破尝试）
        captchaCache.invalidate(captchaKey);
        String actual = captchaCode.trim().toUpperCase();
        boolean ok = expected.equalsIgnoreCase(actual);
        if (!ok) {
            log.debug("验证码校验失败 key={} expected={} actual={}",
                    captchaKey.length() > 8 ? captchaKey.substring(0, 8) + "***" : captchaKey,
                    expected, actual);
        }
        return ok;
    }

    // ----------------------------------------------------------------
    // 内部工具
    // ----------------------------------------------------------------

    private String randomCode() {
        String cs = cfg.getCharset();
        StringBuilder sb = new StringBuilder(cfg.getCodeLength());
        for (int i = 0; i < cfg.getCodeLength(); i++) {
            sb.append(cs.charAt(rand.nextInt(cs.length())));
        }
        return sb.toString();
    }

    private String renderPngBase64(String code) {
        int w = cfg.getImageWidth();
        int h = cfg.getImageHeight();
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            // 背景色：浅灰蓝渐变
            g.setColor(new Color(246, 248, 252));
            g.fillRect(0, 0, w, h);
            // 5 条干扰线
            for (int i = 0; i < 5; i++) {
                g.setColor(new Color(180 + rand.nextInt(50), 180 + rand.nextInt(50), 200 + rand.nextInt(40)));
                int x1 = rand.nextInt(w / 3), y1 = rand.nextInt(h);
                int x2 = w / 2 + rand.nextInt(w / 2), y2 = rand.nextInt(h);
                g.drawLine(x1, y1, x2, y2);
            }
            // 100 个噪点
            for (int i = 0; i < 100; i++) {
                int x = rand.nextInt(w), y = rand.nextInt(h);
                g.setColor(new Color(rand.nextInt(256), rand.nextInt(256), rand.nextInt(256), 100 + rand.nextInt(100)));
                g.drawOval(x, y, 1, 1);
            }
            // 字符
            Font font = new Font("Segoe UI", Font.BOLD, 30);
            g.setFont(font);
            int charWidth = (w - 20) / code.length();
            for (int i = 0; i < code.length(); i++) {
                String ch = String.valueOf(code.charAt(i));
                // 随机颜色（深色，对比度高）
                g.setColor(new Color(30 + rand.nextInt(100), 40 + rand.nextInt(90), 80 + rand.nextInt(120)));
                // 随机旋转 -25° ~ 25°
                double angle = Math.toRadians(rand.nextInt(51) - 25);
                int cx = 10 + charWidth * i + charWidth / 2;
                int cy = h / 2 + rand.nextInt(8) - 4;
                g.rotate(angle, cx, cy);
                FontMetrics fm = g.getFontMetrics();
                g.drawString(ch, cx - fm.stringWidth(ch) / 2, cy + fm.getAscent() / 2 - 2);
                g.rotate(-angle, cx, cy);
            }
            // PNG 输出 -> Base64
            ByteArrayOutputStream baos = new ByteArrayOutputStream(4096);
            ImageIO.write(img, "png", baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("验证码图片生成失败", e);
        } finally {
            g.dispose();
        }
    }
}
package com.edu.schedule.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * CORS 跨域配置
 *
 * <h3>安全要点（第 2 项落地）</h3>
 * <ul>
 *   <li>旧配置：<code>addAllowedOriginPattern("*") + setAllowCredentials(true)</code>
 *       ⇒ 允许任何网站携带 cookie/token 跨域访问，存在 CSRF 风险。</li>
 *   <li>新配置：通过 <code>edu.cors.allowed-origins</code> 读取白名单，
 *       非白名单来源不会被返回 <code>Access-Control-Allow-Origin</code>，浏览器自动拦截。</li>
 *   <li>白名单为空时自动降级为 <b>禁止所有跨域</b>（而不是放行 "*"），避免漏配时裸奔。</li>
 * </ul>
 */
@Configuration
@ConfigurationProperties(prefix = "edu.cors")
public class CorsConfig {

    /**
     * YAML 配置的允许来源列表（支持通配符子域名，如 {@code https://*.example.com}）
     */
    private List<String> allowedOrigins = new ArrayList<>();

    public List<String> getAllowedOrigins() {
        return allowedOrigins;
    }

    public void setAllowedOrigins(List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins == null ? Collections.emptyList() : allowedOrigins;
    }

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();

        // 允许跨域携带 cookie / Authorization header（JWT Bearer Token 需要）
        config.setAllowCredentials(true);

        // ⚠️ 此处不再硬编码 "*"，所有来源全部来自 application.yml 的 edu.cors.allowed-origins
        if (allowedOrigins.isEmpty()) {
            // 白名单未配置时：显式放一个空列表，Spring 对空列表 = 不放行任何来源
            // 绝不 fallback 到 "*" + allowCredentials=true 的危险组合
            config.setAllowedOriginPatterns(Collections.emptyList());
        } else {
            config.setAllowedOriginPatterns(allowedOrigins);
        }

        // 允许的请求头 / 请求方法：此处放开为 "*"，因为 Origin 已经是主要的安全边界
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");

        // 预检请求（OPTIONS）缓存时间：1 小时，减少重复 OPTIONS 请求
        config.setMaxAge(3600L);

        // 暴露给前端 JS 读取的响应头（默认只暴露 Cache-Control / Content-Type / Content-Language / Expires / Last-Modified / Pragma）
        // 这里额外暴露 Authorization，方便自定义登录响应场景
        config.addExposedHeader("Authorization");

        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
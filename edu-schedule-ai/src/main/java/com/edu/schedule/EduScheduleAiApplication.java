package com.edu.schedule;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;

import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * 应用启动类。
 * 2026-08-15 新增: @EnableAsync —— Agent 排课改为后台异步执行，前端立刻返回「运行中」，
 *           防止排课时长超过 HTTP 超时（300s/5min）导致前端报错、后端仍在跑的「脱钩」问题。
 */
@SpringBootApplication(exclude = {
        org.springframework.ai.model.openai.autoconfigure.OpenAiChatAutoConfiguration.class,
        org.springframework.ai.model.openai.autoconfigure.OpenAiEmbeddingAutoConfiguration.class,
        org.springframework.ai.model.openai.autoconfigure.OpenAiImageAutoConfiguration.class,
        org.springframework.ai.model.openai.autoconfigure.OpenAiAudioSpeechAutoConfiguration.class,
        org.springframework.ai.model.openai.autoconfigure.OpenAiAudioTranscriptionAutoConfiguration.class,
        org.springframework.ai.model.openai.autoconfigure.OpenAiModerationAutoConfiguration.class
})
@MapperScan("com.edu.schedule.mapper")
@EnableAsync
// 自动扫描 com.edu.schedule 包下所有 @ConfigurationProperties（AsyncProperties / AuthProperties / CorsConfig等）
@ConfigurationPropertiesScan("com.edu.schedule")
public class EduScheduleAiApplication {

    private static final int[] COMMON_PROXY_PORTS = {7890, 7897, 7895, 10809, 1080, 20171, 10800, 8888};

    public static void main(String[] args) {
        autoConfigureProxy();
        SpringApplication.run(EduScheduleAiApplication.class, args);
        System.out.println("\n" +
                "========================================\n" +
                "  edu-schedule-ai 启动成功\n" +
                "  AI Agent 智能教务排课与教学质量分析系统\n" +
                "  接口地址: http://localhost:8080/api\n" +
                "  已启用异步模式: @EnableAsync（Agent 排课后台运行）\n" +
                "========================================\n");
    }

    /**
     * 启动前代理配置策略（2026-08-18 修订）：
     *
     * ✅ 默认禁用系统代理。原因：
     *   - 项目主要对接 SiliconFlow（api.siliconflow.cn），该 API 在国内可直连，无需代理；
     *   - 若强制走本地代理（Clash/V2Ray），当代理工具未启动或拒绝转发时，
     *     JDK HttpClient 会在 TCP 握手阶段被 RST，报 ClosedChannelException / ConnectException: null；
     *   - 之前「自动检测端口并应用 http.proxyHost」的逻辑会把这个错误放大到所有 AI 调用。
     *
     * 🔧 如需访问需要梯子的 API（如 OpenAI 官方），请改用启动参数：
     *      -Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=7890
     *      -Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=7890
     *    （不要用 java.net.useSystemProxies=true，它会接管所有 HTTP 客户端且不可控）
     */
    private static void autoConfigureProxy() {
        // 1. 显式禁用系统代理接管（最关键的一行）
        System.setProperty("java.net.useSystemProxies", "false");

        // 2. 清掉可能残留的 proxyHost 设置（防止上次启动的属性残留到本次）
        System.clearProperty("http.proxyHost");
        System.clearProperty("http.proxyPort");
        System.clearProperty("https.proxyHost");
        System.clearProperty("https.proxyPort");
        System.clearProperty("http.nonProxyHosts");

        // 3. 仅做诊断打印：是否检测到本地代理端口（仅提示，不再自动应用）
        StringBuilder sb = new StringBuilder();
        sb.append("\n---------------- 🌐 代理诊断（仅提示，不会自动应用） ----------------\n");
        sb.append(" java.net.useSystemProxies = false（已禁用，SiliconFlow 国内可直连）\n");

        Integer foundPort = null;
        for (int port : COMMON_PROXY_PORTS) {
            boolean ok = isTcpPortOpen("127.0.0.1", port, 500);
            sb.append("  探测 127.0.0.1:").append(String.format("%-5d", port)).append(" → ").append(ok ? "✅ 可用" : "❌ 无响应").append("\n");
            if (ok && foundPort == null) {
                foundPort = port;
            }
        }

        if (foundPort != null) {
            sb.append(" ℹ️  检测到本地代理端口 127.0.0.1:").append(foundPort).append("（本次未自动应用）\n");
            sb.append("     如需访问需要梯子的 API，请在启动参数添加：\n");
            sb.append("       -Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=").append(foundPort).append("\n");
        } else {
            sb.append(" ℹ️  未检测到本地代理端口（SiliconFlow 无需代理，可忽略）\n");
        }
        sb.append("----------------------------------------------------------------------\n");
        System.err.println(sb);
    }

    private static boolean isTcpPortOpen(String host, int port, int timeoutMs) {
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(host, port), timeoutMs);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
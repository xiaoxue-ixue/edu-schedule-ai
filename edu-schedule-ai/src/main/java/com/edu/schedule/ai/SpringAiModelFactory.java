package com.edu.schedule.ai;

import com.edu.schedule.common.BusinessException;
import com.edu.schedule.entity.AiModelConfig;
import com.edu.schedule.mapper.AiModelConfigMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SpringAiModelFactory {

    @Autowired
    private AiModelConfigMapper configMapper;

    private final ConcurrentHashMap<String, ChatClient> cache = new ConcurrentHashMap<>();

    public ChatClient getChatClient(String modelCode) {
        if (modelCode == null || modelCode.isBlank()) {
            return getDefaultChatClient();
        }
        return cache.computeIfAbsent(modelCode, code -> {
            AiModelConfig c = configMapper.selectByCode(code);
            if (c == null) {
                throw new BusinessException("模型配置不存在: " + code);
            }
            assertEnabled(c);
            return buildClient(c);
        });
    }

    public ChatClient getDefaultChatClient() {
        return cache.computeIfAbsent("__default__", k -> {
            AiModelConfig c = configMapper.selectDefault();
            if (c == null) {
                c = configMapper.selectAllEnabled().stream().findFirst()
                        .orElseThrow(() -> new BusinessException("没有可用的 AI 模型配置，请先在【AI模型配置】中启用一项"));
            }
            return buildClient(c);
        });
    }

    public AiModelConfig getConfig(String modelCode) {
        AiModelConfig c = configMapper.selectByCode(modelCode);
        if (c == null) {
            throw new BusinessException("模型配置不存在: " + modelCode);
        }
        return c;
    }

    public AiModelConfig getDefaultConfig() {
        AiModelConfig c = configMapper.selectDefault();
        if (c == null) {
            c = configMapper.selectAllEnabled().stream().findFirst()
                    .orElseThrow(() -> new BusinessException("没有可用的 AI 模型配置"));
        }
        return c;
    }

    private ChatClient buildClient(AiModelConfig c) {
        validate(c);
        String rawUrl    = c.getBaseUrl();
        String rawKey    = c.getApiKey();
        String rawModel  = c.getModel();
        // ✅ 兼容：model 字段为空时用 modelName 兜底
        if (rawModel == null || rawModel.isBlank()) {
            rawModel = c.getModelName();
        }

        String baseUrl = cleanBaseUrlForSpringAi(rawUrl);
        String apiKey  = cleanToken(rawKey);
        String model   = clean(rawModel);

        String keyPreview = apiKey.length() > 12
                ? apiKey.substring(0, 6) + "..." + apiKey.substring(apiKey.length() - 4)
                : apiKey;

        System.err.println("\n================================= AI 模型配置诊断 ==================================");
        System.err.println(" 数据库原始 base_url: [" + rawUrl + "]");
        System.err.println(" 清理后 base_url (传给 Spring AI，不要带 /v1): [" + baseUrl + "]");
        System.err.println(" ⚠️  Spring AI 内部会自己补 /v1，所以最终 = " + baseUrl + "/v1/chat/completions");
        System.err.println(" API Key 原始长度:    " + (rawKey == null ? 0 : rawKey.length()) + " 字符");
        System.err.println(" API Key 清理后预览:  [" + keyPreview + "]   长度:" + apiKey.length());
        System.err.println(" model:               [" + model + "]");
        System.err.println("=================================================================================\n");

        probeConnectivity(baseUrl, apiKey, model);

        OpenAiApi api = OpenAiApi.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .build();

        OpenAiChatOptions.Builder ob = OpenAiChatOptions.builder().model(model);
        if (c.getTemperature() != null) {
            ob.temperature(c.getTemperature().doubleValue());
        }
        if (c.getMaxTokens() != null) {
            ob.maxTokens(c.getMaxTokens());
        }

        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .openAiApi(api)
                .defaultOptions(ob.build())
                .build();

        return ChatClient.builder(chatModel).build();
    }

    private String cleanBaseUrlForSpringAi(String url) {
        String s = url == null ? "" : url;
        s = s.replace("`", "").replace("，", ",").replaceAll(",+$", "");
        s = s.trim();
        if (s.endsWith("/")) {
            s = s.substring(0, s.length() - 1);
        }
        while (s.endsWith("/v1")) {
            s = s.substring(0, s.length() - 3);
            if (s.endsWith("/")) {
                s = s.substring(0, s.length() - 1);
            }
        }
        while (s.contains("/v1/v1")) {
            s = s.replace("/v1/v1", "/v1");
        }
        return s;
    }

    private void probeConnectivity(String baseUrl, String apiKey, String model) {
        String payload = "{\"model\":\"" + escapeJson(model) + "\",\"max_tokens\":8,\"messages\":[{\"role\":\"user\",\"content\":\"hi\"}]}";
        String urlWithoutV1 = baseUrl + "/chat/completions";
        String urlWithV1    = baseUrl + "/v1/chat/completions";

        String ok1 = doPostCheck(urlWithoutV1, apiKey, payload);
        String ok2 = doPostCheck(urlWithV1,    apiKey, payload);

        System.err.println("\n------------------------------ 🟢 连通性探测结论 ------------------------------");
        System.err.println(" 方案 A: " + urlWithoutV1 + "  → " + ok1);
        System.err.println(" 方案 B: " + urlWithV1    + "  → " + ok2);
        if (ok2.contains("200") && !ok1.contains("200")) {
            System.err.println(" ✅ 方案 B 正确。当前 cleanBaseUrlForSpringAi 策略会把 baseUrl 的 /v1 去掉 → 让 Spring AI 内部补");
        } else if (ok1.contains("200") && !ok2.contains("200")) {
            System.err.println(" ⚠️  方案 A 正确。说明这个模型供应商不需要 Spring AI 补 /v1（少见，考虑直接用非 OpenAI 兼容 SDK）");
        } else if (ok1.contains("200")) {
            System.err.println(" ✅ 两种都能通，用 B 就行");
        } else {
            System.err.println(" ❌ 两种都不通！把方案 A/B 里的错误响应体复制到 DeepSeek 文档查（大概率 Key 无效/余额不足）");
        }
        System.err.println("------------------------------------------------------------------------------\n");
    }

    private String doPostCheck(String url, String apiKey, String jsonBody) {
        try {
            WebClient.ResponseSpec spec = WebClient.builder()
                    .build()
                    .post()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .bodyValue(jsonBody)
                    .retrieve();
            String body = spec.toEntity(String.class)
                    .block(Duration.ofSeconds(15))
                    .getBody();
            String shortBody = body == null ? "" : (body.length() > 120 ? body.substring(0, 120) + "..." : body);
            return "200 OK ✅ 响应预览: " + shortBody;
        } catch (Exception e) {
            Throwable t = e;
            while (t.getCause() != null && t.getCause() != t) t = t.getCause();
            if (t instanceof org.springframework.web.reactive.function.client.WebClientResponseException wx) {
                String b;
                try {
                    b = wx.getResponseBodyAsString(java.nio.charset.StandardCharsets.UTF_8);
                } catch (Exception ignore) { b = ""; }
                String sb = b == null ? "" : (b.length() > 160 ? b.substring(0, 160) + "..." : b);
                return "HTTP " + wx.getStatusCode().value() + " ❌ 响应体: " + sb;
            }
            return t.getClass().getSimpleName() + ": " + (t.getMessage() == null ? "" : t.getMessage());
        }
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String cleanToken(String s) {
        return clean(s);
    }
    private String clean(String s) {
        if (s == null) return "";
        return s.replace("`", "").trim();
    }

    private void validate(AiModelConfig c) {
        if (c.getBaseUrl() == null || c.getBaseUrl().isBlank()) {
            throw new BusinessException("模型 baseUrl 为空");
        }
        if (c.getApiKey() == null || c.getApiKey().isBlank()) {
            throw new BusinessException("模型 apiKey 为空，请在【AI模型配置】填入真实 Key");
        }
        // ✅ 兼容：model 字段为空时用 modelName 兜底
        String modelValue = c.getModel();
        if (modelValue == null || modelValue.isBlank()) {
            modelValue = c.getModelName();
        }
        if (modelValue == null || modelValue.isBlank()) {
            throw new BusinessException("模型 model 为空，请在【AI模型配置】填写模型名称或模型标识");
        }
    }

    private void assertEnabled(AiModelConfig c) {
        if (c.getEnabled() == null || c.getEnabled() != 1) {
            throw new BusinessException("模型配置已禁用: " + c.getModelCode());
        }
    }

    public void evict(String modelCode) {
        if (modelCode == null || modelCode.isBlank()) {
            cache.clear();
        } else {
            cache.remove(modelCode);
            cache.remove("__default__");
        }
    }

    public String testConnection(Long configId) {
        AiModelConfig c = configMapper.selectById(configId);
        if (c == null) {
            throw new BusinessException("模型配置不存在");
        }
        validate(c);
        try {
            ChatClient client = buildClient(c);
            return client.prompt().user("请只回复两个字：pong").call().content();
        } catch (Exception e) {
            throw new BusinessException("连通性测试失败: " + rootMessage(e));
        }
    }

    private String rootMessage(Throwable e) {
        Throwable t = e;
        while (t.getCause() != null && t.getCause() != t) {
            t = t.getCause();
        }
        return t.getMessage();
    }
}
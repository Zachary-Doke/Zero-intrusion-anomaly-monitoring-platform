package com.platform.analyze.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.analyze.config.DeepSeekProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DeepSeek API 客户端
 */
@Component
public class DeepSeekClient {

    private static final Logger log = LoggerFactory.getLogger(DeepSeekClient.class);

    private final WebClient webClient;
    private final DeepSeekProperties properties;
    private final ObjectMapper objectMapper;

    public DeepSeekClient(WebClient.Builder webClientBuilder,
                          DeepSeekProperties properties,
                          ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.webClient = webClientBuilder
                .baseUrl(properties.getBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * 调用 Chat Completion 接口
     *
     * @param prompt 用户提示词
     * @return 结构化的 JSON 字符串
     */
    public String chatCompletion(String prompt) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", properties.getModel());
            
            // 系统提示词强化约束
            String systemPrompt = "你是一个资深的Java开发专家，擅长排查和解决异常问题。" +
                    "请务必严格按照指定的JSON结构输出分析结果。不要编造信息，证据不足时允许输出不确定。";
            
            requestBody.put("messages", List.of(
                    Map.of("role", "system", "content", systemPrompt),
                    Map.of("role", "user", "content", prompt)
            ));
            
            // 启用 JSON 模式（要求模型支持且包含相关提示）
            requestBody.put("response_format", Map.of("type", "json_object"));

            Map response = webClient.post()
                    .uri("/chat/completions")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofMillis(properties.getTimeout()))
                    .block();

            if (response != null && response.containsKey("choices")) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
                if (!choices.isEmpty()) {
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    return (String) message.get("content");
                }
            }
            log.warn("DeepSeek 响应为空或格式异常: {}", response);
            return null;
        } catch (Exception e) {
            log.error("调用 DeepSeek API 失败: {}", e.getMessage(), e);
            throw new RuntimeException("AI 分析调用失败，请稍后重试");
        }
    }

    public String currentModelName() {
        return properties.getModel();
    }
}

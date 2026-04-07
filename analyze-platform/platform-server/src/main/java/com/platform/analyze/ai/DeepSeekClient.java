package com.platform.analyze.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class DeepSeekClient {

    private static final Logger log = LoggerFactory.getLogger(DeepSeekClient.class);

    private final WebClient.Builder webClientBuilder;

    public DeepSeekClient(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }

    public String chatCompletion(String prompt,
                                 String apiKey,
                                 String baseUrl,
                                 String model,
                                 long timeoutMs) {
        if (!StringUtils.hasText(apiKey) || !StringUtils.hasText(baseUrl) || !StringUtils.hasText(model)) {
            throw new RuntimeException("AI 接口配置不完整");
        }

        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", List.of(
                    Map.of("role", "system", "content",
                            "你是一个资深的 Java 异常排查专家。请严格输出 JSON，禁止附加 markdown。"),
                    Map.of("role", "user", "content", prompt)
            ));
            requestBody.put("response_format", Map.of("type", "json_object"));

            WebClient webClient = webClientBuilder
                    .baseUrl(baseUrl)
                    .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .build();

            Map response = webClient.post()
                    .uri("/chat/completions")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofMillis(timeoutMs))
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
}

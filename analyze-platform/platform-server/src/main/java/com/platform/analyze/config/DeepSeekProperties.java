package com.platform.analyze.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * DeepSeek AI 配置属性
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "ai.deepseek")
public class DeepSeekProperties {
    /**
     * API 密钥
     */
    private String apiKey;
    
    /**
     * 基础 URL
     */
    private String baseUrl = "https://api.deepseek.com/v1";
    
    /**
     * 使用的模型
     */
    private String model = "deepseek-chat";
    
    /**
     * 调用超时时间（毫秒）
     */
    private long timeout = 30000;
}

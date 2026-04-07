package com.platform.analyze.dto;

import lombok.Data;

@Data
public class RuleSettingsDto {

    private String packagePatterns;
    private Boolean deepSamplingEnabled;
    private Integer depthLimit;
    private Integer lengthLimit;
    private Integer collectionLimit;
    private Double defaultSampleRate;
    private Integer queueCapacity;
    private Integer flushIntervalMs;
    private String aiBaseUrl;
    private String aiApiKey;
    private Boolean aiApiKeyConfigured;
    private String aiModel;
    private String aiPromptTemplate;
    private String traceKeys;
    private String sensitiveFields;
    private Long version;
}

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
    private Integer thresholdCount;
    private Integer thresholdWindowMinutes;
    private String alertRecipients;
    private String aiModel;
    private String traceKeys;
    private String sensitiveFields;
    private Long version;
}

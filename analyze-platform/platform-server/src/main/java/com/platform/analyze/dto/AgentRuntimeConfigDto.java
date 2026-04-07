package com.platform.analyze.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AgentRuntimeConfigDto {

    private String serviceName;
    private String appName;
    private String packagePatterns;
    private Integer depthLimit;
    private Integer lengthLimit;
    private Integer collectionLimit;
    private Double defaultSampleRate;
    private Integer queueCapacity;
    private Integer flushIntervalMs;
    private String traceKeys;
    private String sensitiveFields;
    private Boolean deepSamplingEnabled;
    private String aiModel;
    private Long version;
}

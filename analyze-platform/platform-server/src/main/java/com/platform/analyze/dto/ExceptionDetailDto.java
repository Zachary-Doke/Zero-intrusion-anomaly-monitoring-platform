package com.platform.analyze.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ExceptionDetailDto {

    private Long id;
    private String fingerprint;
    private String summary;
    private String severity;
    private String status;
    private String appName;
    private String serviceName;
    private String environment;
    private String host;
    private String pid;
    private String threadName;
    private String className;
    private String methodName;
    private String methodSignature;
    private String exceptionClass;
    private String message;
    private String stackTrace;
    private String topStackFrame;
    private String argumentsSnapshot;
    private String thisSnapshot;
    private String traceId;
    private String traceContext;
    private Integer queueSize;
    private Long droppedCount;
    private String agentVersion;
    private Long configVersion;
    private LocalDateTime lastConfigSyncAt;
    private String lastConfigSyncStatus;
    private String lastConfigSyncError;
    private ExceptionSuggestionDto suggestion;
    private LocalDateTime occurrenceTime;
}

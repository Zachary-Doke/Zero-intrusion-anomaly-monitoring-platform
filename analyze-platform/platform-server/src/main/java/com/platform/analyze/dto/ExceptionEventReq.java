package com.platform.analyze.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
public class ExceptionEventReq {

    @NotNull(message = "事件时间戳不能为空")
    private Long timestamp;

    @NotBlank(message = "应用名称不能为空")
    private String appName;

    private String serviceName;

    private String environment;

    private String host;

    private String pid;

    private String threadName;

    @NotBlank(message = "类名不能为空")
    private String className;

    @NotBlank(message = "方法名不能为空")
    private String methodName;

    @NotBlank(message = "方法签名不能为空")
    private String methodSignature;

    private Object argumentsSnapshot;

    private Object thisSnapshot;

    @NotBlank(message = "异常类名不能为空")
    private String exceptionClass;

    private String severity;

    private String message;

    @NotBlank(message = "异常堆栈不能为空")
    private String stackTrace;

    @NotBlank(message = "栈顶方法不能为空")
    private String topStackFrame;

    private String traceId;

    private Map<String, String> traceContext;

    private String fingerprint;

    private Integer queueSize;

    private Long droppedCount;

    private String agentVersion;

    private Long configVersion;

    private LocalDateTime lastConfigSyncAt;

    private String lastConfigSyncStatus;

    private String lastConfigSyncError;
}

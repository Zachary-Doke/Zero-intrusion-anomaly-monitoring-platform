package com.platform.analyze.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ExceptionListItemDto {

    private Long id;
    private String fingerprint;
    private String summary;
    private String severity;
    private String status;
    private String alertStatus;
    private String appName;
    private String serviceName;
    private String exceptionClass;
    private String methodName;
    private String topStackFrame;
    private LocalDateTime occurrenceTime;
    private String traceId;
}

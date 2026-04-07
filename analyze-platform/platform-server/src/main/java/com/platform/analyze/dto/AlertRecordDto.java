package com.platform.analyze.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class AlertRecordDto {

    private Long id;
    private String fingerprint;
    private String serviceName;
    private String severity;
    private String summary;
    private String alertStatus;
    private Long alertCount;
    private String channel;
    private String sendStatus;
    private String recipients;
    private String content;
    private String reportStatus;
    private String reportTriggerSource;
    private LocalDateTime triggeredAt;
    private LocalDateTime reportRequestedAt;
    private LocalDateTime reportCompletedAt;
}

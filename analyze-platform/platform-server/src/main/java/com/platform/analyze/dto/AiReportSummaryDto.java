package com.platform.analyze.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class AiReportSummaryDto {

    private String fingerprint;
    private String title;
    private String reportStatus;
    private String modelName;
    private String triggerSource;
    private String summary;
    private String fixSuggestion;
    private String errorMessage;
    private LocalDateTime requestedAt;
    private LocalDateTime startedAt;
    private LocalDateTime analysisTime;
}

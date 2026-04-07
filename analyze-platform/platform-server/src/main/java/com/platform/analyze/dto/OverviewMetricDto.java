package com.platform.analyze.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class OverviewMetricDto {

    private long totalExceptions;
    private long fingerprintCount;
    private long openExceptionCount;
    private long criticalExceptionCount;
    private long serviceCount;
    private long triggeredAlertCount;
    private long aiReportCount;
    private long averageAnalysisMinutes;
    private long agentCount;
    private long effectiveAgentCount;
}

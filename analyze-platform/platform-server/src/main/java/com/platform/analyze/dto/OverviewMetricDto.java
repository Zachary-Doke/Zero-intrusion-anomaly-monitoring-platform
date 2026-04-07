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
    private long effectiveAgentCount;
}

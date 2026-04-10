package com.platform.analyze.dto;

import com.platform.analyze.entity.ExceptionFingerprint;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ExceptionOverviewDto {

    private OverviewMetricDto metrics;
    private List<ExceptionTrendDto> trends;
    private List<ExceptionListItemDto> recentEvents;
    private List<ExceptionFingerprint> topFingerprints;
    private RiskSummaryDto riskSummary;
}

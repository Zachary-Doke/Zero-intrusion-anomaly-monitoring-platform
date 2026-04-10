package com.platform.analyze.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RiskSummaryDto {

    private String riskLevel;
    private String summary;
    private List<String> highlights;
    private String source;
    private LocalDateTime updatedAt;
}

package com.platform.analyze.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExceptionSuggestionDto {

    private String rootCauseAnalysis;
    private String impactScope;
    private List<String> troubleshootingSteps;
    private String fixSuggestion;
    private String suggestionStatus;
    private LocalDateTime suggestionUpdatedAt;
}

package com.platform.analyze.dto;

import lombok.Data;
import java.util.List;

/**
 * AI 分析结果 DTO
 */
@Data
public class AiAnalysisResultDto {
    /**
     * 可能的根本原因
     */
    private String probableRootCause;
    
    /**
     * 影响范围评估
     */
    private String impactScope;
    
    /**
     * 排查步骤
     */
    private List<String> troubleshootingSteps;
    
    /**
     * 修复建议
     */
    private String fixSuggestion;
}

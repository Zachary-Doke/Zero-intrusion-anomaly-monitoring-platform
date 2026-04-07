package com.platform.analyze.controller;

import com.platform.analyze.ai.AiAnalysisService;
import com.platform.analyze.common.Result;
import com.platform.analyze.dto.AiAnalysisResultDto;
import com.platform.analyze.dto.AiReportSummaryDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "AI Analysis API", description = "AI 异常分析接口")
@RestController
@RequestMapping("/api/ai")
public class AiAnalysisController {

    private final AiAnalysisService aiService;

    public AiAnalysisController(AiAnalysisService aiService) {
        this.aiService = aiService;
    }

    @Operation(summary = "调用 AI 进行异常分析")
    @PostMapping("/analyze/{fingerprint}")
    public Result<AiAnalysisResultDto> analyzeFingerprint(@PathVariable String fingerprint) {
        return Result.success(aiService.analyze(fingerprint));
    }

    @Operation(summary = "查询 AI 报告列表")
    @GetMapping("/reports")
    public Result<List<AiReportSummaryDto>> getReports(@RequestParam(required = false) String keyword,
                                                       @RequestParam(required = false) String status) {
        return Result.success(aiService.getReports(keyword, status));
    }
}

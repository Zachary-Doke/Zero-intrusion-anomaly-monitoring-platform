package com.platform.analyze.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.analyze.ai.AiAnalysisService;
import com.platform.analyze.ai.DeepSeekClient;
import com.platform.analyze.config.DeepSeekProperties;
import com.platform.analyze.dto.ExceptionListItemDto;
import com.platform.analyze.dto.ExceptionTrendDto;
import com.platform.analyze.dto.OverviewMetricDto;
import com.platform.analyze.dto.RiskSummaryDto;
import com.platform.analyze.entity.ExceptionFingerprint;
import com.platform.analyze.entity.RuleSettings;
import com.platform.analyze.repository.AiAnalysisResultRepository;
import com.platform.analyze.repository.ExceptionFingerprintRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiAnalysisServiceRiskSummaryTest {

    @Mock
    private DeepSeekClient deepSeekClient;

    @Mock
    private RuleSettingsService ruleSettingsService;

    @Mock
    private AiAnalysisResultRepository aiAnalysisResultRepository;

    @Mock
    private ExceptionFingerprintRepository fingerprintRepository;

    private AiAnalysisService aiAnalysisService;

    @BeforeEach
    void setUp() {
        DeepSeekProperties properties = new DeepSeekProperties();
        properties.setTimeout(3000L);
        aiAnalysisService = new AiAnalysisService(
                deepSeekClient,
                properties,
                ruleSettingsService,
                aiAnalysisResultRepository,
                fingerprintRepository,
                new ObjectMapper()
        );
    }

    @Test
    void shouldKeepRuleRiskLevelWhenAiReturnsDifferentRiskLevel() {
        RuleSettings settings = new RuleSettings();
        settings.setAiApiKey("test-key");
        settings.setAiBaseUrl("https://api.example.com");
        settings.setAiModel("deepseek-chat");
        when(ruleSettingsService.currentSettings()).thenReturn(settings);
        when(deepSeekClient.chatCompletion(anyString(), anyString(), anyString(), anyString(), anyLong()))
                .thenReturn("{\"riskLevel\":\"LOW\",\"summary\":\"风险概览\",\"highlights\":[\"高频接口超时\",\"错误量上升\"]}");

        OverviewMetricDto metrics = new OverviewMetricDto(10, 1, 8, 0, 1, 1);
        List<ExceptionTrendDto> trends = List.of(
                new ExceptionTrendDto(LocalDate.now().minusDays(1).toString(), 3L),
                new ExceptionTrendDto(LocalDate.now().toString(), 5L)
        );
        ExceptionFingerprint topFingerprint = new ExceptionFingerprint();
        topFingerprint.setServiceName("payment-gateway");
        topFingerprint.setSummary("provider timeout");
        topFingerprint.setExceptionClass("java.lang.IllegalStateException");
        topFingerprint.setOccurrenceCount(9L);
        List<ExceptionListItemDto> recentEvents = List.of(
                new ExceptionListItemDto(
                        1L,
                        "fp-1",
                        "provider timeout",
                        "CRITICAL",
                        "OPEN",
                        "demo-app",
                        "payment-gateway",
                        "java.lang.IllegalStateException",
                        "handle",
                        "com.demo.Handler.handle(Handler.java:11)",
                        LocalDateTime.now(),
                        "trace-1"
                )
        );

        RiskSummaryDto summary = aiAnalysisService.generateDailyRiskSummary(
                metrics,
                trends,
                List.of(topFingerprint),
                recentEvents
        );

        assertThat(summary.getRiskLevel()).isEqualTo("HIGH");
        assertThat(summary.getSource()).isEqualTo("AI");
    }
}

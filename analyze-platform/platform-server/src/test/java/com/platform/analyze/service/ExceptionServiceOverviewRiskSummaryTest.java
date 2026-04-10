package com.platform.analyze.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.analyze.ai.AiAnalysisService;
import com.platform.analyze.dto.ExceptionOverviewDto;
import com.platform.analyze.dto.RiskSummaryDto;
import com.platform.analyze.entity.ExceptionEvent;
import com.platform.analyze.entity.ExceptionFingerprint;
import com.platform.analyze.entity.RuleSettings;
import com.platform.analyze.repository.ExceptionEventRepository;
import com.platform.analyze.repository.ExceptionFingerprintRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExceptionServiceOverviewRiskSummaryTest {

    @Mock
    private ExceptionEventRepository eventRepository;

    @Mock
    private ExceptionFingerprintRepository fingerprintRepository;

    @Mock
    private FingerprintGenerator fingerprintGenerator;

    @Mock
    private SensitiveDataSanitizer sensitiveDataSanitizer;

    @Mock
    private AgentSyncStatusService agentSyncStatusService;

    @Mock
    private RuleSettingsService ruleSettingsService;

    @Mock
    private AiAnalysisService aiAnalysisService;

    private ExceptionService exceptionService;

    @BeforeEach
    void setUp() {
        exceptionService = new ExceptionService(
                eventRepository,
                fingerprintRepository,
                fingerprintGenerator,
                new ObjectMapper(),
                sensitiveDataSanitizer,
                agentSyncStatusService,
                ruleSettingsService,
                aiAnalysisService
        );
    }

    @Test
    void shouldReturnOverviewWithoutBlockingOnLiveAiAndReuseDailySnapshot() throws Exception {
        RuleSettings settings = new RuleSettings();
        settings.setVersion(1L);
        when(ruleSettingsService.currentSettings()).thenReturn(settings);
        when(eventRepository.count()).thenReturn(5L);
        when(eventRepository.countByStatus("OPEN")).thenReturn(4L);
        when(eventRepository.countBySeverityAndStatus("CRITICAL", "OPEN")).thenReturn(1L);
        when(agentSyncStatusService.countEffective(1L)).thenReturn(1L);

        ExceptionFingerprint fingerprint = new ExceptionFingerprint();
        fingerprint.setFingerprint("fp-1");
        fingerprint.setServiceName("payment-gateway");
        fingerprint.setSummary("provider timeout");
        fingerprint.setExceptionClass("java.lang.IllegalStateException");
        fingerprint.setOccurrenceCount(5L);
        fingerprint.setLastSeen(LocalDateTime.now());
        when(fingerprintRepository.findAll()).thenReturn(List.of(fingerprint));
        when(fingerprintRepository.findTop8ByOrderByOccurrenceCountDescLastSeenDesc()).thenReturn(List.of(fingerprint));

        ExceptionEvent event = new ExceptionEvent();
        event.setId(1L);
        event.setFingerprint("fp-1");
        event.setSummary("provider timeout");
        event.setSeverity("CRITICAL");
        event.setStatus("OPEN");
        event.setAppName("demo-app");
        event.setServiceName("payment-gateway");
        event.setExceptionClass("java.lang.IllegalStateException");
        event.setMethodName("handle");
        event.setTopStackFrame("com.demo.Handler.handle(Handler.java:11)");
        event.setOccurrenceTime(LocalDateTime.now());
        event.setTraceId("trace-1");
        when(eventRepository.findAll(any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(event)));
        when(eventRepository.findByOccurrenceTimeGreaterThanEqualOrderByOccurrenceTimeAsc(any(LocalDateTime.class)))
                .thenReturn(List.of(event));

        RiskSummaryDto ruleSummary = new RiskSummaryDto(
                "MEDIUM",
                "规则摘要",
                List.of("今日异常 5 条", "未处理 4 条"),
                "RULE",
                LocalDateTime.now()
        );
        RiskSummaryDto aiSummary = new RiskSummaryDto(
                "MEDIUM",
                "AI 摘要",
                List.of("调用链存在超时", "错误集中在支付服务"),
                "AI",
                LocalDateTime.now()
        );
        CountDownLatch aiFinished = new CountDownLatch(1);
        when(aiAnalysisService.generateRuleDailyRiskSummary(any(), anyList(), anyList(), anyList()))
                .thenReturn(ruleSummary);
        when(aiAnalysisService.generateDailyRiskSummary(any(), anyList(), anyList(), anyList()))
                .thenAnswer(invocation -> {
                    Thread.sleep(400L);
                    aiFinished.countDown();
                    return aiSummary;
                });

        long start = System.nanoTime();
        ExceptionOverviewDto first = exceptionService.buildOverview();
        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

        assertThat(first.getRiskSummary()).isEqualTo(ruleSummary);
        assertThat(elapsedMs).isLessThan(300L);
        assertThat(aiFinished.await(2, TimeUnit.SECONDS)).isTrue();

        ExceptionOverviewDto second = null;
        for (int i = 0; i < 20; i++) {
            second = exceptionService.buildOverview();
            if ("AI".equals(second.getRiskSummary().getSource())) {
                break;
            }
            Thread.sleep(50L);
        }

        assertThat(second).isNotNull();
        assertThat(second.getRiskSummary().getSource()).isEqualTo("AI");
        verify(aiAnalysisService, times(1)).generateDailyRiskSummary(any(), anyList(), anyList(), anyList());
        verify(aiAnalysisService, atLeastOnce()).generateRuleDailyRiskSummary(any(), anyList(), anyList(), anyList());
    }
}

package com.platform.analyze.service;

import com.platform.analyze.dto.ExceptionDetailDto;
import com.platform.analyze.dto.ExceptionEventReq;
import com.platform.analyze.dto.ExceptionOverviewDto;
import com.platform.analyze.dto.ExceptionSuggestionDto;
import com.platform.analyze.entity.ExceptionFingerprint;
import com.platform.analyze.repository.ExceptionEventRepository;
import com.platform.analyze.repository.ExceptionFingerprintRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ExceptionServiceTest {

    @Autowired
    private ExceptionService exceptionService;

    @Autowired
    private ExceptionEventRepository eventRepository;

    @Autowired
    private ExceptionFingerprintRepository fingerprintRepository;

    @BeforeEach
    void setUp() {
        eventRepository.deleteAll();
        fingerprintRepository.deleteAll();
    }

    @Test
    void shouldMaskSensitivePayloadsAcrossEventSurfaces() {
        ExceptionEventReq req = buildEvent("payment-gateway", "HIGH", "login failed sensitive=password123");
        req.setArgumentsSnapshot(Map.of("password", "password123", "note", "sensitive=password123"));
        req.setTraceContext(Map.of("traceId", "trace-test", "password", "password123"));
        req.setStackTrace("java.lang.IllegalStateException: sensitive=password123");

        exceptionService.saveEvent(req);
        Long eventId = eventRepository.findAll().get(0).getId();
        ExceptionDetailDto detail = exceptionService.getEventDetail(eventId);

        assertThat(detail.getMessage()).doesNotContain("password123").contains("***");
        assertThat(detail.getSummary()).doesNotContain("password123").contains("***");
        assertThat(detail.getStackTrace()).doesNotContain("password123").contains("***");
        assertThat(detail.getTraceContext()).doesNotContain("password123").contains("***");
        assertThat(detail.getArgumentsSnapshot()).doesNotContain("password123").contains("***");
    }

    @Test
    void shouldMaskSensitiveValuesFoundOnlyInMessageText() {
        ExceptionEventReq req = buildEvent(
                "payment-gateway",
                "HIGH",
                "Invalid argument with data: {name=test-user, id=123, sensitive=password123}"
        );

        exceptionService.saveEvent(req);
        Long eventId = eventRepository.findAll().get(0).getId();
        ExceptionDetailDto detail = exceptionService.getEventDetail(eventId);

        assertThat(detail.getMessage()).doesNotContain("password123").contains("sensitive=***");
        assertThat(detail.getSummary()).doesNotContain("password123").contains("sensitive=***");
        assertThat(detail.getStackTrace()).doesNotContain("password123").contains("sensitive=***");
    }

    @Test
    void shouldTrackAgentSyncStateOnEventCapture() {
        for (int i = 0; i < 5; i++) {
            ExceptionEventReq req = buildEvent("payment-gateway", "CRITICAL", "provider timeout");
            req.setConfigVersion(3L);
            req.setLastConfigSyncStatus("SUCCESS");
            exceptionService.saveEvent(req);
        }

        ExceptionFingerprint fingerprint = fingerprintRepository.findAll().get(0);
        ExceptionDetailDto detail = exceptionService.getEventDetail(eventRepository.findAll().get(0).getId());

        assertThat(fingerprint.getOccurrenceCount()).isEqualTo(5);
        assertThat(detail.getConfigVersion()).isEqualTo(3L);
        assertThat(detail.getLastConfigSyncStatus()).isEqualTo("SUCCESS");
    }

    @Test
    void shouldBuildOverviewWithEffectiveAgents() {
        for (int i = 0; i < 5; i++) {
            ExceptionEventReq req = buildEvent("payment-gateway", "CRITICAL", "provider timeout");
            req.setConfigVersion(2L);
            req.setLastConfigSyncStatus("SUCCESS");
            exceptionService.saveEvent(req);
        }

        ExceptionOverviewDto overview = exceptionService.buildOverview();

        assertThat(overview.getMetrics().getTotalExceptions()).isEqualTo(5);
        assertThat(overview.getMetrics().getFingerprintCount()).isEqualTo(1);
        assertThat(overview.getMetrics().getEffectiveAgentCount()).isGreaterThanOrEqualTo(1);
        assertThat(overview.getTopFingerprints()).isNotEmpty();
    }

    @Test
    void shouldExposeRootCauseAndSuggestionFromDetailWorkflow() {
        exceptionService.saveEvent(buildEvent("checkout-api", "HIGH", "cart item missing"));
        Long id = eventRepository.findAll().get(0).getId();

        ExceptionDetailDto detail = exceptionService.getEventDetail(id);
        ExceptionSuggestionDto suggestion = exceptionService.generateSuggestion(id);
        ExceptionDetailDto refreshed = exceptionService.getEventDetail(id);

        assertThat(detail.getSuggestion()).isNotNull();
        assertThat(detail.getSuggestion().getRootCauseAnalysis()).isNotBlank();
        assertThat(detail.getSuggestion().getFixSuggestion()).isNull();
        assertThat(suggestion.getFixSuggestion()).isNotBlank();
        assertThat(refreshed.getSuggestion().getFixSuggestion()).isNotBlank();
    }

    private ExceptionEventReq buildEvent(String serviceName, String severity, String message) {
        ExceptionEventReq req = new ExceptionEventReq();
        req.setTimestamp(Instant.now().toEpochMilli());
        req.setAppName("demo-app");
        req.setServiceName(serviceName);
        req.setEnvironment("test");
        req.setHost("localhost");
        req.setPid("1");
        req.setThreadName("main");
        req.setClassName("com.demo." + serviceName + ".Handler");
        req.setMethodName("handle");
        req.setMethodSignature("public void handle()");
        req.setExceptionClass("java.lang.IllegalStateException");
        req.setSeverity(severity);
        req.setMessage(message);
        req.setStackTrace("java.lang.IllegalStateException: " + message);
        req.setTopStackFrame("com.demo." + serviceName + ".Handler.handle(Handler.java:10)");
        req.setTraceContext(Map.of("traceId", "trace-test"));
        req.setTraceId("trace-test");
        req.setAgentVersion("agent-test");
        return req;
    }
}

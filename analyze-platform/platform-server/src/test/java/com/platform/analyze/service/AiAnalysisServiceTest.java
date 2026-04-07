package com.platform.analyze.service;

import com.platform.analyze.ai.AiAnalysisService;
import com.platform.analyze.dto.AiAnalysisResultDto;
import com.platform.analyze.dto.ExceptionEventReq;
import com.platform.analyze.repository.AiAnalysisResultRepository;
import com.platform.analyze.repository.ExceptionEventRepository;
import com.platform.analyze.repository.ExceptionFingerprintRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class AiAnalysisServiceTest {

    @Autowired
    private AiAnalysisService aiAnalysisService;

    @Autowired
    private ExceptionService exceptionService;

    @Autowired
    private ExceptionEventRepository eventRepository;

    @Autowired
    private ExceptionFingerprintRepository fingerprintRepository;

    @Autowired
    private AiAnalysisResultRepository aiAnalysisResultRepository;

    @BeforeEach
    void setUp() {
        aiAnalysisResultRepository.deleteAll();
        eventRepository.deleteAll();
        fingerprintRepository.deleteAll();
    }

    @Test
    void shouldNotCreateDuplicateReportWhenAutoAndManualAnalyzeCoexist() {
        for (int i = 0; i < 5; i++) {
            exceptionService.saveEvent(buildEvent());
        }
        String fingerprint = fingerprintRepository.findAll().get(0).getFingerprint();

        aiAnalysisService.analyze(fingerprint);

        assertThat(aiAnalysisResultRepository.count()).isEqualTo(1);
        assertThat(aiAnalysisResultRepository.findByFingerprint(fingerprint)).isPresent();
    }

    @Test
    void shouldFallbackToHeuristicAnalysisWhenRemoteAiFails() {
        exceptionService.saveEvent(buildEvent());
        String fingerprint = fingerprintRepository.findAll().get(0).getFingerprint();

        AiAnalysisResultDto result = aiAnalysisService.analyze(fingerprint);

        assertThat(result.getProbableRootCause()).isNotBlank();
        assertThat(aiAnalysisResultRepository.findByFingerprint(fingerprint).orElseThrow().getModelName())
                .isEqualTo("heuristic-local");
    }

    private ExceptionEventReq buildEvent() {
        ExceptionEventReq req = new ExceptionEventReq();
        req.setTimestamp(Instant.now().toEpochMilli());
        req.setAppName("demo-app");
        req.setServiceName("payment-gateway");
        req.setEnvironment("test");
        req.setClassName("com.demo.Handler");
        req.setMethodName("handle");
        req.setMethodSignature("public void handle()");
        req.setExceptionClass("java.lang.IllegalStateException");
        req.setSeverity("HIGH");
        req.setMessage("provider timeout");
        req.setStackTrace("java.lang.IllegalStateException: provider timeout");
        req.setTopStackFrame("com.demo.Handler.handle(Handler.java:11)");
        return req;
    }
}

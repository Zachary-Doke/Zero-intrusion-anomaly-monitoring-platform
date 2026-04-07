package com.platform.analyze.controller;

import com.platform.analyze.auth.AuthenticatedUser;
import com.platform.analyze.auth.TokenService;
import com.platform.analyze.auth.UserRole;
import com.platform.analyze.dto.ExceptionEventReq;
import com.platform.analyze.repository.ExceptionEventRepository;
import com.platform.analyze.repository.ExceptionFingerprintRepository;
import com.platform.analyze.service.ExceptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AlertControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ExceptionService exceptionService;

    @Autowired
    private ExceptionEventRepository eventRepository;

    @Autowired
    private ExceptionFingerprintRepository fingerprintRepository;

    @Autowired
    private TokenService tokenService;

    @BeforeEach
    void setUp() {
        eventRepository.deleteAll();
        fingerprintRepository.deleteAll();
    }

    @Test
    void shouldReturnAlertRecordsWithReportStatus() throws Exception {
        for (int i = 0; i < 5; i++) {
            exceptionService.saveEvent(buildEvent("payment-gateway", "CRITICAL", "provider timeout"));
        }

        mockMvc.perform(get("/api/alerts")
                        .header("Authorization", bearer(UserRole.VIEWER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].alertStatus").value("TRIGGERED"))
                .andExpect(jsonPath("$.data[0].reportStatus").value("COMPLETED"));
    }

    private ExceptionEventReq buildEvent(String serviceName, String severity, String message) {
        ExceptionEventReq req = new ExceptionEventReq();
        req.setTimestamp(Instant.now().toEpochMilli());
        req.setAppName("demo-app");
        req.setServiceName(serviceName);
        req.setEnvironment("test");
        req.setClassName("com.demo.Handler");
        req.setMethodName("handle");
        req.setMethodSignature("public void handle()");
        req.setExceptionClass("java.lang.IllegalStateException");
        req.setSeverity(severity);
        req.setMessage(message);
        req.setStackTrace("java.lang.IllegalStateException: " + message);
        req.setTopStackFrame("com.demo.Handler.handle(Handler.java:11)");
        return req;
    }

    private String bearer(UserRole role) {
        return "Bearer " + tokenService.issueToken(new AuthenticatedUser(
                role.name().toLowerCase(),
                role.name(),
                role,
                LocalDateTime.now().plusHours(1)
        ));
    }
}

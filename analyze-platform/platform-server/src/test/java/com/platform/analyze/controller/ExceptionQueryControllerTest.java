package com.platform.analyze.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ExceptionQueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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
    void shouldReturnFilteredExceptions() throws Exception {
        exceptionService.saveEvent(buildEvent("payment-gateway", "CRITICAL", "payment timeout"));
        exceptionService.saveEvent(buildEvent("user-center", "HIGH", "profile validation failed"));

        mockMvc.perform(get("/api/exceptions")
                        .header("Authorization", bearer(UserRole.VIEWER))
                        .param("severity", "CRITICAL")
                        .param("keyword", "timeout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].serviceName").value("payment-gateway"));
    }

    @Test
    void shouldUpdateStatusThroughHttpEndpoint() throws Exception {
        exceptionService.saveEvent(buildEvent("payment-gateway", "CRITICAL", "payment timeout"));
        Long id = eventRepository.findAll().get(0).getId();

        mockMvc.perform(patch("/api/exceptions/{id}/status", id)
                        .header("Authorization", bearer(UserRole.OPERATOR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of("status", "INVESTIGATING"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("INVESTIGATING"));
    }

    @Test
    void shouldReturnFingerprintRecords() throws Exception {
        for (int i = 0; i < 5; i++) {
            exceptionService.saveEvent(buildEvent("payment-gateway", "CRITICAL", "payment timeout"));
        }

        mockMvc.perform(get("/api/exceptions/fingerprints")
                        .header("Authorization", bearer(UserRole.VIEWER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].occurrenceCount").value(5));
    }

    @Test
    void shouldGenerateSuggestionThroughHttpEndpoint() throws Exception {
        exceptionService.saveEvent(buildEvent("payment-gateway", "CRITICAL", "payment timeout"));
        Long id = eventRepository.findAll().get(0).getId();

        mockMvc.perform(post("/api/exceptions/{id}/suggestion", id)
                        .header("Authorization", bearer(UserRole.OPERATOR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rootCauseAnalysis").isNotEmpty())
                .andExpect(jsonPath("$.data.fixSuggestion").isNotEmpty());
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

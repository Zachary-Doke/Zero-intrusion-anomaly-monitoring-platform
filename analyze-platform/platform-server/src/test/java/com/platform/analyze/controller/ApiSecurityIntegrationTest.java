package com.platform.analyze.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.analyze.auth.AuthenticatedUser;
import com.platform.analyze.auth.TokenService;
import com.platform.analyze.auth.UserRole;
import com.platform.analyze.dto.ExceptionEventReq;
import com.platform.analyze.dto.LoginRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ApiSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TokenService tokenService;

    @Test
    void shouldRejectViewerAccessToSettings() throws Exception {
        mockMvc.perform(get("/api/settings")
                        .header("Authorization", bearer(UserRole.VIEWER)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void shouldReturnRoleAwareLoginResponse() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("Admin@123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("admin"))
                .andExpect(jsonPath("$.data.role").value("ADMIN"))
                .andExpect(jsonPath("$.data.token").isNotEmpty());
    }

    @Test
    void shouldRequireAgentKeyForEventIngestion() throws Exception {
        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildEvent())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void shouldAcceptEventIngestionWithAgentKey() throws Exception {
        mockMvc.perform(post("/api/events")
                        .header("X-Agent-Key", "local-agent-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildEvent())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
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
        req.setAgentVersion("agent-test");
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

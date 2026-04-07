package com.platform.analyze.service;

import com.platform.analyze.dto.ExceptionEventReq;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Component
public class DemoDataInitializer implements ApplicationRunner {

    private final ExceptionService exceptionService;
    private final RuleSettingsService ruleSettingsService;

    public DemoDataInitializer(ExceptionService exceptionService,
                               RuleSettingsService ruleSettingsService) {
        this.exceptionService = exceptionService;
        this.ruleSettingsService = ruleSettingsService;
    }

    @Override
    public void run(ApplicationArguments args) {
        ruleSettingsService.getSettings();
        if (exceptionService.getAllFingerprints().isEmpty()) {
            exceptionService.saveEvents(List.of(
                    sampleEvent("payment-gateway", "java.sql.SQLTimeoutException", "DB lock timeout exceeded on transactions_log", "CRITICAL", 0),
                    sampleEvent("order-service", "java.lang.IllegalStateException", "order workflow state invalid for current transition", "HIGH", 1),
                    sampleEvent("checkout-api", "java.lang.NullPointerException", "cart item price missing in discount pipeline", "HIGH", 2),
                    sampleEvent("payment-gateway", "java.net.SocketTimeoutException", "upstream provider timeout during capture call", "CRITICAL", 3),
                    sampleEvent("user-center", "java.lang.IllegalArgumentException", "user profile payload contains invalid phone number", "MEDIUM", 4)
            ));
        }
    }

    private ExceptionEventReq sampleEvent(String serviceName,
                                          String exceptionClass,
                                          String message,
                                          String severity,
                                          int daysAgo) {
        ExceptionEventReq req = new ExceptionEventReq();
        req.setTimestamp(Instant.now().minusSeconds(daysAgo * 86400L + 3600L).toEpochMilli());
        req.setAppName("zero-monitor-demo");
        req.setServiceName(serviceName);
        req.setEnvironment("dev");
        req.setHost("localhost");
        req.setPid("demo-pid");
        req.setThreadName("demo-thread");
        req.setClassName("com.github.monitor.demo." + capitalize(serviceName) + "Handler");
        req.setMethodName("handleRequest");
        req.setMethodSignature("public void handleRequest(java.lang.String)");
        req.setArgumentsSnapshot(List.of("demo-input", Map.of("service", serviceName)));
        req.setExceptionClass(exceptionClass);
        req.setSeverity(severity);
        req.setMessage(message);
        req.setStackTrace(exceptionClass + ": " + message + "\n\tat com.github.monitor.demo." + capitalize(serviceName) + "Handler.handleRequest(" + capitalize(serviceName) + "Handler.java:42)");
        req.setTopStackFrame("com.github.monitor.demo." + capitalize(serviceName) + "Handler.handleRequest(" + capitalize(serviceName) + "Handler.java:42)");
        req.setTraceId("trace-" + serviceName + "-" + daysAgo);
        req.setTraceContext(Map.of("traceId", "trace-" + serviceName + "-" + daysAgo, "requestId", "req-" + daysAgo));
        req.setQueueSize(2);
        req.setDroppedCount(0L);
        req.setAgentVersion("agent-demo-1.1.0");
        return req;
    }

    private String capitalize(String value) {
        String[] parts = value.split("-");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return builder.toString();
    }
}

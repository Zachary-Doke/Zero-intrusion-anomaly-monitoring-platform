package com.github.monitor.agent;

import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.net.InetAddress;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class EventCollector {
    private static final String HOST;
    private static final String PID;

    static {
        String host = "unknown";
        try { host = InetAddress.getLocalHost().getHostName(); } catch (Exception e) {}
        HOST = host;
        
        String pid = "unknown";
        try { pid = ManagementFactory.getRuntimeMXBean().getName().split("@")[0]; } catch (Exception e) {}
        PID = pid;
    }

    public static void onException(
            Class<?> clazz,
            String methodName,
            String methodDesc,
            Object[] args,
            Object thisObj,
            Throwable throwable) {
        
        if (throwable == null) return;
        
        // Sampling
        double rate = AgentConfig.sampleRates.getOrDefault(throwable.getClass().getName(), AgentConfig.defaultSampleRate);
        if (rate < 1.0 && ThreadLocalRandom.current().nextDouble() > rate) {
            return;
        }

        Set<String> literals = new LinkedHashSet<String>();
        Event event = new Event();
        event.timestamp = System.currentTimeMillis();
        event.appName = AgentConfig.appName;
        event.serviceName = resolveServiceName(clazz);
        event.environment = AgentConfig.env;
        event.host = HOST;
        event.pid = PID;
        event.threadName = Thread.currentThread().getName();
        
        event.className = clazz.getName();
        event.methodName = methodName;
        event.methodSignature = methodDesc;
        
        // Raw references - will be serialized in async thread
        event.argumentsSnapshot = args; 
        if (AgentConfig.enableThisSnapshot) {
            event.thisSnapshot = thisObj;
        }
        
        event.exceptionClass = throwable.getClass().getName();
        event.severity = inferSeverity(throwable);
        event.message = SensitiveDataSanitizer.sanitizeText(throwable.getMessage(), literals);
        
        // Stacktrace
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement ste : throwable.getStackTrace()) {
            sb.append(ste.toString()).append("\n");
        }
        event.stackTrace = SensitiveDataSanitizer.sanitizeText(sb.toString(), literals);
        event.topStackFrame = throwable.getStackTrace().length > 0
                ? SensitiveDataSanitizer.sanitizeText(throwable.getStackTrace()[0].toString(), literals)
                : clazz.getName() + "." + methodName;
        
        // Trace Context (MDC)
        event.traceContext = SensitiveDataSanitizer.sanitizeMap(captureTraceContext(), literals);
        event.traceId = firstTraceId(event.traceContext);
        event.fingerprint = generateFingerprint(event.exceptionClass, event.topStackFrame, event.methodSignature);
        event.agentVersion = AgentConfig.agentVersion;
        event.configVersion = AgentConfig.configVersion;
        event.lastConfigSyncAt = AgentConfig.lastConfigSyncAt;
        event.lastConfigSyncStatus = AgentConfig.lastConfigSyncStatus;
        event.lastConfigSyncError = SensitiveDataSanitizer.sanitizeText(AgentConfig.lastConfigSyncError, literals);
        
        AsyncReporter.getInstance().report(event);
    }
    
    private static Map<String, String> captureTraceContext() {
        Map<String, String> context = new HashMap<>();
        if (AgentConfig.traceKeys.isEmpty()) return context;

        // Try SLF4J MDC via reflection
        try {
            Class<?> mdc = Class.forName("org.slf4j.MDC");
            java.lang.reflect.Method get = mdc.getMethod("get", String.class);
            for (String key : AgentConfig.traceKeys) {
                String val = (String) get.invoke(null, key);
                if (val != null) context.put(key, val);
            }
        } catch (Throwable t) {
            // ignore
        }
        return context;
    }

    private static String firstTraceId(Map<String, String> traceContext) {
        if (traceContext == null || traceContext.isEmpty()) {
            return null;
        }
        if (traceContext.containsKey("traceId")) {
            return traceContext.get("traceId");
        }
        if (traceContext.containsKey("requestId")) {
            return traceContext.get("requestId");
        }
        return traceContext.values().iterator().next();
    }

    private static String generateFingerprint(String exceptionClass, String topStackFrame, String methodSignature) {
        String raw = exceptionClass + "|" + normalizeStackFrame(topStackFrame) + "|" + methodSignature;
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                String value = Integer.toHexString(b & 0xff);
                if (value.length() == 1) {
                    hex.append('0');
                }
                hex.append(value);
            }
            return hex.toString();
        } catch (Exception e) {
            return raw;
        }
    }

    private static String normalizeStackFrame(String stackFrame) {
        if (stackFrame == null) {
            return "";
        }
        return stackFrame.replaceAll(":\\d+\\)", ")");
    }

    private static String resolveServiceName(Class<?> clazz) {
        if (AgentConfig.serviceName != null && !AgentConfig.serviceName.trim().isEmpty()) {
            return AgentConfig.serviceName;
        }
        return AgentConfig.appName;
    }

    private static String inferSeverity(Throwable throwable) {
        String className = throwable.getClass().getName().toLowerCase();
        String message = throwable.getMessage() == null ? "" : throwable.getMessage().toLowerCase();
        if (className.contains("outofmemory") || className.contains("timeout") || message.contains("timeout") || message.contains("deadlock")) {
            return "CRITICAL";
        }
        if (className.contains("illegal") || className.contains("nullpointer") || className.contains("state")) {
            return "HIGH";
        }
        return "MEDIUM";
    }
}

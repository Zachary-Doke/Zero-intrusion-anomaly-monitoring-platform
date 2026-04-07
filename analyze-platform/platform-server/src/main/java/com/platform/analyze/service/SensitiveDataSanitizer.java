package com.platform.analyze.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.analyze.dto.ExceptionEventReq;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SensitiveDataSanitizer {

    private static final String MASK = "***";
    private static final Set<String> DEFAULT_SENSITIVE_KEYS = Set.of(
            "password", "passwd", "pwd", "token", "secret", "credential", "authorization", "sensitive"
    );
    private static final Pattern BEARER_PATTERN = Pattern.compile("(?i)(bearer\\s+)([A-Za-z0-9._\\-]+)");

    private final ObjectMapper objectMapper;
    private final RuleSettingsService ruleSettingsService;

    public SensitiveDataSanitizer(ObjectMapper objectMapper, RuleSettingsService ruleSettingsService) {
        this.objectMapper = objectMapper;
        this.ruleSettingsService = ruleSettingsService;
    }

    public SanitizedEventPayload sanitize(ExceptionEventReq req) {
        Set<String> sensitiveKeys = effectiveSensitiveKeys();
        LinkedHashSet<String> literals = new LinkedHashSet<>();
        Object argumentsSnapshot = sanitizeStructuredValue(req.getArgumentsSnapshot(), sensitiveKeys, literals);
        Object thisSnapshot = sanitizeStructuredValue(req.getThisSnapshot(), sensitiveKeys, literals);
        @SuppressWarnings("unchecked")
        Map<String, String> traceContext = (Map<String, String>) sanitizeStructuredValue(req.getTraceContext(), sensitiveKeys, literals);
        String message = sanitizeText(req.getMessage(), sensitiveKeys, literals);
        String stackTrace = sanitizeText(req.getStackTrace(), sensitiveKeys, literals);
        String topStackFrame = sanitizeText(req.getTopStackFrame(), sensitiveKeys, literals);
        String traceId = replaceKnownLiterals(req.getTraceId(), literals);
        return new SanitizedEventPayload(argumentsSnapshot, thisSnapshot, traceContext, message, stackTrace, topStackFrame, traceId);
    }

    public String sanitizeText(String value) {
        return sanitizeText(value, effectiveSensitiveKeys(), new LinkedHashSet<>());
    }

    private Set<String> effectiveSensitiveKeys() {
        LinkedHashSet<String> keys = new LinkedHashSet<>(DEFAULT_SENSITIVE_KEYS);
        keys.addAll(ruleSettingsService.getSensitiveFieldNames());
        return keys;
    }

    private Object sanitizeStructuredValue(Object value, Set<String> sensitiveKeys, Set<String> literals) {
        if (value == null) {
            return null;
        }
        if (value instanceof String text) {
            return sanitizeStringValue(text, sensitiveKeys, literals);
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> sanitized = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = String.valueOf(entry.getKey());
                if (isSensitiveKey(key, sensitiveKeys)) {
                    collectLiteral(entry.getValue(), literals);
                }
            }
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = String.valueOf(entry.getKey());
                Object raw = entry.getValue();
                if (isSensitiveKey(key, sensitiveKeys)) {
                    collectLiteral(raw, literals);
                    sanitized.put(key, MASK);
                } else {
                    sanitized.put(key, sanitizeStructuredValue(raw, sensitiveKeys, literals));
                }
            }
            return sanitized;
        }
        if (value instanceof Collection<?> collection) {
            List<Object> sanitized = new ArrayList<>(collection.size());
            for (Object item : collection) {
                sanitized.add(sanitizeStructuredValue(item, sensitiveKeys, literals));
            }
            return sanitized;
        }
        if (value.getClass().isArray()) {
            int length = Array.getLength(value);
            List<Object> sanitized = new ArrayList<>(length);
            for (int i = 0; i < length; i++) {
                sanitized.add(sanitizeStructuredValue(Array.get(value, i), sensitiveKeys, literals));
            }
            return sanitized;
        }
        return value;
    }

    private Object sanitizeStringValue(String text, Set<String> sensitiveKeys, Set<String> literals) {
        if (!StringUtils.hasText(text)) {
            return text;
        }
        String trimmed = text.trim();
        if ((trimmed.startsWith("{") && trimmed.endsWith("}")) || (trimmed.startsWith("[") && trimmed.endsWith("]"))) {
            try {
                Object structured = objectMapper.readValue(trimmed, Object.class);
                return sanitizeStructuredValue(structured, sensitiveKeys, literals);
            } catch (JsonProcessingException ignored) {
                // Fall back to plain-text masking.
            }
        }
        return sanitizeText(text, sensitiveKeys, literals);
    }

    private String sanitizeText(String value, Set<String> sensitiveKeys, Set<String> literals) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        String sanitized = value;
        for (String key : sensitiveKeys) {
            String normalizedKey = Pattern.quote(key);
            Pattern pattern = Pattern.compile(
                    "(?i)(\"?" + normalizedKey + "\"?\\s*(?:[:=]|=>)\\s*\"?)([^\"\\s,;\\]}]+)"
            );
            sanitized = maskPattern(sanitized, pattern, literals);
        }
        sanitized = maskPattern(sanitized, BEARER_PATTERN, literals);
        return replaceKnownLiterals(sanitized, literals);
    }

    private String maskPattern(String value, Pattern pattern, Set<String> literals) {
        Matcher matcher = pattern.matcher(value);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String prefix = matcher.group(1);
            String raw = matcher.group(2);
            if (isMaskableLiteral(raw)) {
                literals.add(raw);
            }
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(prefix + MASK));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private String replaceKnownLiterals(String value, Set<String> literals) {
        if (!StringUtils.hasText(value) || literals.isEmpty()) {
            return value;
        }
        List<String> ordered = literals.stream()
                .filter(this::isMaskableLiteral)
                .sorted(Comparator.comparingInt(String::length).reversed())
                .toList();
        String sanitized = value;
        for (String literal : ordered) {
            sanitized = sanitized.replace(literal, MASK);
        }
        return sanitized;
    }

    private boolean isSensitiveKey(String key, Set<String> sensitiveKeys) {
        String normalized = key == null ? "" : key.trim().toLowerCase(Locale.ROOT);
        for (String sensitiveKey : sensitiveKeys) {
            if (!sensitiveKey.isBlank() && normalized.contains(sensitiveKey.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private void collectLiteral(Object value, Set<String> literals) {
        if (value == null) {
            return;
        }
        if (value instanceof Collection<?> collection) {
            for (Object item : collection) {
                collectLiteral(item, literals);
            }
            return;
        }
        if (value instanceof Map<?, ?> map) {
            for (Object item : map.values()) {
                collectLiteral(item, literals);
            }
            return;
        }
        if (value.getClass().isArray()) {
            int length = Array.getLength(value);
            for (int i = 0; i < length; i++) {
                collectLiteral(Array.get(value, i), literals);
            }
            return;
        }
        String literal = String.valueOf(value);
        if (isMaskableLiteral(literal)) {
            literals.add(literal);
        }
    }

    private boolean isMaskableLiteral(String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        String trimmed = value.trim();
        if (trimmed.length() < 3) {
            return false;
        }
        return trimmed.chars().anyMatch(Character::isLetterOrDigit);
    }

    public static final class SanitizedEventPayload {
        private final Object argumentsSnapshot;
        private final Object thisSnapshot;
        private final Map<String, String> traceContext;
        private final String message;
        private final String stackTrace;
        private final String topStackFrame;
        private final String traceId;

        private SanitizedEventPayload(Object argumentsSnapshot,
                                      Object thisSnapshot,
                                      Map<String, String> traceContext,
                                      String message,
                                      String stackTrace,
                                      String topStackFrame,
                                      String traceId) {
            this.argumentsSnapshot = argumentsSnapshot;
            this.thisSnapshot = thisSnapshot;
            this.traceContext = traceContext;
            this.message = message;
            this.stackTrace = stackTrace;
            this.topStackFrame = topStackFrame;
            this.traceId = traceId;
        }

        public Object getArgumentsSnapshot() {
            return argumentsSnapshot;
        }

        public Object getThisSnapshot() {
            return thisSnapshot;
        }

        public Map<String, String> getTraceContext() {
            return traceContext;
        }

        public String getMessage() {
            return message;
        }

        public String getStackTrace() {
            return stackTrace;
        }

        public String getTopStackFrame() {
            return topStackFrame;
        }

        public String getTraceId() {
            return traceId;
        }
    }
}

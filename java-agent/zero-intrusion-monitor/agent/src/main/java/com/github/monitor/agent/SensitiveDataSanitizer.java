package com.github.monitor.agent;

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

public final class SensitiveDataSanitizer {

    private static final String MASK = "***";
    private static final String[] DEFAULT_KEYS = new String[] {
            "password", "passwd", "pwd", "token", "secret", "credential", "authorization", "sensitive"
    };
    private static final Pattern BEARER_PATTERN = Pattern.compile("(?i)(bearer\\s+)([A-Za-z0-9._\\-]+)");

    private SensitiveDataSanitizer() {
    }

    public static String sanitizeText(String value) {
        return sanitizeText(value, new LinkedHashSet<String>());
    }

    public static String sanitizeText(String value, Set<String> literals) {
        if (value == null || value.trim().isEmpty()) {
            return value;
        }
        String sanitized = value;
        for (String key : effectiveKeys()) {
            String normalized = Pattern.quote(key);
            Pattern pattern = Pattern.compile(
                    "(?i)(\"?" + normalized + "\"?\\s*(?:[:=]|=>)\\s*\"?)([^\"\\s,;\\]}]+)"
            );
            sanitized = maskPattern(sanitized, pattern, literals);
        }
        sanitized = maskPattern(sanitized, BEARER_PATTERN, literals);
        return replaceKnownLiterals(sanitized, literals);
    }

    public static Map<String, String> sanitizeMap(Map<String, String> source, Set<String> literals) {
        if (source == null) {
            return null;
        }
        Map<String, String> sanitized = new LinkedHashMap<String, String>();
        for (Map.Entry<String, String> entry : source.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (isSensitiveKey(key)) {
                collectLiteral(value, literals);
                sanitized.put(key, MASK);
            } else {
                sanitized.put(key, sanitizeText(value, literals));
            }
        }
        return sanitized;
    }

    public static Object sanitizeStructured(Object value, Set<String> literals) {
        if (value == null) {
            return null;
        }
        if (value instanceof String) {
            return sanitizeText((String) value, literals);
        }
        if (value instanceof Map) {
            Map<?, ?> raw = (Map<?, ?>) value;
            Map<String, Object> sanitized = new LinkedHashMap<String, Object>();
            for (Map.Entry<?, ?> entry : raw.entrySet()) {
                String key = String.valueOf(entry.getKey());
                if (isSensitiveKey(key)) {
                    collectLiteral(entry.getValue(), literals);
                }
            }
            for (Map.Entry<?, ?> entry : raw.entrySet()) {
                String key = String.valueOf(entry.getKey());
                Object entryValue = entry.getValue();
                if (isSensitiveKey(key)) {
                    collectLiteral(entryValue, literals);
                    sanitized.put(key, MASK);
                } else {
                    sanitized.put(key, sanitizeStructured(entryValue, literals));
                }
            }
            return sanitized;
        }
        if (value instanceof Collection) {
            Collection<?> raw = (Collection<?>) value;
            List<Object> sanitized = new ArrayList<Object>(raw.size());
            for (Object item : raw) {
                sanitized.add(sanitizeStructured(item, literals));
            }
            return sanitized;
        }
        if (value.getClass().isArray()) {
            int length = Array.getLength(value);
            List<Object> sanitized = new ArrayList<Object>(length);
            for (int i = 0; i < length; i++) {
                sanitized.add(sanitizeStructured(Array.get(value, i), literals));
            }
            return sanitized;
        }
        return value;
    }

    public static String maskValue(Object value) {
        collectLiteral(value, new LinkedHashSet<String>());
        return MASK;
    }

    private static String maskPattern(String value, Pattern pattern, Set<String> literals) {
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

    private static String replaceKnownLiterals(String value, Set<String> literals) {
        if (value == null || value.isEmpty() || literals.isEmpty()) {
            return value;
        }
        List<String> ordered = new ArrayList<String>(literals);
        ordered.sort(new Comparator<String>() {
            @Override
            public int compare(String left, String right) {
                return Integer.compare(right.length(), left.length());
            }
        });
        String sanitized = value;
        for (String literal : ordered) {
            if (isMaskableLiteral(literal)) {
                sanitized = sanitized.replace(literal, MASK);
            }
        }
        return sanitized;
    }

    private static boolean isSensitiveKey(String key) {
        String normalized = key == null ? "" : key.trim().toLowerCase(Locale.ROOT);
        for (String candidate : effectiveKeys()) {
            if (!candidate.isEmpty() && normalized.contains(candidate.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private static List<String> effectiveKeys() {
        LinkedHashSet<String> keys = new LinkedHashSet<String>();
        for (String key : DEFAULT_KEYS) {
            keys.add(key);
        }
        for (String key : AgentConfig.sensitiveFields) {
            if (key != null && !key.trim().isEmpty()) {
                keys.add(key.trim());
            }
        }
        return new ArrayList<String>(keys);
    }

    private static void collectLiteral(Object value, Set<String> literals) {
        if (value == null) {
            return;
        }
        if (value instanceof Collection) {
            for (Object item : (Collection<?>) value) {
                collectLiteral(item, literals);
            }
            return;
        }
        if (value instanceof Map) {
            for (Object item : ((Map<?, ?>) value).values()) {
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

    private static boolean isMaskableLiteral(String value) {
        if (value == null) {
            return false;
        }
        String trimmed = value.trim();
        if (trimmed.length() < 3) {
            return false;
        }
        for (int i = 0; i < trimmed.length(); i++) {
            if (Character.isLetterOrDigit(trimmed.charAt(i))) {
                return true;
            }
        }
        return false;
    }
}

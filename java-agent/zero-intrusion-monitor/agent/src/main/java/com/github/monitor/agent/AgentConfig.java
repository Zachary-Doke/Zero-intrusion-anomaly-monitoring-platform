package com.github.monitor.agent;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class AgentConfig {
    public static String appName = "unknown-app";
    public static String serviceName = "unknown-service";
    public static String env = "dev";
    public static Set<String> packages = new HashSet<String>();
    public static String endpoint = "http://127.0.0.1:8080/api/events/batch";
    public static String agentVersion = "agent-1.1.0";
    public static String agentApiKey = "local-agent-key";
    public static String configEndpoint = "http://127.0.0.1:8080/api/settings/agent-runtime";
    public static String configConfirmEndpoint = "http://127.0.0.1:8080/api/settings/agent-runtime/confirm";
    public static long configVersion = 0L;
    public static long configSyncIntervalMs = 30000L;
    public static volatile String lastConfigSyncAt;
    public static volatile String lastConfigSyncStatus = "NEVER";
    public static volatile String lastConfigSyncError;

    public static double defaultSampleRate = 1.0;
    public static Map<String, Double> sampleRates = new HashMap<String, Double>();

    public static int queueCapacity = 10000;
    public static int batchSize = 10;
    public static int flushIntervalMs = 5000;

    public static int depthLimit = 5;
    public static int lengthLimit = 4096;
    public static int collectionLimit = 50;

    public static Set<String> fieldWhitelist = new HashSet<String>();
    public static Set<String> fieldBlacklist = new HashSet<String>();
    public static Set<String> sensitiveFields = new HashSet<String>();

    public static Set<String> traceKeys = new HashSet<String>();
    public static boolean enableThisSnapshot = false;
    public static boolean enableExperimentalLocalVars = false;
    public static boolean deepSamplingEnabled = false;

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static volatile long lastConfigSyncEpochMs = 0L;

    public static void parse(String args) {
        if (args == null || args.isEmpty()) {
            normalizeIdentity();
            return;
        }

        for (String pair : args.split(";")) {
            String[] kv = pair.split("=", 2);
            if (kv.length != 2) {
                continue;
            }
            String key = kv[0].trim();
            String value = kv[1].trim();

            switch (key) {
                case "appName":
                    appName = value;
                    break;
                case "serviceName":
                    serviceName = value;
                    break;
                case "env":
                    env = value;
                    break;
                case "packages":
                    packages = parseCsv(value);
                    break;
                case "endpoint":
                    endpoint = value;
                    break;
                case "agentVersion":
                    agentVersion = value;
                    break;
                case "agentApiKey":
                    agentApiKey = value;
                    break;
                case "configEndpoint":
                    configEndpoint = value;
                    break;
                case "configConfirmEndpoint":
                    configConfirmEndpoint = value;
                    break;
                case "configVersion":
                    configVersion = Long.parseLong(value);
                    break;
                case "configSyncIntervalMs":
                    configSyncIntervalMs = Long.parseLong(value);
                    break;
                case "defaultSampleRate":
                    defaultSampleRate = Double.parseDouble(value);
                    break;
                case "queueCapacity":
                    queueCapacity = Integer.parseInt(value);
                    break;
                case "batchSize":
                    batchSize = Integer.parseInt(value);
                    break;
                case "flushIntervalMs":
                    flushIntervalMs = Integer.parseInt(value);
                    break;
                case "depthLimit":
                    depthLimit = Integer.parseInt(value);
                    break;
                case "lengthLimit":
                    lengthLimit = Integer.parseInt(value);
                    break;
                case "collectionLimit":
                    collectionLimit = Integer.parseInt(value);
                    break;
                case "fieldWhitelist":
                    fieldWhitelist = parseCsv(value);
                    break;
                case "fieldBlacklist":
                    fieldBlacklist = parseCsv(value);
                    break;
                case "sensitiveFields":
                    sensitiveFields = parseCsv(value);
                    break;
                case "traceKeys":
                    traceKeys = parseCsv(value);
                    break;
                case "enableThisSnapshot":
                    enableThisSnapshot = Boolean.parseBoolean(value);
                    deepSamplingEnabled = enableThisSnapshot;
                    break;
                case "enableExperimentalLocalVars":
                    enableExperimentalLocalVars = Boolean.parseBoolean(value);
                    deepSamplingEnabled = enableExperimentalLocalVars || deepSamplingEnabled;
                    break;
                case "deepSamplingEnabled":
                    deepSamplingEnabled = Boolean.parseBoolean(value);
                    enableThisSnapshot = deepSamplingEnabled;
                    enableExperimentalLocalVars = deepSamplingEnabled;
                    break;
                default:
                    if (key.startsWith("sample.")) {
                        sampleRates.put(key.substring(7), Double.parseDouble(value));
                    }
                    break;
            }
        }
        normalizeIdentity();
    }

    static void normalizeIdentity() {
        if (appName == null || appName.trim().isEmpty()) {
            appName = "unknown-app";
        } else {
            appName = appName.trim();
        }

        if (serviceName == null || serviceName.trim().isEmpty()
                || "unknown-service".equalsIgnoreCase(serviceName.trim())) {
            serviceName = appName;
        } else {
            serviceName = serviceName.trim();
        }
    }

    public static synchronized void syncRemoteConfigNow() {
        if (configEndpoint == null || configEndpoint.trim().isEmpty()) {
            return;
        }
        lastConfigSyncEpochMs = System.currentTimeMillis();
        pullRemoteConfig();
    }

    public static synchronized void syncRemoteConfigIfNeeded() {
        if (configEndpoint == null || configEndpoint.trim().isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        if (lastConfigSyncEpochMs > 0L && now - lastConfigSyncEpochMs < configSyncIntervalMs) {
            return;
        }
        lastConfigSyncEpochMs = now;
        pullRemoteConfig();
    }

    private static void pullRemoteConfig() {
        HttpURLConnection conn = null;
        try {
            String query = "?serviceName=" + encode(serviceName) + "&appName=" + encode(appName);
            URL url = new URL(configEndpoint + query);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("X-Agent-Key", agentApiKey);

            int code = conn.getResponseCode();
            if (code >= 400) {
                recordSync("FAILED", "pull config http " + code);
                confirmRuntimeConfig();
                return;
            }

            InputStream inputStream = conn.getInputStream();
            Map root = MAPPER.readValue(inputStream, Map.class);
            Object codeValue = root.get("code");
            if (codeValue == null || ((Number) codeValue).intValue() != 200) {
                recordSync("FAILED", stringValue(root.get("message"), "pull config rejected"));
                confirmRuntimeConfig();
                return;
            }

            Map data = root.get("data") instanceof Map ? (Map) root.get("data") : Collections.emptyMap();
            boolean updated = applyRuntimeConfig(data);
            recordSync(updated ? "SUCCESS" : "UP_TO_DATE", null);
            confirmRuntimeConfig();
        } catch (Exception ex) {
            recordSync("FAILED", ex.getMessage());
            confirmRuntimeConfig();
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private static boolean applyRuntimeConfig(Map data) {
        if (data == null || data.isEmpty()) {
            return false;
        }

        boolean changed = false;
        long incomingVersion = longValue(data.get("version"), configVersion);
        if (incomingVersion != configVersion) {
            configVersion = incomingVersion;
            changed = true;
        }

        Set<String> incomingPackages = parseCsv(stringValue(data.get("packagePatterns"), joinCsv(packages)));
        if (!incomingPackages.isEmpty() && !incomingPackages.equals(packages)) {
            packages = incomingPackages;
            changed = true;
        }

        int incomingDepthLimit = intValue(data.get("depthLimit"), depthLimit);
        if (incomingDepthLimit != depthLimit) {
            depthLimit = incomingDepthLimit;
            changed = true;
        }

        int incomingLengthLimit = intValue(data.get("lengthLimit"), lengthLimit);
        if (incomingLengthLimit != lengthLimit) {
            lengthLimit = incomingLengthLimit;
            changed = true;
        }

        int incomingCollectionLimit = intValue(data.get("collectionLimit"), collectionLimit);
        if (incomingCollectionLimit != collectionLimit) {
            collectionLimit = incomingCollectionLimit;
            changed = true;
        }

        double incomingSampleRate = doubleValue(data.get("defaultSampleRate"), defaultSampleRate);
        if (Double.compare(incomingSampleRate, defaultSampleRate) != 0) {
            defaultSampleRate = incomingSampleRate;
            changed = true;
        }

        int incomingQueueCapacity = intValue(data.get("queueCapacity"), queueCapacity);
        if (incomingQueueCapacity != queueCapacity) {
            queueCapacity = incomingQueueCapacity;
            changed = true;
        }

        int incomingFlushIntervalMs = intValue(data.get("flushIntervalMs"), flushIntervalMs);
        if (incomingFlushIntervalMs != flushIntervalMs) {
            flushIntervalMs = incomingFlushIntervalMs;
            changed = true;
        }

        Set<String> incomingTraceKeys = parseCsv(stringValue(data.get("traceKeys"), joinCsv(traceKeys)));
        if (!incomingTraceKeys.equals(traceKeys)) {
            traceKeys = incomingTraceKeys;
            changed = true;
        }

        Set<String> incomingSensitiveFields = parseCsv(stringValue(data.get("sensitiveFields"), joinCsv(sensitiveFields)));
        if (!incomingSensitiveFields.equals(sensitiveFields)) {
            sensitiveFields = incomingSensitiveFields;
            changed = true;
        }

        boolean incomingDeepSamplingEnabled = booleanValue(data.get("deepSamplingEnabled"), deepSamplingEnabled);
        if (incomingDeepSamplingEnabled != deepSamplingEnabled) {
            deepSamplingEnabled = incomingDeepSamplingEnabled;
            enableThisSnapshot = incomingDeepSamplingEnabled;
            enableExperimentalLocalVars = incomingDeepSamplingEnabled;
            changed = true;
        }

        return changed;
    }

    private static void confirmRuntimeConfig() {
        if (configConfirmEndpoint == null || configConfirmEndpoint.trim().isEmpty()) {
            return;
        }

        HttpURLConnection conn = null;
        try {
            Map<String, Object> payload = new LinkedHashMap<String, Object>();
            payload.put("serviceName", serviceName);
            payload.put("appName", appName);
            payload.put("environment", env);
            payload.put("agentVersion", agentVersion);
            payload.put("configVersion", configVersion);
            payload.put("lastConfigSyncAt", lastConfigSyncAt);
            payload.put("lastConfigSyncStatus", lastConfigSyncStatus);
            payload.put("lastConfigSyncError", lastConfigSyncError);

            URL url = new URL(configConfirmEndpoint);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("X-Agent-Key", agentApiKey);

            OutputStream os = conn.getOutputStream();
            os.write(MAPPER.writeValueAsBytes(payload));
            os.flush();
            os.close();
            conn.getResponseCode();
        } catch (Exception ignored) {
            // Keep local sync state even when confirm fails.
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private static void recordSync(String status, String error) {
        lastConfigSyncAt = LocalDateTime.now().toString();
        lastConfigSyncStatus = status;
        lastConfigSyncError = error;
    }

    private static Set<String> parseCsv(String value) {
        LinkedHashSet<String> values = new LinkedHashSet<String>();
        if (value == null || value.trim().isEmpty()) {
            return values;
        }
        for (String item : value.split(",")) {
            String trimmed = item.trim();
            if (!trimmed.isEmpty()) {
                values.add(trimmed);
            }
        }
        return values;
    }

    private static String joinCsv(Set<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            if (builder.length() > 0) {
                builder.append(',');
            }
            builder.append(value);
        }
        return builder.toString();
    }

    private static String encode(String value) throws Exception {
        return URLEncoder.encode(value == null ? "" : value, "UTF-8");
    }

    private static String stringValue(Object value, String defaultValue) {
        return value == null ? defaultValue : String.valueOf(value);
    }

    private static int intValue(Object value, int defaultValue) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String && !((String) value).trim().isEmpty()) {
            return Integer.parseInt(((String) value).trim());
        }
        return defaultValue;
    }

    private static long longValue(Object value, long defaultValue) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String && !((String) value).trim().isEmpty()) {
            return Long.parseLong(((String) value).trim());
        }
        return defaultValue;
    }

    private static double doubleValue(Object value, double defaultValue) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof String && !((String) value).trim().isEmpty()) {
            return Double.parseDouble(((String) value).trim());
        }
        return defaultValue;
    }

    private static boolean booleanValue(Object value, boolean defaultValue) {
        if (value instanceof Boolean) {
            return ((Boolean) value).booleanValue();
        }
        if (value instanceof String && !((String) value).trim().isEmpty()) {
            return Boolean.parseBoolean(((String) value).trim());
        }
        return defaultValue;
    }
}

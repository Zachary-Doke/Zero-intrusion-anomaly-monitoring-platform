package com.github.monitor.agent;

import java.util.Map;

public class Event {
    public long timestamp;
    public String appName;
    public String serviceName;
    public String environment;
    public String host;
    public String pid;
    public String threadName;
    
    public String className;
    public String methodName;
    public String methodSignature;
    
    public Object argumentsSnapshot; // List<Object> or Map
    public Object thisSnapshot;      // Map<String, Object>
    
    public String exceptionClass;
    public String severity;
    public String message;
    public String stackTrace;
    public String topStackFrame;
    
    public String fingerprint;
    public String traceId;
    public Map<String, String> traceContext;
    
    // Metrics (optional, can be separate or included)
    public int queueSize;
    public long droppedCount;
    public String agentVersion;
    public Long configVersion;
    public String lastConfigSyncAt;
    public String lastConfigSyncStatus;
    public String lastConfigSyncError;
}

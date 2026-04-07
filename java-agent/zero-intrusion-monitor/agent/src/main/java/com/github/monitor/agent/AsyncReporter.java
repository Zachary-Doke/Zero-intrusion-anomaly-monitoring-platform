package com.github.monitor.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class AsyncReporter {
    private static AsyncReporter instance;
    private final ArrayBlockingQueue<Event> queue;
    private final Thread worker;
    private volatile boolean running = true;
    private final AtomicLong droppedCount = new AtomicLong(0);
    private final ObjectMapper mapper = new ObjectMapper();

    private AsyncReporter() {
        this.queue = new ArrayBlockingQueue<>(AgentConfig.queueCapacity);
        this.worker = new Thread(this::runWorker, "monitor-agent-reporter");
        this.worker.setDaemon(true);
        this.worker.start();
    }

    public static synchronized AsyncReporter getInstance() {
        if (instance == null) {
            instance = new AsyncReporter();
        }
        return instance;
    }

    public void report(Event event) {
        if (!queue.offer(event)) {
            droppedCount.incrementAndGet();
        }
    }

    private void runWorker() {
        List<Event> batch = new ArrayList<>(AgentConfig.batchSize);
        while (running) {
            try {
                AgentConfig.syncRemoteConfigIfNeeded();
                Event event = queue.poll(AgentConfig.flushIntervalMs, TimeUnit.MILLISECONDS);
                if (event != null) {
                    batch.add(processEvent(event));
                    queue.drainTo(batch, AgentConfig.batchSize - 1);
                }
                
                if (!batch.isEmpty()) {
                    // Process remaining events in batch
                    for (int i = 1; i < batch.size(); i++) {
                         batch.set(i, processEvent(batch.get(i)));
                    }
                    sendBatch(batch);
                    batch.clear();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Throwable t) {
                // Sallow exception to keep thread alive
                t.printStackTrace(); 
            }
        }
    }

    // Convert raw objects in Event to serialized snapshots
    private Event processEvent(Event e) {
        try {
            java.util.Set<String> literals = new java.util.LinkedHashSet<String>();
            // Snapshot arguments
            if (e.argumentsSnapshot != null && !(e.argumentsSnapshot instanceof String)) {
                e.argumentsSnapshot = SnapshotSerializer.serialize(SensitiveDataSanitizer.sanitizeStructured(e.argumentsSnapshot, literals));
            }
            // Snapshot this
            if (e.thisSnapshot != null && !(e.thisSnapshot instanceof String)) {
                 e.thisSnapshot = SnapshotSerializer.serialize(SensitiveDataSanitizer.sanitizeStructured(e.thisSnapshot, literals));
            }
            e.message = SensitiveDataSanitizer.sanitizeText(e.message, literals);
            e.stackTrace = SensitiveDataSanitizer.sanitizeText(e.stackTrace, literals);
            e.lastConfigSyncError = SensitiveDataSanitizer.sanitizeText(e.lastConfigSyncError, literals);
            // Fill metrics
            e.droppedCount = droppedCount.getAndSet(0);
            e.queueSize = queue.size();
        } catch (Throwable t) {
            e.message = (e.message == null ? "" : e.message) + " [Serialization Error: " + t.getMessage() + "]";
        }
        return e;
    }

    private void sendBatch(List<Event> events) {
        try {
            String json = mapper.writeValueAsString(events);
            URL url = new URL(AgentConfig.endpoint);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("X-Agent-Key", AgentConfig.agentApiKey);
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);
            
            try (OutputStream os = conn.getOutputStream()) {
                os.write(json.getBytes("UTF-8"));
                os.flush();
            }
            
            int code = conn.getResponseCode();
            if (code >= 400) {
                System.err.println("[MonitorAgent] Report failed, code: " + code);
            }
        } catch (Exception e) {
            // Simple retry or just log
            // System.err.println("[MonitorAgent] Report error: " + e.getMessage());
        }
    }
}

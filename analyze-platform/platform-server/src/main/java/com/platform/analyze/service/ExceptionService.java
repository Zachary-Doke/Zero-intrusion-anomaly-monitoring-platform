package com.platform.analyze.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.analyze.dto.ExceptionDetailDto;
import com.platform.analyze.dto.ExceptionEventReq;
import com.platform.analyze.dto.ExceptionListItemDto;
import com.platform.analyze.dto.ExceptionOverviewDto;
import com.platform.analyze.dto.ExceptionTrendDto;
import com.platform.analyze.dto.OverviewMetricDto;
import com.platform.analyze.entity.AiAnalysisResult;
import com.platform.analyze.entity.ExceptionEvent;
import com.platform.analyze.entity.ExceptionFingerprint;
import com.platform.analyze.repository.AiAnalysisResultRepository;
import com.platform.analyze.repository.ExceptionEventRepository;
import com.platform.analyze.repository.ExceptionFingerprintRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class ExceptionService {

    private final ExceptionEventRepository eventRepository;
    private final ExceptionFingerprintRepository fingerprintRepository;
    private final FingerprintGenerator fingerprintGenerator;
    private final ObjectMapper objectMapper;
    private final SensitiveDataSanitizer sensitiveDataSanitizer;
    private final AlertLifecycleService alertLifecycleService;
    private final AgentSyncStatusService agentSyncStatusService;
    private final RuleSettingsService ruleSettingsService;
    private final AiAnalysisResultRepository aiAnalysisResultRepository;

    public ExceptionService(ExceptionEventRepository eventRepository,
                            ExceptionFingerprintRepository fingerprintRepository,
                            FingerprintGenerator fingerprintGenerator,
                            ObjectMapper objectMapper,
                            SensitiveDataSanitizer sensitiveDataSanitizer,
                            AlertLifecycleService alertLifecycleService,
                            AgentSyncStatusService agentSyncStatusService,
                            RuleSettingsService ruleSettingsService,
                            AiAnalysisResultRepository aiAnalysisResultRepository) {
        this.eventRepository = eventRepository;
        this.fingerprintRepository = fingerprintRepository;
        this.fingerprintGenerator = fingerprintGenerator;
        this.objectMapper = objectMapper;
        this.sensitiveDataSanitizer = sensitiveDataSanitizer;
        this.alertLifecycleService = alertLifecycleService;
        this.agentSyncStatusService = agentSyncStatusService;
        this.ruleSettingsService = ruleSettingsService;
        this.aiAnalysisResultRepository = aiAnalysisResultRepository;
    }

    @Transactional
    public void saveEvent(ExceptionEventReq req) {
        SensitiveDataSanitizer.SanitizedEventPayload sanitized = sensitiveDataSanitizer.sanitize(req);
        String fingerprintValue = StringUtils.hasText(req.getFingerprint())
                ? req.getFingerprint()
                : fingerprintGenerator.generate(req.getExceptionClass(), sanitized.getTopStackFrame(), req.getMethodSignature());
        LocalDateTime occurrenceTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(req.getTimestamp()), ZoneId.systemDefault());
        String severity = normalizeSeverity(req.getSeverity(), req.getExceptionClass(), sanitized.getMessage());
        String summary = buildSummary(req, sanitized.getMessage());
        String serviceName = inferServiceName(req);
        String syncError = sensitiveDataSanitizer.sanitizeText(req.getLastConfigSyncError());

        agentSyncStatusService.captureEventSync(
                serviceName,
                req.getAppName(),
                req.getEnvironment(),
                req.getAgentVersion(),
                req.getConfigVersion(),
                req.getLastConfigSyncAt(),
                req.getLastConfigSyncStatus(),
                syncError
        );

        ExceptionEvent event = new ExceptionEvent();
        event.setFingerprint(fingerprintValue);
        event.setAppName(req.getAppName());
        event.setServiceName(serviceName);
        event.setEnvironment(req.getEnvironment());
        event.setHost(req.getHost());
        event.setPid(req.getPid());
        event.setThreadName(req.getThreadName());
        event.setClassName(req.getClassName());
        event.setMethodName(req.getMethodName());
        event.setMethodSignature(req.getMethodSignature());
        event.setArgumentsSnapshot(writeJson(sanitized.getArgumentsSnapshot(), "argumentsSnapshot"));
        event.setThisSnapshot(writeJson(sanitized.getThisSnapshot(), "thisSnapshot"));
        event.setExceptionClass(req.getExceptionClass());
        event.setSeverity(severity);
        event.setStatus("OPEN");
        event.setSummary(summary);
        event.setMessage(sanitized.getMessage());
        event.setStackTrace(sanitized.getStackTrace());
        event.setTopStackFrame(sanitized.getTopStackFrame());
        event.setOccurrenceTime(occurrenceTime);
        event.setTraceId(sanitized.getTraceId());
        event.setTraceContext(writeJson(sanitized.getTraceContext(), "traceContext"));
        event.setQueueSize(req.getQueueSize());
        event.setDroppedCount(req.getDroppedCount());
        event.setAgentVersion(req.getAgentVersion());
        event.setConfigVersion(req.getConfigVersion());
        event.setLastConfigSyncAt(req.getLastConfigSyncAt());
        event.setLastConfigSyncStatus(req.getLastConfigSyncStatus());
        event.setLastConfigSyncError(syncError);
        eventRepository.save(event);

        ExceptionFingerprint fingerprint = fingerprintRepository.findById(fingerprintValue)
                .orElseGet(() -> {
                    ExceptionFingerprint item = new ExceptionFingerprint();
                    item.setFingerprint(fingerprintValue);
                    item.setExceptionClass(req.getExceptionClass());
                    item.setServiceName(serviceName);
                    item.setSeverity(severity);
                    item.setStatus("OPEN");
                    item.setSummary(summary);
                    item.setTopStackFrame(sanitized.getTopStackFrame());
                    item.setMethodSignature(req.getMethodSignature());
                    item.setOccurrenceCount(0L);
                    item.setAlertStatus("PENDING");
                    item.setAlertCount(0L);
                    item.setAlertTriggeredAt(null);
                    item.setLastNotificationStatus(null);
                    item.setFirstSeen(occurrenceTime);
                    return item;
                });

        fingerprint.setServiceName(serviceName);
        fingerprint.setSeverity(moreSevere(fingerprint.getSeverity(), severity));
        fingerprint.setStatus("OPEN");
        fingerprint.setSummary(summary);
        fingerprint.setTopStackFrame(sanitized.getTopStackFrame());
        fingerprint.setOccurrenceCount(fingerprint.getOccurrenceCount() + 1);
        if (fingerprint.getFirstSeen() == null || occurrenceTime.isBefore(fingerprint.getFirstSeen())) {
            fingerprint.setFirstSeen(occurrenceTime);
        }
        if (fingerprint.getLastSeen() == null || occurrenceTime.isAfter(fingerprint.getLastSeen())) {
            fingerprint.setLastSeen(occurrenceTime);
        }
        fingerprintRepository.save(fingerprint);
        alertLifecycleService.refreshFingerprint(fingerprintValue, occurrenceTime);
    }

    @Transactional
    public void saveEvents(List<ExceptionEventReq> requests) {
        for (ExceptionEventReq request : requests) {
            saveEvent(request);
        }
    }

    public Page<ExceptionListItemDto> getEvents(String severity,
                                                String status,
                                                String serviceName,
                                                String keyword,
                                                Integer days,
                                                Pageable pageable) {
        return eventRepository.findAll(buildEventSpecification(severity, status, serviceName, keyword, days), pageable)
                .map(this::toListItem);
    }

    public ExceptionDetailDto getEventDetail(Long id) {
        ExceptionEvent event = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Exception Event not found"));
        return toDetail(event);
    }

    public List<ExceptionFingerprint> getAllFingerprints() {
        return sortedFingerprints();
    }

    public List<ExceptionTrendDto> getTrends(Integer days) {
        int normalizedDays = days == null || days <= 0 ? 7 : days;
        LocalDateTime start = LocalDateTime.now().minusDays(normalizedDays - 1L).truncatedTo(ChronoUnit.DAYS);
        List<ExceptionEvent> events = eventRepository.findByOccurrenceTimeGreaterThanEqualOrderByOccurrenceTimeAsc(start);
        Map<String, Long> countsByDate = new LinkedHashMap<>();

        for (int i = 0; i < normalizedDays; i++) {
            countsByDate.put(start.plusDays(i).toLocalDate().toString(), 0L);
        }
        for (ExceptionEvent event : events) {
            String key = event.getOccurrenceTime().toLocalDate().toString();
            countsByDate.computeIfPresent(key, (ignored, value) -> value + 1);
        }

        List<ExceptionTrendDto> trends = new ArrayList<>();
        for (Map.Entry<String, Long> entry : countsByDate.entrySet()) {
            trends.add(new ExceptionTrendDto(entry.getKey(), entry.getValue()));
        }
        return trends;
    }

    public ExceptionOverviewDto buildOverview() {
        List<ExceptionFingerprint> fingerprints = fingerprintRepository.findAll();
        long totalExceptions = eventRepository.count();
        long fingerprintCount = fingerprints.size();
        long openExceptionCount = eventRepository.countByStatus("OPEN");
        long criticalExceptionCount = eventRepository.countBySeverityAndStatus("CRITICAL", "OPEN");
        long serviceCount = fingerprints.stream()
                .map(ExceptionFingerprint::getServiceName)
                .filter(StringUtils::hasText)
                .distinct()
                .count();
        long triggeredAlertCount = fingerprintRepository.countByAlertStatus("TRIGGERED");
        long aiReportCount = aiAnalysisResultRepository.count();
        long averageAnalysisMinutes = computeAverageAnalysisMinutes();
        long agentCount = agentSyncStatusService.countAgents();
        long effectiveAgentCount = agentSyncStatusService.countEffective(ruleSettingsService.currentSettings().getVersion());
        OverviewMetricDto metrics = new OverviewMetricDto(
                totalExceptions,
                fingerprintCount,
                openExceptionCount,
                criticalExceptionCount,
                serviceCount,
                triggeredAlertCount,
                aiReportCount,
                averageAnalysisMinutes,
                agentCount,
                effectiveAgentCount
        );
        List<ExceptionListItemDto> recentEvents = eventRepository.findAll(
                        PageRequest.of(0, 8, Sort.by(Sort.Direction.DESC, "occurrenceTime"))
                )
                .getContent()
                .stream()
                .map(this::toListItem)
                .toList();
        List<ExceptionFingerprint> topFingerprints = fingerprintRepository
                .findTop8ByAlertStatusOrderByAlertTriggeredAtDesc("TRIGGERED");
        if (topFingerprints.isEmpty()) {
            topFingerprints = sortedFingerprints().stream().limit(8).toList();
        }
        return new ExceptionOverviewDto(metrics, getTrends(7), recentEvents, topFingerprints);
    }

    @Transactional
    public ExceptionDetailDto updateStatus(Long id, String status) {
        ExceptionEvent event = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Exception Event not found"));
        event.setStatus(normalizeStatus(status));
        eventRepository.save(event);

        String fingerprintId = event.getFingerprint();
        List<ExceptionEvent> fingerprintEvents = eventRepository.findAll((root, query, cb) ->
                cb.equal(root.get("fingerprint"), fingerprintId));
        boolean hasOpen = fingerprintEvents.stream().anyMatch(item -> "OPEN".equals(item.getStatus()));
        fingerprintRepository.findById(fingerprintId).ifPresent(item -> {
            item.setStatus(hasOpen ? "OPEN" : event.getStatus());
            fingerprintRepository.save(item);
        });
        return toDetail(event);
    }

    private Specification<ExceptionEvent> buildEventSpecification(String severity,
                                                                  String status,
                                                                  String serviceName,
                                                                  String keyword,
                                                                  Integer days) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(severity)) {
                predicates.add(cb.equal(root.get("severity"), severity.trim().toUpperCase(Locale.ROOT)));
            }
            if (StringUtils.hasText(status)) {
                predicates.add(cb.equal(root.get("status"), normalizeStatus(status)));
            }
            if (StringUtils.hasText(serviceName)) {
                predicates.add(cb.equal(root.get("serviceName"), serviceName.trim()));
            }
            if (StringUtils.hasText(keyword)) {
                String pattern = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("summary")), pattern),
                        cb.like(cb.lower(root.get("exceptionClass")), pattern),
                        cb.like(cb.lower(root.get("fingerprint")), pattern),
                        cb.like(cb.lower(root.get("message")), pattern)
                ));
            }
            if (days != null && days > 0) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("occurrenceTime"), LocalDateTime.now().minusDays(days)));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private ExceptionListItemDto toListItem(ExceptionEvent event) {
        String alertStatus = fingerprintRepository.findById(event.getFingerprint())
                .map(ExceptionFingerprint::getAlertStatus)
                .orElse(null);
        return new ExceptionListItemDto(
                event.getId(),
                event.getFingerprint(),
                event.getSummary(),
                event.getSeverity(),
                event.getStatus(),
                alertStatus,
                event.getAppName(),
                event.getServiceName(),
                event.getExceptionClass(),
                event.getMethodName(),
                event.getTopStackFrame(),
                event.getOccurrenceTime(),
                event.getTraceId()
        );
    }

    private ExceptionDetailDto toDetail(ExceptionEvent event) {
        ExceptionFingerprint fingerprint = fingerprintRepository.findById(event.getFingerprint()).orElse(null);
        return new ExceptionDetailDto(
                event.getId(),
                event.getFingerprint(),
                event.getSummary(),
                event.getSeverity(),
                event.getStatus(),
                fingerprint == null ? null : fingerprint.getAlertStatus(),
                fingerprint == null ? null : fingerprint.getAlertCount(),
                fingerprint == null ? null : fingerprint.getAlertTriggeredAt(),
                fingerprint == null ? null : fingerprint.getLastNotificationStatus(),
                event.getAppName(),
                event.getServiceName(),
                event.getEnvironment(),
                event.getHost(),
                event.getPid(),
                event.getThreadName(),
                event.getClassName(),
                event.getMethodName(),
                event.getMethodSignature(),
                event.getExceptionClass(),
                event.getMessage(),
                event.getStackTrace(),
                event.getTopStackFrame(),
                event.getArgumentsSnapshot(),
                event.getThisSnapshot(),
                event.getTraceId(),
                event.getTraceContext(),
                event.getQueueSize(),
                event.getDroppedCount(),
                event.getAgentVersion(),
                event.getConfigVersion(),
                event.getLastConfigSyncAt(),
                event.getLastConfigSyncStatus(),
                event.getLastConfigSyncError(),
                event.getOccurrenceTime()
        );
    }

    private List<ExceptionFingerprint> sortedFingerprints() {
        return fingerprintRepository.findAll().stream()
                .sorted(Comparator
                        .comparingLong((ExceptionFingerprint item) -> item.getOccurrenceCount() == null ? 0L : item.getOccurrenceCount())
                        .reversed()
                        .thenComparing(ExceptionFingerprint::getLastSeen, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private String inferServiceName(ExceptionEventReq req) {
        if (StringUtils.hasText(req.getServiceName())) {
            return req.getServiceName().trim();
        }
        if (StringUtils.hasText(req.getAppName())) {
            return req.getAppName().trim();
        }
        if (!StringUtils.hasText(req.getClassName())) {
            return "unknown-service";
        }
        String className = req.getClassName();
        int packageEnd = className.lastIndexOf('.');
        if (packageEnd <= 0) {
            return className;
        }
        String packageName = className.substring(0, packageEnd);
        int serviceStart = packageName.lastIndexOf('.');
        return serviceStart >= 0 ? packageName.substring(serviceStart + 1) : packageName;
    }

    private String buildSummary(ExceptionEventReq req, String sanitizedMessage) {
        if (StringUtils.hasText(sanitizedMessage)) {
            String message = sanitizedMessage.trim();
            return message.length() > 160 ? message.substring(0, 160) + "..." : message;
        }
        return req.getExceptionClass() + " @ " + req.getMethodName();
    }

    private String normalizeSeverity(String severity, String exceptionClass, String message) {
        if (StringUtils.hasText(severity)) {
            return severity.trim().toUpperCase(Locale.ROOT);
        }
        String lowered = (exceptionClass + " " + (message == null ? "" : message)).toLowerCase(Locale.ROOT);
        if (lowered.contains("outofmemory") || lowered.contains("timeout") || lowered.contains("deadlock")) {
            return "CRITICAL";
        }
        if (lowered.contains("illegal") || lowered.contains("nullpointer") || lowered.contains("state")) {
            return "HIGH";
        }
        return "MEDIUM";
    }

    private String normalizeStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return "OPEN";
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        if ("INVESTIGATING".equals(normalized) || "RESOLVED".equals(normalized) || "OPEN".equals(normalized)) {
            return normalized;
        }
        return "OPEN";
    }

    private String moreSevere(String current, String incoming) {
        return severityWeight(incoming) > severityWeight(current) ? incoming : current;
    }

    private int severityWeight(String severity) {
        if ("CRITICAL".equalsIgnoreCase(severity)) {
            return 4;
        }
        if ("HIGH".equalsIgnoreCase(severity)) {
            return 3;
        }
        if ("MEDIUM".equalsIgnoreCase(severity)) {
            return 2;
        }
        return 1;
    }

    private long computeAverageAnalysisMinutes() {
        List<AiAnalysisResult> items = aiAnalysisResultRepository.findAll();
        if (items.isEmpty()) {
            return 0L;
        }
        return Math.max(1L, Math.round(items.stream()
                .filter(item -> "COMPLETED".equalsIgnoreCase(item.getReportStatus()))
                .filter(item -> item.getStartedAt() != null && item.getAnalysisTime() != null)
                .map(item -> Duration.between(item.getStartedAt(), item.getAnalysisTime()).toMinutes())
                .mapToLong(Long::longValue)
                .average()
                .orElse(0D)));
    }

    private String writeJson(Object value, String fieldName) {
        if (value == null) {
            return null;
        }
        if (value instanceof String text) {
            return text;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(fieldName + " 序列化失败", e);
        }
    }
}

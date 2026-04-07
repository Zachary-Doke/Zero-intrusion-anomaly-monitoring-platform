package com.platform.analyze.service;

import com.platform.analyze.dto.AlertRecordDto;
import com.platform.analyze.entity.AiAnalysisResult;
import com.platform.analyze.entity.AlertNotificationLog;
import com.platform.analyze.entity.ExceptionFingerprint;
import com.platform.analyze.repository.AiAnalysisResultRepository;
import com.platform.analyze.repository.AlertNotificationLogRepository;
import com.platform.analyze.repository.ExceptionFingerprintRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AlertRecordService {

    private final AlertNotificationLogRepository alertLogRepository;
    private final ExceptionFingerprintRepository fingerprintRepository;
    private final AiAnalysisResultRepository aiAnalysisResultRepository;

    public AlertRecordService(AlertNotificationLogRepository alertLogRepository,
                              ExceptionFingerprintRepository fingerprintRepository,
                              AiAnalysisResultRepository aiAnalysisResultRepository) {
        this.alertLogRepository = alertLogRepository;
        this.fingerprintRepository = fingerprintRepository;
        this.aiAnalysisResultRepository = aiAnalysisResultRepository;
    }

    public List<AlertRecordDto> listAlerts(String keyword, String alertStatus, String sendStatus) {
        Map<String, ExceptionFingerprint> fingerprintMap = fingerprintRepository.findAll().stream()
                .collect(Collectors.toMap(ExceptionFingerprint::getFingerprint, Function.identity(), (left, right) -> left));
        Map<String, AiAnalysisResult> reportMap = aiAnalysisResultRepository.findAll().stream()
                .collect(Collectors.toMap(AiAnalysisResult::getFingerprint, Function.identity(), (left, right) -> left));
        return alertLogRepository.findAllByOrderByTriggeredAtDesc().stream()
                .map(log -> toDto(log, fingerprintMap.get(log.getFingerprint()), reportMap.get(log.getFingerprint())))
                .filter(item -> !StringUtils.hasText(alertStatus) || item.getAlertStatus().equalsIgnoreCase(alertStatus))
                .filter(item -> !StringUtils.hasText(sendStatus) || item.getSendStatus().equalsIgnoreCase(sendStatus))
                .filter(item -> matchKeyword(item, keyword))
                .toList();
    }

    private AlertRecordDto toDto(AlertNotificationLog log, ExceptionFingerprint fingerprint, AiAnalysisResult report) {
        return new AlertRecordDto(
                log.getId(),
                log.getFingerprint(),
                fingerprint == null ? null : fingerprint.getServiceName(),
                fingerprint == null ? null : fingerprint.getSeverity(),
                fingerprint == null ? log.getContent() : fingerprint.getSummary(),
                fingerprint == null ? "TRIGGERED" : fingerprint.getAlertStatus(),
                log.getAlertCount(),
                log.getChannel(),
                log.getSendStatus(),
                log.getRecipients(),
                log.getContent(),
                report == null ? "NOT_GENERATED" : report.getReportStatus(),
                report == null ? null : report.getTriggerSource(),
                log.getTriggeredAt(),
                report == null ? null : report.getRequestedAt(),
                report == null ? null : report.getAnalysisTime()
        );
    }

    private boolean matchKeyword(AlertRecordDto item, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return true;
        }
        String pattern = keyword.trim().toLowerCase(Locale.ROOT);
        return contains(item.getFingerprint(), pattern)
                || contains(item.getServiceName(), pattern)
                || contains(item.getSummary(), pattern)
                || contains(item.getRecipients(), pattern);
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(keyword);
    }
}

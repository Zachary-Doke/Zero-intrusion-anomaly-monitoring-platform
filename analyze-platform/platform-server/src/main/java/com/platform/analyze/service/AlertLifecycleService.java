package com.platform.analyze.service;

import com.platform.analyze.ai.AiAnalysisService;
import com.platform.analyze.entity.ExceptionFingerprint;
import com.platform.analyze.entity.RuleSettings;
import com.platform.analyze.repository.ExceptionEventRepository;
import com.platform.analyze.repository.ExceptionFingerprintRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AlertLifecycleService {

    private final ExceptionEventRepository eventRepository;
    private final ExceptionFingerprintRepository fingerprintRepository;
    private final RuleSettingsService ruleSettingsService;
    private final AlertNotificationService alertNotificationService;
    private final AiAnalysisService aiAnalysisService;

    public AlertLifecycleService(ExceptionEventRepository eventRepository,
                                 ExceptionFingerprintRepository fingerprintRepository,
                                 RuleSettingsService ruleSettingsService,
                                 AlertNotificationService alertNotificationService,
                                 AiAnalysisService aiAnalysisService) {
        this.eventRepository = eventRepository;
        this.fingerprintRepository = fingerprintRepository;
        this.ruleSettingsService = ruleSettingsService;
        this.alertNotificationService = alertNotificationService;
        this.aiAnalysisService = aiAnalysisService;
    }

    @Transactional
    public ExceptionFingerprint refreshFingerprint(String fingerprintId, LocalDateTime occurrenceTime) {
        ExceptionFingerprint fingerprint = fingerprintRepository.findById(fingerprintId)
                .orElseThrow(() -> new RuntimeException("Exception Fingerprint not found"));
        RuleSettings settings = ruleSettingsService.currentSettings();
        LocalDateTime windowStart = occurrenceTime.minusMinutes(Math.max(1, settings.getThresholdWindowMinutes()));
        long matchedEvents = eventRepository.countByFingerprintAndOccurrenceTimeGreaterThanEqual(fingerprintId, windowStart);
        boolean thresholdReached = matchedEvents >= Math.max(1, settings.getThresholdCount());

        fingerprint.setAlertCount(matchedEvents);
        if (thresholdReached) {
            if (!"TRIGGERED".equalsIgnoreCase(fingerprint.getAlertStatus())) {
                fingerprint.setAlertStatus("TRIGGERED");
                fingerprint.setAlertTriggeredAt(occurrenceTime);
                fingerprint.setLastNotificationStatus(
                        alertNotificationService.logTriggeredAlert(fingerprint, settings, matchedEvents)
                );
            }
            fingerprintRepository.save(fingerprint);
            aiAnalysisService.analyzeIfAbsent(fingerprintId, "AUTO");
            return fingerprint;
        }

        if (!"RESOLVED".equalsIgnoreCase(fingerprint.getStatus())) {
            fingerprint.setAlertStatus("PENDING");
            fingerprint.setLastNotificationStatus(null);
        }
        return fingerprintRepository.save(fingerprint);
    }
}

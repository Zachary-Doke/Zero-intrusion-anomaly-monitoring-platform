package com.platform.analyze.service;

import com.platform.analyze.entity.AlertNotificationLog;
import com.platform.analyze.entity.ExceptionFingerprint;
import com.platform.analyze.entity.RuleSettings;
import com.platform.analyze.repository.AlertNotificationLogRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AlertNotificationService {

    private final AlertNotificationLogRepository repository;

    public AlertNotificationService(AlertNotificationLogRepository repository) {
        this.repository = repository;
    }

    public String logTriggeredAlert(ExceptionFingerprint fingerprint, RuleSettings settings, long alertCount) {
        AlertNotificationLog log = new AlertNotificationLog();
        log.setFingerprint(fingerprint.getFingerprint());
        log.setRecipients(settings.getAlertRecipients());
        log.setChannel("IN_APP_LOG");
        log.setSendStatus("LOGGED");
        log.setAlertCount(alertCount);
        log.setTriggeredAt(LocalDateTime.now());
        log.setContent(fingerprint.getServiceName() + " / " + fingerprint.getSummary());
        repository.save(log);
        return log.getSendStatus();
    }
}

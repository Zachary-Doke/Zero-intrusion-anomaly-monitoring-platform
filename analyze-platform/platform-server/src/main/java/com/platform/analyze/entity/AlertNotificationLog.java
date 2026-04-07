package com.platform.analyze.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "alert_notification_log")
public class AlertNotificationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String fingerprint;

    @Column(nullable = false, length = 1024)
    private String recipients;

    @Column(nullable = false)
    private String channel;

    @Column(nullable = false)
    private String sendStatus;

    @Column(nullable = false)
    private Long alertCount;

    @Column(nullable = false)
    private LocalDateTime triggeredAt;

    @Column(length = 1024)
    private String content;
}

package com.platform.analyze.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 异常指纹聚合实体类
 */
@Data
@Entity
@Table(name = "exception_fingerprint")
public class ExceptionFingerprint {

    @Id
    @Column(length = 64)
    private String fingerprint;

    @Column(nullable = false)
    private String exceptionClass;

    @Column
    private String serviceName;

    @Column(nullable = false)
    private String severity;

    @Column(nullable = false)
    private String status;

    @Column(length = 512)
    private String summary;

    @Column(nullable = false)
    private String topStackFrame;

    @Column(nullable = false)
    private String methodSignature;

    @Column(nullable = false)
    private Long occurrenceCount;

    @Column(nullable = false)
    private String alertStatus;

    @Column
    private LocalDateTime alertTriggeredAt;

    @Column(nullable = false)
    private Long alertCount;

    @Column
    private String lastNotificationStatus;

    @Column(nullable = false)
    private LocalDateTime firstSeen;

    @Column(nullable = false)
    private LocalDateTime lastSeen;
}

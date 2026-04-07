package com.platform.analyze.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 异常事件实体类
 */
@Data
@Entity
@Table(name = "exception_event")
public class ExceptionEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String fingerprint;

    @Column(nullable = false)
    private String appName;

    @Column
    private String serviceName;

    @Column
    private String environment;

    @Column
    private String host;

    @Column
    private String pid;

    @Column
    private String threadName;

    @Column(nullable = false)
    private String className;

    @Column(nullable = false)
    private String methodName;

    @Column(nullable = false)
    private String methodSignature;

    @Column(columnDefinition = "TEXT")
    private String argumentsSnapshot;

    @Column(columnDefinition = "TEXT")
    private String thisSnapshot;

    @Column(nullable = false)
    private String exceptionClass;

    @Column(nullable = false)
    private String severity;

    @Column(nullable = false)
    private String status;

    @Column(length = 512)
    private String summary;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(columnDefinition = "TEXT")
    private String stackTrace;

    @Column(nullable = false)
    private String topStackFrame;

    @Column(nullable = false)
    private LocalDateTime occurrenceTime;

    @Column
    private String traceId;

    @Column(columnDefinition = "TEXT")
    private String traceContext;

    @Column
    private Integer queueSize;

    @Column
    private Long droppedCount;

    @Column
    private String agentVersion;

    @Column
    private Long configVersion;

    @Column
    private LocalDateTime lastConfigSyncAt;

    @Column
    private String lastConfigSyncStatus;

    @Column(length = 1024)
    private String lastConfigSyncError;
}

package com.platform.analyze.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "agent_sync_status")
public class AgentSyncStatus {

    @Id
    private String id;

    @Column(nullable = false)
    private String serviceName;

    @Column(nullable = false)
    private String appName;

    @Column
    private String environment;

    @Column
    private String agentVersion;

    @Column(nullable = false)
    private Long targetConfigVersion;

    @Column
    private Long lastConfirmedConfigVersion;

    @Column
    private Long lastSuccessfulConfigVersion;

    @Column
    private LocalDateTime lastPulledAt;

    @Column
    private LocalDateTime lastSeenAt;

    @Column
    private LocalDateTime lastConfigSyncAt;

    @Column
    private String lastConfigSyncStatus;

    @Column(length = 1024)
    private String lastConfigSyncError;
}

package com.platform.analyze.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class AgentSyncStatusDto {

    private String serviceName;
    private String appName;
    private String environment;
    private String agentVersion;
    private Long targetConfigVersion;
    private Long lastConfirmedConfigVersion;
    private Long lastSuccessfulConfigVersion;
    private LocalDateTime lastPulledAt;
    private LocalDateTime lastSeenAt;
    private LocalDateTime lastConfigSyncAt;
    private String lastConfigSyncStatus;
    private String lastConfigSyncError;
    private boolean effective;
}

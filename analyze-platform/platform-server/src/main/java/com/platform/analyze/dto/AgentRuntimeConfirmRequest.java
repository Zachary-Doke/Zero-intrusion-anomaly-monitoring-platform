package com.platform.analyze.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AgentRuntimeConfirmRequest {

    @NotBlank(message = "serviceName 不能为空")
    private String serviceName;

    @NotBlank(message = "appName 不能为空")
    private String appName;

    private String environment;

    private String agentVersion;

    @NotNull(message = "configVersion 不能为空")
    private Long configVersion;

    private LocalDateTime lastConfigSyncAt;

    private String lastConfigSyncStatus;

    private String lastConfigSyncError;
}

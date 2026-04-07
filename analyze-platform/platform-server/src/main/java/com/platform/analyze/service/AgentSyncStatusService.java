package com.platform.analyze.service;

import com.platform.analyze.dto.AgentRuntimeConfirmRequest;
import com.platform.analyze.dto.AgentSyncStatusDto;
import com.platform.analyze.entity.AgentSyncStatus;
import com.platform.analyze.repository.AgentSyncStatusRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AgentSyncStatusService {

    private final AgentSyncStatusRepository repository;

    public AgentSyncStatusService(AgentSyncStatusRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void markPulled(String serviceName, String appName, long targetConfigVersion) {
        AgentSyncStatus status = loadOrCreate(serviceName, appName);
        status.setTargetConfigVersion(targetConfigVersion);
        status.setLastPulledAt(LocalDateTime.now());
        repository.save(status);
    }

    @Transactional
    public void confirm(AgentRuntimeConfirmRequest request) {
        AgentSyncStatus status = loadOrCreate(request.getServiceName(), request.getAppName());
        status.setEnvironment(request.getEnvironment());
        status.setAgentVersion(request.getAgentVersion());
        status.setLastSeenAt(LocalDateTime.now());
        status.setLastConfirmedConfigVersion(request.getConfigVersion());
        status.setLastConfigSyncAt(request.getLastConfigSyncAt());
        status.setLastConfigSyncStatus(request.getLastConfigSyncStatus());
        status.setLastConfigSyncError(request.getLastConfigSyncError());
        if ("SUCCESS".equalsIgnoreCase(request.getLastConfigSyncStatus())
                || "UP_TO_DATE".equalsIgnoreCase(request.getLastConfigSyncStatus())) {
            status.setLastSuccessfulConfigVersion(request.getConfigVersion());
        }
        repository.save(status);
    }

    @Transactional
    public void captureEventSync(String serviceName,
                                 String appName,
                                 String environment,
                                 String agentVersion,
                                 Long configVersion,
                                 LocalDateTime lastConfigSyncAt,
                                 String lastConfigSyncStatus,
                                 String lastConfigSyncError) {
        if (!StringUtils.hasText(serviceName) || !StringUtils.hasText(appName)) {
            return;
        }
        AgentSyncStatus status = loadOrCreate(serviceName, appName);
        status.setEnvironment(environment);
        status.setAgentVersion(agentVersion);
        status.setLastSeenAt(LocalDateTime.now());
        if (configVersion != null) {
            status.setLastConfirmedConfigVersion(configVersion);
        }
        if (lastConfigSyncAt != null) {
            status.setLastConfigSyncAt(lastConfigSyncAt);
        }
        if (StringUtils.hasText(lastConfigSyncStatus)) {
            status.setLastConfigSyncStatus(lastConfigSyncStatus);
            if ("SUCCESS".equalsIgnoreCase(lastConfigSyncStatus)
                    || "UP_TO_DATE".equalsIgnoreCase(lastConfigSyncStatus)) {
                status.setLastSuccessfulConfigVersion(configVersion);
            }
        }
        status.setLastConfigSyncError(lastConfigSyncError);
        repository.save(status);
    }

    @Transactional
    public void refreshTargetVersion(long targetConfigVersion) {
        repository.findAll().forEach(status -> status.setTargetConfigVersion(targetConfigVersion));
    }

    public List<AgentSyncStatusDto> listStatuses() {
        return repository.findAll().stream()
                .sorted((left, right) -> {
                    LocalDateTime l = left.getLastSeenAt();
                    LocalDateTime r = right.getLastSeenAt();
                    if (l == null && r == null) {
                        return left.getId().compareTo(right.getId());
                    }
                    if (l == null) {
                        return 1;
                    }
                    if (r == null) {
                        return -1;
                    }
                    return r.compareTo(l);
                })
                .map(this::toDto)
                .toList();
    }

    public long countEffective(long targetVersion) {
        return repository.findAll().stream()
                .filter(item -> targetVersion > 0 && targetVersion == safeLong(item.getLastSuccessfulConfigVersion()))
                .count();
    }

    public long countAgents() {
        return repository.count();
    }

    private AgentSyncStatus loadOrCreate(String serviceName, String appName) {
        String normalizedService = StringUtils.hasText(serviceName) ? serviceName.trim() : "unknown-service";
        String normalizedApp = StringUtils.hasText(appName) ? appName.trim() : "unknown-app";
        String id = normalizedApp + "::" + normalizedService;
        return repository.findById(id).orElseGet(() -> {
            AgentSyncStatus status = new AgentSyncStatus();
            status.setId(id);
            status.setServiceName(normalizedService);
            status.setAppName(normalizedApp);
            status.setTargetConfigVersion(0L);
            return status;
        });
    }

    private AgentSyncStatusDto toDto(AgentSyncStatus status) {
        long targetVersion = safeLong(status.getTargetConfigVersion());
        long successVersion = safeLong(status.getLastSuccessfulConfigVersion());
        return new AgentSyncStatusDto(
                status.getServiceName(),
                status.getAppName(),
                status.getEnvironment(),
                status.getAgentVersion(),
                status.getTargetConfigVersion(),
                status.getLastConfirmedConfigVersion(),
                status.getLastSuccessfulConfigVersion(),
                status.getLastPulledAt(),
                status.getLastSeenAt(),
                status.getLastConfigSyncAt(),
                status.getLastConfigSyncStatus(),
                status.getLastConfigSyncError(),
                targetVersion > 0 && targetVersion == successVersion
        );
    }

    private long safeLong(Long value) {
        return value == null ? 0L : value;
    }
}

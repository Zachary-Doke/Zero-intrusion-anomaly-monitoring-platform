package com.platform.analyze.service;

import com.platform.analyze.dto.RuleSettingsDto;
import com.platform.analyze.dto.AgentRuntimeConfigDto;
import com.platform.analyze.entity.RuleSettings;
import com.platform.analyze.repository.RuleSettingsRepository;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.Set;

@Service
public class RuleSettingsService {

    private static final String DEFAULT_ID = "default";

    private final RuleSettingsRepository repository;
    private final AgentSyncStatusService agentSyncStatusService;

    public RuleSettingsService(RuleSettingsRepository repository,
                               AgentSyncStatusService agentSyncStatusService) {
        this.repository = repository;
        this.agentSyncStatusService = agentSyncStatusService;
    }

    public RuleSettingsDto getSettings() {
        return toDto(loadOrCreate());
    }

    public RuleSettingsDto save(RuleSettingsDto dto) {
        RuleSettings settings = loadOrCreate();
        settings.setPackagePatterns(dto.getPackagePatterns());
        settings.setDeepSamplingEnabled(dto.getDeepSamplingEnabled());
        settings.setDepthLimit(dto.getDepthLimit());
        settings.setLengthLimit(dto.getLengthLimit());
        settings.setCollectionLimit(dto.getCollectionLimit());
        settings.setDefaultSampleRate(dto.getDefaultSampleRate());
        settings.setQueueCapacity(dto.getQueueCapacity());
        settings.setFlushIntervalMs(dto.getFlushIntervalMs());
        settings.setThresholdCount(dto.getThresholdCount());
        settings.setThresholdWindowMinutes(dto.getThresholdWindowMinutes());
        settings.setAlertRecipients(dto.getAlertRecipients());
        settings.setAiModel(dto.getAiModel());
        settings.setTraceKeys(dto.getTraceKeys());
        settings.setSensitiveFields(dto.getSensitiveFields());
        settings.setVersion(settings.getVersion() + 1);
        repository.save(settings);
        agentSyncStatusService.refreshTargetVersion(settings.getVersion());
        return toDto(settings);
    }

    public AgentRuntimeConfigDto getAgentRuntimeConfig(String serviceName, String appName) {
        RuleSettings settings = loadOrCreate();
        agentSyncStatusService.markPulled(serviceName, appName, settings.getVersion());
        return new AgentRuntimeConfigDto(
                serviceName,
                appName,
                settings.getPackagePatterns(),
                settings.getDepthLimit(),
                settings.getLengthLimit(),
                settings.getCollectionLimit(),
                settings.getDefaultSampleRate(),
                settings.getQueueCapacity(),
                settings.getFlushIntervalMs(),
                settings.getTraceKeys(),
                settings.getSensitiveFields(),
                settings.getDeepSamplingEnabled(),
                settings.getAiModel(),
                settings.getVersion()
        );
    }

    public RuleSettings currentSettings() {
        return loadOrCreate();
    }

    public Set<String> getSensitiveFieldNames() {
        LinkedHashSet<String> fields = new LinkedHashSet<>();
        fields.add("password");
        fields.add("token");
        fields.add("secret");
        String configured = loadOrCreate().getSensitiveFields();
        if (configured == null || configured.isBlank()) {
            return fields;
        }
        for (String item : configured.split(",")) {
            String trimmed = item.trim();
            if (!trimmed.isEmpty()) {
                fields.add(trimmed);
            }
        }
        return fields;
    }

    private RuleSettings loadOrCreate() {
        return repository.findById(DEFAULT_ID).orElseGet(() -> {
            RuleSettings settings = new RuleSettings();
            settings.setId(DEFAULT_ID);
            settings.setPackagePatterns("com.github.monitor.demo,com.platform");
            settings.setDeepSamplingEnabled(Boolean.TRUE);
            settings.setDepthLimit(8);
            settings.setLengthLimit(1024);
            settings.setCollectionLimit(50);
            settings.setDefaultSampleRate(1.0);
            settings.setQueueCapacity(10000);
            settings.setFlushIntervalMs(5000);
            settings.setThresholdCount(5);
            settings.setThresholdWindowMinutes(1);
            settings.setAlertRecipients("admin@example.com;platform@example.com");
            settings.setAiModel("deepseek-chat");
            settings.setTraceKeys("traceId,requestId");
            settings.setSensitiveFields("password,token,secret");
            settings.setVersion(1L);
            return repository.save(settings);
        });
    }

    private RuleSettingsDto toDto(RuleSettings settings) {
        RuleSettingsDto dto = new RuleSettingsDto();
        dto.setPackagePatterns(settings.getPackagePatterns());
        dto.setDeepSamplingEnabled(settings.getDeepSamplingEnabled());
        dto.setDepthLimit(settings.getDepthLimit());
        dto.setLengthLimit(settings.getLengthLimit());
        dto.setCollectionLimit(settings.getCollectionLimit());
        dto.setDefaultSampleRate(settings.getDefaultSampleRate());
        dto.setQueueCapacity(settings.getQueueCapacity());
        dto.setFlushIntervalMs(settings.getFlushIntervalMs());
        dto.setThresholdCount(settings.getThresholdCount());
        dto.setThresholdWindowMinutes(settings.getThresholdWindowMinutes());
        dto.setAlertRecipients(settings.getAlertRecipients());
        dto.setAiModel(settings.getAiModel());
        dto.setTraceKeys(settings.getTraceKeys());
        dto.setSensitiveFields(settings.getSensitiveFields());
        dto.setVersion(settings.getVersion());
        return dto;
    }
}

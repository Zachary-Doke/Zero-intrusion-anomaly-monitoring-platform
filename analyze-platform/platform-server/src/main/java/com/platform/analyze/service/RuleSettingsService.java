package com.platform.analyze.service;

import com.platform.analyze.dto.RuleSettingsDto;
import com.platform.analyze.dto.AgentRuntimeConfigDto;
import com.platform.analyze.config.DeepSeekProperties;
import com.platform.analyze.entity.RuleSettings;
import com.platform.analyze.repository.RuleSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.Set;

@Service
public class RuleSettingsService {

    private static final String DEFAULT_ID = "default";
    private static final String DEFAULT_SENSITIVE_FIELDS = "password,token,secret,sensitive";
    private static final String DEFAULT_PROMPT_TEMPLATE =
            "请分析以下 Java 异常，并输出 JSON。"
                    + "\n异常类型: {{exception_class}}"
                    + "\n服务: {{service_name}}"
                    + "\n摘要: {{summary}}"
                    + "\n栈顶方法: {{top_stack_frame}}"
                    + "\n方法签名: {{method_signature}}"
                    + "\n发生次数: {{occurrence_count}}"
                    + "\n首次发生: {{first_seen}}"
                    + "\n最近发生: {{last_seen}}"
                    + "\n请返回 rootCauseAnalysis、impactScope、troubleshootingSteps、fixSuggestion 四个字段。";

    private final RuleSettingsRepository repository;
    private final AgentSyncStatusService agentSyncStatusService;
    private final DeepSeekProperties deepSeekProperties;

    public RuleSettingsService(RuleSettingsRepository repository,
                               AgentSyncStatusService agentSyncStatusService,
                               DeepSeekProperties deepSeekProperties) {
        this.repository = repository;
        this.agentSyncStatusService = agentSyncStatusService;
        this.deepSeekProperties = deepSeekProperties;
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
        settings.setAiBaseUrl(dto.getAiBaseUrl());
        if (StringUtils.hasText(dto.getAiApiKey())) {
            settings.setAiApiKey(dto.getAiApiKey().trim());
        }
        settings.setAiModel(dto.getAiModel());
        settings.setAiPromptTemplate(dto.getAiPromptTemplate());
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
        fields.add("sensitive");
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
            settings.setAiBaseUrl(deepSeekProperties.getBaseUrl());
            settings.setAiApiKey(deepSeekProperties.getApiKey());
            settings.setAiModel(deepSeekProperties.getModel());
            settings.setAiPromptTemplate(DEFAULT_PROMPT_TEMPLATE);
            settings.setTraceKeys("traceId,requestId");
            settings.setSensitiveFields(DEFAULT_SENSITIVE_FIELDS);
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
        dto.setAiBaseUrl(settings.getAiBaseUrl());
        dto.setAiApiKey("");
        dto.setAiApiKeyConfigured(StringUtils.hasText(settings.getAiApiKey()));
        dto.setAiModel(settings.getAiModel());
        dto.setAiPromptTemplate(settings.getAiPromptTemplate());
        dto.setTraceKeys(settings.getTraceKeys());
        dto.setSensitiveFields(settings.getSensitiveFields());
        dto.setVersion(settings.getVersion());
        return dto;
    }
}

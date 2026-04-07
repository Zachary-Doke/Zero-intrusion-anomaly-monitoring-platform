package com.platform.analyze.service;

import com.platform.analyze.dto.AgentRuntimeConfirmRequest;
import com.platform.analyze.dto.RuleSettingsDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class RuleSettingsServiceTest {

    @Autowired
    private RuleSettingsService ruleSettingsService;

    @Autowired
    private AgentSyncStatusService agentSyncStatusService;

    @BeforeEach
    void setUp() {
        // RuleSettings is singleton-like and kept for subsequent assertions.
    }

    @Test
    void shouldTrackPublishedVersionAndAgentEffectiveStatus() {
        RuleSettingsDto current = ruleSettingsService.getSettings();
        long firstVersion = current.getVersion();
        ruleSettingsService.getAgentRuntimeConfig("payment-gateway", "demo-app");

        AgentRuntimeConfirmRequest confirm = new AgentRuntimeConfirmRequest();
        confirm.setServiceName("payment-gateway");
        confirm.setAppName("demo-app");
        confirm.setEnvironment("test");
        confirm.setAgentVersion("agent-test");
        confirm.setConfigVersion(firstVersion);
        confirm.setLastConfigSyncAt(LocalDateTime.now());
        confirm.setLastConfigSyncStatus("SUCCESS");
        agentSyncStatusService.confirm(confirm);

        RuleSettingsDto updated = new RuleSettingsDto();
        updated.setPackagePatterns(current.getPackagePatterns());
        updated.setDeepSamplingEnabled(current.getDeepSamplingEnabled());
        updated.setDepthLimit(current.getDepthLimit());
        updated.setLengthLimit(current.getLengthLimit());
        updated.setCollectionLimit(current.getCollectionLimit());
        updated.setDefaultSampleRate(current.getDefaultSampleRate());
        updated.setQueueCapacity(current.getQueueCapacity());
        updated.setFlushIntervalMs(current.getFlushIntervalMs());
        updated.setAiBaseUrl(current.getAiBaseUrl());
        updated.setAiApiKey(current.getAiApiKey());
        updated.setAiModel(current.getAiModel());
        updated.setAiPromptTemplate(current.getAiPromptTemplate() + "\n# adjusted");
        updated.setTraceKeys(current.getTraceKeys());
        updated.setSensitiveFields(current.getSensitiveFields());

        RuleSettingsDto saved = ruleSettingsService.save(updated);
        assertThat(saved.getVersion()).isGreaterThan(firstVersion);
        assertThat(agentSyncStatusService.countEffective(saved.getVersion())).isZero();

        confirm.setConfigVersion(saved.getVersion());
        agentSyncStatusService.confirm(confirm);

        assertThat(agentSyncStatusService.countEffective(saved.getVersion())).isEqualTo(1);
        assertThat(agentSyncStatusService.listStatuses()).hasSizeGreaterThanOrEqualTo(1);
        assertThat(agentSyncStatusService.listStatuses().get(0).isEffective()).isTrue();
    }

    @Test
    void shouldHideStoredAiApiKeyAndPreserveItOnBlankSave() {
        RuleSettingsDto current = ruleSettingsService.getSettings();

        RuleSettingsDto updated = copyOf(current);
        updated.setAiApiKey("secret-for-test");

        RuleSettingsDto saved = ruleSettingsService.save(updated);
        assertThat(saved.getAiApiKey()).isEmpty();
        assertThat(saved.getAiApiKeyConfigured()).isTrue();

        RuleSettingsDto blankUpdate = copyOf(saved);
        blankUpdate.setAiApiKey("");

        RuleSettingsDto savedAgain = ruleSettingsService.save(blankUpdate);
        assertThat(savedAgain.getAiApiKey()).isEmpty();
        assertThat(savedAgain.getAiApiKeyConfigured()).isTrue();
    }

    private RuleSettingsDto copyOf(RuleSettingsDto source) {
        RuleSettingsDto copy = new RuleSettingsDto();
        copy.setPackagePatterns(source.getPackagePatterns());
        copy.setDeepSamplingEnabled(source.getDeepSamplingEnabled());
        copy.setDepthLimit(source.getDepthLimit());
        copy.setLengthLimit(source.getLengthLimit());
        copy.setCollectionLimit(source.getCollectionLimit());
        copy.setDefaultSampleRate(source.getDefaultSampleRate());
        copy.setQueueCapacity(source.getQueueCapacity());
        copy.setFlushIntervalMs(source.getFlushIntervalMs());
        copy.setAiBaseUrl(source.getAiBaseUrl());
        copy.setAiApiKey(source.getAiApiKey());
        copy.setAiApiKeyConfigured(source.getAiApiKeyConfigured());
        copy.setAiModel(source.getAiModel());
        copy.setAiPromptTemplate(source.getAiPromptTemplate());
        copy.setTraceKeys(source.getTraceKeys());
        copy.setSensitiveFields(source.getSensitiveFields());
        copy.setVersion(source.getVersion());
        return copy;
    }
}

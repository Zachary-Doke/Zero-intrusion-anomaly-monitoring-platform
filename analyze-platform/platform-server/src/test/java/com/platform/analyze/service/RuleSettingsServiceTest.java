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
        updated.setThresholdCount(current.getThresholdCount() + 1);
        updated.setThresholdWindowMinutes(current.getThresholdWindowMinutes());
        updated.setAlertRecipients(current.getAlertRecipients());
        updated.setAiModel(current.getAiModel());
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
}

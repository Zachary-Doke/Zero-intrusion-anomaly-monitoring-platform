package com.platform.analyze.service;

import com.platform.analyze.dto.AgentSyncStatusDto;
import com.platform.analyze.entity.AgentSyncStatus;
import com.platform.analyze.repository.AgentSyncStatusRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentSyncStatusServiceTest {

    @Mock
    private AgentSyncStatusRepository repository;

    private AgentSyncStatusService service;

    @BeforeEach
    void setUp() {
        service = new AgentSyncStatusService(repository);
    }

    @Test
    void shouldNormalizeUnknownServiceToAppNameWhenCapturing() {
        when(repository.findById("demo-app::demo-app")).thenReturn(Optional.empty());
        when(repository.save(any(AgentSyncStatus.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.captureEventSync(
                "unknown-service",
                "demo-app",
                "dev",
                "agent-1.1.0",
                2L,
                LocalDateTime.now(),
                "SUCCESS",
                null
        );

        ArgumentCaptor<AgentSyncStatus> captor = ArgumentCaptor.forClass(AgentSyncStatus.class);
        verify(repository).save(captor.capture());
        AgentSyncStatus saved = captor.getValue();
        assertThat(saved.getId()).isEqualTo("demo-app::demo-app");
        assertThat(saved.getAppName()).isEqualTo("demo-app");
        assertThat(saved.getServiceName()).isEqualTo("demo-app");
    }

    @Test
    void shouldHideLegacyUnknownServiceWhenKnownServiceExistsForSameApp() {
        AgentSyncStatus unknown = new AgentSyncStatus();
        unknown.setId("demo-app::unknown-service");
        unknown.setAppName("demo-app");
        unknown.setServiceName("unknown-service");
        unknown.setTargetConfigVersion(3L);
        unknown.setLastSuccessfulConfigVersion(3L);
        unknown.setLastSeenAt(LocalDateTime.now().minusMinutes(1));

        AgentSyncStatus known = new AgentSyncStatus();
        known.setId("demo-app::demo");
        known.setAppName("demo-app");
        known.setServiceName("demo");
        known.setTargetConfigVersion(3L);
        known.setLastSuccessfulConfigVersion(3L);
        known.setLastSeenAt(LocalDateTime.now());

        when(repository.findAll()).thenReturn(List.of(unknown, known));

        List<AgentSyncStatusDto> statuses = service.listStatuses();

        assertThat(statuses).hasSize(1);
        assertThat(statuses.get(0).getServiceName()).isEqualTo("demo");
        assertThat(service.countAgents()).isEqualTo(1);
        assertThat(service.countEffective(3L)).isEqualTo(1);
    }
}

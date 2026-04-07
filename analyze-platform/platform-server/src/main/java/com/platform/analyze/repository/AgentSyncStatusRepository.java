package com.platform.analyze.repository;

import com.platform.analyze.entity.AgentSyncStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AgentSyncStatusRepository extends JpaRepository<AgentSyncStatus, String> {
}

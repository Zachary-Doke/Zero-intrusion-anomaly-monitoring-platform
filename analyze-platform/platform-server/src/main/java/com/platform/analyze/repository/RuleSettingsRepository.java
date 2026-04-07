package com.platform.analyze.repository;

import com.platform.analyze.entity.RuleSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RuleSettingsRepository extends JpaRepository<RuleSettings, String> {
}

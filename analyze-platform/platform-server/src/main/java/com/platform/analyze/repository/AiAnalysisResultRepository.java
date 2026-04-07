package com.platform.analyze.repository;

import com.platform.analyze.entity.AiAnalysisResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AiAnalysisResultRepository extends JpaRepository<AiAnalysisResult, Long> {
    Optional<AiAnalysisResult> findByFingerprint(String fingerprint);
}

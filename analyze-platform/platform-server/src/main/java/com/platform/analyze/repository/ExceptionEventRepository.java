package com.platform.analyze.repository;

import com.platform.analyze.entity.ExceptionEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ExceptionEventRepository extends JpaRepository<ExceptionEvent, Long>, JpaSpecificationExecutor<ExceptionEvent> {
    List<ExceptionEvent> findByOccurrenceTimeGreaterThanEqualOrderByOccurrenceTimeAsc(LocalDateTime startDate);

    long countByStatus(String status);

    long countBySeverityAndStatus(String severity, String status);

    long countByFingerprintAndOccurrenceTimeGreaterThanEqual(String fingerprint, LocalDateTime startDate);
}

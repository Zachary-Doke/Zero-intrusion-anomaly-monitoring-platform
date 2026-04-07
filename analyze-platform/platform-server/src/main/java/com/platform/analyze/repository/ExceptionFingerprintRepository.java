package com.platform.analyze.repository;

import com.platform.analyze.entity.ExceptionFingerprint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExceptionFingerprintRepository extends JpaRepository<ExceptionFingerprint, String> {

    List<ExceptionFingerprint> findTop8ByOrderByOccurrenceCountDescLastSeenDesc();
}

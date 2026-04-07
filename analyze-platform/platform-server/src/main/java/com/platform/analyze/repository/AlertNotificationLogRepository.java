package com.platform.analyze.repository;

import com.platform.analyze.entity.AlertNotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertNotificationLogRepository extends JpaRepository<AlertNotificationLog, Long> {
    List<AlertNotificationLog> findAllByOrderByTriggeredAtDesc();
}

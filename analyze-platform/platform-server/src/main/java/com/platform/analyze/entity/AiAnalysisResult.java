package com.platform.analyze.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * AI 分析结果实体类
 */
@Data
@Entity
@Table(name = "ai_analysis_result")
public class AiAnalysisResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64, unique = true)
    private String fingerprint;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String reportStatus;

    @Column(nullable = false)
    private String modelName;

    @Column(length = 1024)
    private String summary;

    @Column(columnDefinition = "TEXT")
    private String analysisContent;

    @Column(nullable = false)
    private String triggerSource;

    @Column(nullable = false)
    private LocalDateTime requestedAt;

    @Column
    private LocalDateTime startedAt;

    @Column(length = 1024)
    private String errorMessage;

    @Column(nullable = false)
    private LocalDateTime analysisTime;
}

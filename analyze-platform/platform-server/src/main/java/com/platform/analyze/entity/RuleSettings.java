package com.platform.analyze.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "rule_settings")
public class RuleSettings {

    @Id
    private String id;

    @Column(nullable = false)
    private String packagePatterns;

    @Column(nullable = false)
    private Boolean deepSamplingEnabled;

    @Column(nullable = false)
    private Integer depthLimit;

    @Column(nullable = false)
    private Integer lengthLimit;

    @Column(nullable = false)
    private Integer collectionLimit;

    @Column(nullable = false)
    private Double defaultSampleRate;

    @Column(nullable = false)
    private Integer queueCapacity;

    @Column(nullable = false)
    private Integer flushIntervalMs;

    @Column(nullable = false)
    private String aiBaseUrl;

    @Column(nullable = false, length = 2048)
    private String aiApiKey;

    @Column(nullable = false)
    private String aiModel;

    @Column(nullable = false, length = 4096)
    private String aiPromptTemplate;

    @Column(nullable = false, length = 1024)
    private String traceKeys;

    @Column(nullable = false, length = 1024)
    private String sensitiveFields;

    @Column(nullable = false)
    private Long version;
}

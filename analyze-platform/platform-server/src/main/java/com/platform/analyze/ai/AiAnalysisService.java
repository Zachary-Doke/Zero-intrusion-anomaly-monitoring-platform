package com.platform.analyze.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.analyze.config.DeepSeekProperties;
import com.platform.analyze.dto.ExceptionListItemDto;
import com.platform.analyze.dto.ExceptionSuggestionDto;
import com.platform.analyze.dto.ExceptionTrendDto;
import com.platform.analyze.dto.OverviewMetricDto;
import com.platform.analyze.dto.RiskSummaryDto;
import com.platform.analyze.entity.AiAnalysisResult;
import com.platform.analyze.entity.ExceptionFingerprint;
import com.platform.analyze.entity.RuleSettings;
import com.platform.analyze.repository.AiAnalysisResultRepository;
import com.platform.analyze.repository.ExceptionFingerprintRepository;
import com.platform.analyze.service.RuleSettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class AiAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(AiAnalysisService.class);
    private static final DateTimeFormatter SUMMARY_TIME_FORMATTER = DateTimeFormatter.ofPattern("MM-dd HH:mm");
    private static final long RISK_SUMMARY_TIMEOUT_MS = 6000L;

    private final DeepSeekClient deepSeekClient;
    private final DeepSeekProperties deepSeekProperties;
    private final RuleSettingsService ruleSettingsService;
    private final AiAnalysisResultRepository resultRepository;
    private final ExceptionFingerprintRepository fingerprintRepository;
    private final ObjectMapper objectMapper;

    public AiAnalysisService(DeepSeekClient deepSeekClient,
                             DeepSeekProperties deepSeekProperties,
                             RuleSettingsService ruleSettingsService,
                             AiAnalysisResultRepository resultRepository,
                             ExceptionFingerprintRepository fingerprintRepository,
                             ObjectMapper objectMapper) {
        this.deepSeekClient = deepSeekClient;
        this.deepSeekProperties = deepSeekProperties;
        this.ruleSettingsService = ruleSettingsService;
        this.resultRepository = resultRepository;
        this.fingerprintRepository = fingerprintRepository;
        this.objectMapper = objectMapper;
    }

    public ExceptionSuggestionDto getSuggestionSnapshot(String fingerprintId) {
        ExceptionFingerprint fingerprint = loadFingerprint(fingerprintId);
        return resultRepository.findByFingerprint(fingerprintId)
                .map(entity -> toSnapshot(entity, fingerprint))
                .orElseGet(() -> buildRootCauseOnly(fingerprint, "READY", null));
    }

    public ExceptionSuggestionDto generateSuggestion(String fingerprintId) {
        ExceptionFingerprint fingerprint = loadFingerprint(fingerprintId);
        Optional<AiAnalysisResult> existing = resultRepository.findByFingerprint(fingerprintId);
        if (existing.isPresent() && "COMPLETED".equalsIgnoreCase(existing.get().getReportStatus())) {
            return toSnapshot(existing.get(), fingerprint);
        }

        RuleSettings settings = ruleSettingsService.currentSettings();
        AiAnalysisResult entity = existing.orElseGet(AiAnalysisResult::new);
        LocalDateTime now = LocalDateTime.now();
        entity.setFingerprint(fingerprintId);
        entity.setTitle(fingerprint.getServiceName() + " / " + fingerprint.getExceptionClass() + " 处理建议");
        entity.setTriggerSource("DETAIL_ACTION");
        entity.setRequestedAt(now);
        entity.setStartedAt(now);
        entity.setReportStatus("RUNNING");
        entity.setModelName(settings.getAiModel());
        entity.setSummary(null);
        entity.setErrorMessage(null);
        entity.setAnalysisTime(now);
        resultRepository.save(entity);

        try {
            String content;
            String modelName;
            try {
                content = stripMarkdown(deepSeekClient.chatCompletion(
                        buildPrompt(settings, fingerprint),
                        chooseValue(settings.getAiApiKey(), deepSeekProperties.getApiKey()),
                        chooseValue(settings.getAiBaseUrl(), deepSeekProperties.getBaseUrl()),
                        chooseValue(settings.getAiModel(), deepSeekProperties.getModel()),
                        deepSeekProperties.getTimeout()
                ));
                modelName = chooseValue(settings.getAiModel(), deepSeekProperties.getModel());
            } catch (Exception ex) {
                log.warn("AI 外部调用失败，降级为本地启发式建议: {}", ex.getMessage());
                content = writeJson(buildHeuristicSuggestion(fingerprint));
                modelName = "heuristic-local";
            }

            ExceptionSuggestionDto suggestion = parseJson(content);
            normalizeSuggestion(suggestion, fingerprint, "COMPLETED", LocalDateTime.now());

            entity.setReportStatus("COMPLETED");
            entity.setModelName(modelName);
            entity.setSummary(suggestion.getRootCauseAnalysis());
            entity.setAnalysisContent(writeJson(suggestion));
            entity.setAnalysisTime(suggestion.getSuggestionUpdatedAt());
            entity.setErrorMessage(null);
            resultRepository.save(entity);
            return suggestion;
        } catch (RuntimeException ex) {
            entity.setReportStatus("FAILED");
            entity.setErrorMessage(ex.getMessage());
            entity.setAnalysisTime(LocalDateTime.now());
            resultRepository.save(entity);
            return buildRootCauseOnly(fingerprint, "FAILED", LocalDateTime.now());
        }
    }

    public RiskSummaryDto generateDailyRiskSummary(OverviewMetricDto metrics,
                                                   List<ExceptionTrendDto> trends,
                                                   List<ExceptionFingerprint> topFingerprints,
                                                   List<ExceptionListItemDto> recentEvents) {
        LocalDateTime now = LocalDateTime.now();
        RiskFacts facts = buildRiskFacts(metrics, trends, topFingerprints, recentEvents);
        RuleSettings settings = ruleSettingsService.currentSettings();
        String apiKey = chooseValue(settings.getAiApiKey(), deepSeekProperties.getApiKey());
        String baseUrl = chooseValue(settings.getAiBaseUrl(), deepSeekProperties.getBaseUrl());
        String model = chooseValue(settings.getAiModel(), deepSeekProperties.getModel());

        if (!StringUtils.hasText(apiKey) || !StringUtils.hasText(baseUrl) || !StringUtils.hasText(model)) {
            return buildRuleRiskSummary(facts, now);
        }

        try {
            String content = stripMarkdown(deepSeekClient.chatCompletion(
                    buildRiskSummaryPrompt(facts),
                    apiKey,
                    baseUrl,
                    model,
                    Math.min(deepSeekProperties.getTimeout(), RISK_SUMMARY_TIMEOUT_MS)
            ));
            RiskSummaryDto aiSummary = parseRiskSummaryJson(content);
            return normalizeRiskSummary(aiSummary, facts, now, "AI");
        } catch (Exception ex) {
            log.warn("生成本日风险摘要失败，降级为规则摘要: {}", ex.getMessage());
            return buildRuleRiskSummary(facts, now);
        }
    }

    public RiskSummaryDto generateRuleDailyRiskSummary(OverviewMetricDto metrics,
                                                       List<ExceptionTrendDto> trends,
                                                       List<ExceptionFingerprint> topFingerprints,
                                                       List<ExceptionListItemDto> recentEvents) {
        LocalDateTime now = LocalDateTime.now();
        RiskFacts facts = buildRiskFacts(metrics, trends, topFingerprints, recentEvents);
        return buildRuleRiskSummary(facts, now);
    }

    private ExceptionFingerprint loadFingerprint(String fingerprintId) {
        return fingerprintRepository.findById(fingerprintId)
                .orElseThrow(() -> new RuntimeException("找不到指定的异常指纹: " + fingerprintId));
    }

    private RiskFacts buildRiskFacts(OverviewMetricDto metrics,
                                     List<ExceptionTrendDto> trends,
                                     List<ExceptionFingerprint> topFingerprints,
                                     List<ExceptionListItemDto> recentEvents) {
        long totalCount = metrics == null ? 0L : metrics.getTotalExceptions();
        long openCount = metrics == null ? 0L : metrics.getOpenExceptionCount();
        long criticalOpenCount = metrics == null ? 0L : metrics.getCriticalExceptionCount();
        double openRate = totalCount <= 0 ? 0D : (openCount * 100D / totalCount);

        long todayCount = extractTrendCount(trends, -1);
        long yesterdayCount = extractTrendCount(trends, -2);
        String trendDirection = resolveTrendDirection(todayCount, yesterdayCount);
        String riskLevel = resolveRiskLevel(openRate, criticalOpenCount, todayCount, yesterdayCount);

        ExceptionFingerprint topFingerprint = firstItem(topFingerprints);
        String hottestService = topFingerprint == null ? "" : safeValue(topFingerprint.getServiceName());
        String hottestFingerprint = topFingerprint == null
                ? ""
                : chooseValue(topFingerprint.getSummary(), topFingerprint.getExceptionClass());
        long hottestFingerprintCount = topFingerprint == null || topFingerprint.getOccurrenceCount() == null
                ? 0L
                : topFingerprint.getOccurrenceCount();

        ExceptionListItemDto latest = firstItem(recentEvents);
        String latestOccurrence = latest == null || latest.getOccurrenceTime() == null
                ? "暂无"
                : latest.getOccurrenceTime().format(SUMMARY_TIME_FORMATTER);

        String summary = String.format(
                Locale.ROOT,
                "未处理异常 %d 条（占比 %.0f%%），严重未处理 %d 条，整体风险%s。",
                openCount,
                openRate,
                criticalOpenCount,
                riskLevelLabel(riskLevel)
        );

        List<String> highlights = new ArrayList<>();
        highlights.add(String.format(Locale.ROOT, "近两日趋势：今日 %d，昨日 %d（%s）", todayCount, yesterdayCount, trendDirection));
        if (StringUtils.hasText(hottestFingerprint)) {
            String service = StringUtils.hasText(hottestService) ? hottestService : "未知服务";
            highlights.add(String.format(Locale.ROOT, "高频风险点：%s / %s（%d 次）", service, hottestFingerprint, hottestFingerprintCount));
        } else {
            highlights.add("高频风险点：暂无异常簇数据");
        }
        highlights.add("最近异常时间：" + latestOccurrence);

        return new RiskFacts(
                riskLevel,
                summary,
                highlights,
                openCount,
                criticalOpenCount,
                openRate,
                todayCount,
                yesterdayCount,
                hottestService,
                hottestFingerprint,
                hottestFingerprintCount,
                latestOccurrence
        );
    }

    private RiskSummaryDto buildRuleRiskSummary(RiskFacts facts, LocalDateTime now) {
        return new RiskSummaryDto(
                facts.riskLevel(),
                facts.summary(),
                facts.highlights(),
                "RULE",
                now
        );
    }

    private RiskSummaryDto normalizeRiskSummary(RiskSummaryDto aiSummary,
                                                RiskFacts facts,
                                                LocalDateTime now,
                                                String source) {
        RiskSummaryDto normalized = new RiskSummaryDto();
        normalized.setRiskLevel(normalizeRiskLevel(facts.riskLevel()));
        normalized.setSummary(StringUtils.hasText(aiSummary == null ? null : aiSummary.getSummary())
                ? aiSummary.getSummary().trim()
                : facts.summary());
        List<String> highlights = sanitizeHighlights(aiSummary == null ? null : aiSummary.getHighlights());
        normalized.setHighlights(highlights.isEmpty() ? facts.highlights() : highlights);
        normalized.setSource(source);
        normalized.setUpdatedAt(now);
        return normalized;
    }

    private String buildRiskSummaryPrompt(RiskFacts facts) {
        return "你是异常监控态势分析助手，请输出总览页“本日风险摘要”。"
                + "\n必须只返回 JSON，且字段仅允许 riskLevel、summary、highlights。"
                + "\n约束："
                + "\n1) riskLevel 只能是 HIGH、MEDIUM、LOW，且必须保持为 " + facts.riskLevel()
                + "\n2) summary 必须为一句中文，禁止给出处置建议，长度不超过 60 字"
                + "\n3) highlights 必须 2-3 条，每条不超过 36 字"
                + "\n4) 不允许编造数字，不允许新增事实"
                + "\n事实："
                + "\n- 未处理异常: " + facts.openCount()
                + "\n- 严重未处理异常: " + facts.criticalOpenCount()
                + "\n- 未处理占比: " + String.format(Locale.ROOT, "%.0f%%", facts.openRate())
                + "\n- 今日异常数: " + facts.todayCount()
                + "\n- 昨日异常数: " + facts.yesterdayCount()
                + "\n- 高频风险服务: " + safeValue(facts.hottestService())
                + "\n- 高频风险异常: " + safeValue(facts.hottestFingerprint())
                + "\n- 高频异常出现次数: " + facts.hottestFingerprintCount()
                + "\n- 最近异常时间: " + facts.latestOccurrence();
    }

    private RiskSummaryDto parseRiskSummaryJson(String json) {
        try {
            return objectMapper.readValue(json, RiskSummaryDto.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("风险摘要 AI 返回格式无法解析", e);
        }
    }

    private List<String> sanitizeHighlights(List<String> highlights) {
        if (highlights == null || highlights.isEmpty()) {
            return List.of();
        }
        List<String> sanitized = new ArrayList<>();
        for (String item : highlights) {
            if (!StringUtils.hasText(item)) {
                continue;
            }
            sanitized.add(item.trim());
            if (sanitized.size() >= 3) {
                break;
            }
        }
        return sanitized;
    }

    private String resolveRiskLevel(double openRate,
                                    long criticalOpenCount,
                                    long todayCount,
                                    long yesterdayCount) {
        if (criticalOpenCount >= 5 || openRate >= 65D || (todayCount >= 8 && todayCount - yesterdayCount >= 5)) {
            return "HIGH";
        }
        if (criticalOpenCount >= 1 || openRate >= 35D || todayCount > yesterdayCount) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private String resolveTrendDirection(long todayCount, long yesterdayCount) {
        if (todayCount > yesterdayCount) {
            return "上升";
        }
        if (todayCount < yesterdayCount) {
            return "回落";
        }
        return "持平";
    }

    private String riskLevelLabel(String riskLevel) {
        return switch (normalizeRiskLevel(riskLevel)) {
            case "HIGH" -> "偏高";
            case "MEDIUM" -> "中等";
            default -> "可控";
        };
    }

    private String normalizeRiskLevel(String riskLevel) {
        String value = safeValue(riskLevel).trim().toUpperCase(Locale.ROOT);
        return switch (value) {
            case "HIGH", "MEDIUM", "LOW" -> value;
            case "CRITICAL" -> "HIGH";
            default -> "MEDIUM";
        };
    }

    private long extractTrendCount(List<ExceptionTrendDto> trends, int offsetFromEnd) {
        if (trends == null || trends.isEmpty()) {
            return 0L;
        }
        int index = trends.size() + offsetFromEnd;
        if (index < 0 || index >= trends.size()) {
            return 0L;
        }
        ExceptionTrendDto item = trends.get(index);
        return item == null || item.getCount() == null ? 0L : item.getCount();
    }

    private <T> T firstItem(List<T> items) {
        return (items == null || items.isEmpty()) ? null : items.get(0);
    }

    private ExceptionSuggestionDto toSnapshot(AiAnalysisResult entity, ExceptionFingerprint fingerprint) {
        if ("COMPLETED".equalsIgnoreCase(entity.getReportStatus()) && StringUtils.hasText(entity.getAnalysisContent())) {
            ExceptionSuggestionDto suggestion = parseJson(entity.getAnalysisContent());
            normalizeSuggestion(suggestion, fingerprint, "COMPLETED", entity.getAnalysisTime());
            return suggestion;
        }
        return buildRootCauseOnly(fingerprint, entity.getReportStatus(), entity.getAnalysisTime());
    }

    private ExceptionSuggestionDto buildRootCauseOnly(ExceptionFingerprint fingerprint,
                                                      String status,
                                                      LocalDateTime updatedAt) {
        ExceptionSuggestionDto dto = new ExceptionSuggestionDto();
        dto.setRootCauseAnalysis("异常集中出现在 " + fingerprint.getServiceName()
                + " 的 " + fingerprint.getTopStackFrame()
                + "，初步判断为该代码路径缺少边界校验，或其依赖链路存在不稳定响应。");
        dto.setImpactScope("该指纹已累计出现 " + fingerprint.getOccurrenceCount()
                + " 次，持续影响 " + fingerprint.getServiceName() + " 的相关调用。");
        dto.setTroubleshootingSteps(null);
        dto.setFixSuggestion(null);
        dto.setSuggestionStatus(status);
        dto.setSuggestionUpdatedAt(updatedAt);
        return dto;
    }

    private ExceptionSuggestionDto buildHeuristicSuggestion(ExceptionFingerprint fingerprint) {
        return new ExceptionSuggestionDto(
                "异常集中出现在 " + fingerprint.getServiceName()
                        + " 的 " + fingerprint.getTopStackFrame()
                        + "，高概率是边界输入未校验、空值/状态不一致，或下游依赖超时导致。",
                "该异常已出现 " + fingerprint.getOccurrenceCount()
                        + " 次，影响范围至少覆盖 " + fingerprint.getServiceName() + " 当前调用链。",
                List.of(
                        "复现 " + fingerprint.getTopStackFrame() + " 对应调用，核对入参与上下文状态",
                        "检查最近一次发布、配置变更和依赖服务响应耗时",
                        "串联 traceId 与错误日志，确认是否存在固定触发条件"
                ),
                "在 " + fingerprint.getMethodSignature()
                        + " 增加参数校验、空值兜底和异常降级策略，并为关键依赖补充超时与重试边界。",
                "COMPLETED",
                LocalDateTime.now()
        );
    }

    private String buildPrompt(RuleSettings settings, ExceptionFingerprint fingerprint) {
        String template = chooseValue(settings.getAiPromptTemplate(), "");
        if (!StringUtils.hasText(template)) {
            template = "请分析异常并输出 JSON。异常类型: {{exception_class}}，服务: {{service_name}}，"
                    + "摘要: {{summary}}，栈顶方法: {{top_stack_frame}}，方法签名: {{method_signature}}，"
                    + "发生次数: {{occurrence_count}}，首次发生: {{first_seen}}，最近发生: {{last_seen}}。"
                    + "请返回 rootCauseAnalysis、impactScope、troubleshootingSteps、fixSuggestion 四个字段。";
        }
        return template
                .replace("{{exception_class}}", safeValue(fingerprint.getExceptionClass()))
                .replace("{{service_name}}", safeValue(fingerprint.getServiceName()))
                .replace("{{summary}}", safeValue(fingerprint.getSummary()))
                .replace("{{top_stack_frame}}", safeValue(fingerprint.getTopStackFrame()))
                .replace("{{method_signature}}", safeValue(fingerprint.getMethodSignature()))
                .replace("{{occurrence_count}}", String.valueOf(fingerprint.getOccurrenceCount()))
                .replace("{{first_seen}}", String.valueOf(fingerprint.getFirstSeen()))
                .replace("{{last_seen}}", String.valueOf(fingerprint.getLastSeen()));
    }

    private void normalizeSuggestion(ExceptionSuggestionDto suggestion,
                                     ExceptionFingerprint fingerprint,
                                     String defaultStatus,
                                     LocalDateTime defaultUpdatedAt) {
        if (!StringUtils.hasText(suggestion.getRootCauseAnalysis())) {
            suggestion.setRootCauseAnalysis(buildRootCauseOnly(fingerprint, defaultStatus, defaultUpdatedAt).getRootCauseAnalysis());
        }
        if (!StringUtils.hasText(suggestion.getImpactScope())) {
            suggestion.setImpactScope("该异常已影响 " + fingerprint.getServiceName() + " 相关调用链。");
        }
        if (!StringUtils.hasText(suggestion.getSuggestionStatus())) {
            suggestion.setSuggestionStatus(defaultStatus);
        }
        if (suggestion.getSuggestionUpdatedAt() == null) {
            suggestion.setSuggestionUpdatedAt(defaultUpdatedAt);
        }
    }

    private ExceptionSuggestionDto parseJson(String json) {
        try {
            return objectMapper.readValue(json, ExceptionSuggestionDto.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("AI 返回格式无法解析", e);
        }
    }

    private String writeJson(ExceptionSuggestionDto dto) {
        try {
            return objectMapper.writeValueAsString(dto);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("AI 结果序列化失败", e);
        }
    }

    private String stripMarkdown(String content) {
        String value = content == null ? "" : content.trim();
        if (value.startsWith("```json")) {
            value = value.substring(7);
        } else if (value.startsWith("```")) {
            value = value.substring(3);
        }
        if (value.endsWith("```")) {
            value = value.substring(0, value.length() - 3);
        }
        return value.trim();
    }

    private String chooseValue(String primary, String fallback) {
        return StringUtils.hasText(primary) ? primary : fallback;
    }

    private String safeValue(String value) {
        return value == null ? "" : value;
    }

    private record RiskFacts(String riskLevel,
                             String summary,
                             List<String> highlights,
                             long openCount,
                             long criticalOpenCount,
                             double openRate,
                             long todayCount,
                             long yesterdayCount,
                             String hottestService,
                             String hottestFingerprint,
                             long hottestFingerprintCount,
                             String latestOccurrence) {
    }
}

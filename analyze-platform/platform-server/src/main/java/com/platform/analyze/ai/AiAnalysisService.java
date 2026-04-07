package com.platform.analyze.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.analyze.config.DeepSeekProperties;
import com.platform.analyze.dto.ExceptionSuggestionDto;
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
import java.util.List;
import java.util.Optional;

@Service
public class AiAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(AiAnalysisService.class);

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

    private ExceptionFingerprint loadFingerprint(String fingerprintId) {
        return fingerprintRepository.findById(fingerprintId)
                .orElseThrow(() -> new RuntimeException("找不到指定的异常指纹: " + fingerprintId));
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
}

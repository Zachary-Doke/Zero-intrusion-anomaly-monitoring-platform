package com.platform.analyze.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.analyze.dto.AiAnalysisResultDto;
import com.platform.analyze.dto.AiReportSummaryDto;
import com.platform.analyze.entity.AiAnalysisResult;
import com.platform.analyze.entity.ExceptionFingerprint;
import com.platform.analyze.repository.AiAnalysisResultRepository;
import com.platform.analyze.repository.ExceptionFingerprintRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class AiAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(AiAnalysisService.class);

    private final DeepSeekClient deepSeekClient;
    private final AiAnalysisResultRepository resultRepository;
    private final ExceptionFingerprintRepository fingerprintRepository;
    private final ObjectMapper objectMapper;

    public AiAnalysisService(DeepSeekClient deepSeekClient,
                             AiAnalysisResultRepository resultRepository,
                             ExceptionFingerprintRepository fingerprintRepository,
                             ObjectMapper objectMapper) {
        this.deepSeekClient = deepSeekClient;
        this.resultRepository = resultRepository;
        this.fingerprintRepository = fingerprintRepository;
        this.objectMapper = objectMapper;
    }

    public AiAnalysisResultDto analyze(String fingerprintId) {
        return analyzeInternal(fingerprintId, "MANUAL", false);
    }

    public AiAnalysisResultDto analyzeIfAbsent(String fingerprintId, String triggerSource) {
        return analyzeInternal(fingerprintId, triggerSource, true);
    }

    private AiAnalysisResultDto analyzeInternal(String fingerprintId, String triggerSource, boolean reuseExisting) {
        Optional<AiAnalysisResult> cachedResult = resultRepository.findByFingerprint(fingerprintId);
        if (cachedResult.isPresent()) {
            AiAnalysisResult entity = cachedResult.get();
            if (reuseExisting || "RUNNING".equalsIgnoreCase(entity.getReportStatus())) {
                return buildResultDto(entity);
            }
        }

        ExceptionFingerprint fingerprint = fingerprintRepository.findById(fingerprintId)
                .orElseThrow(() -> new RuntimeException("找不到指定的异常指纹: " + fingerprintId));
        LocalDateTime requestedAt = LocalDateTime.now();
        AiAnalysisResult entity = cachedResult.orElseGet(AiAnalysisResult::new);
        entity.setFingerprint(fingerprintId);
        entity.setTitle(buildTitle(fingerprint));
        entity.setReportStatus("PENDING");
        entity.setModelName(deepSeekClient.currentModelName());
        entity.setSummary(null);
        entity.setAnalysisContent(null);
        entity.setTriggerSource(triggerSource);
        entity.setRequestedAt(requestedAt);
        entity.setStartedAt(null);
        entity.setErrorMessage(null);
        entity.setAnalysisTime(requestedAt);
        resultRepository.save(entity);

        LocalDateTime startedAt = LocalDateTime.now();
        entity.setReportStatus("RUNNING");
        entity.setStartedAt(startedAt);
        resultRepository.save(entity);

        try {
            String content;
            String modelName;
            try {
                content = stripMarkdown(deepSeekClient.chatCompletion(buildPrompt(fingerprint)));
                modelName = deepSeekClient.currentModelName();
            } catch (Exception ex) {
                log.warn("AI 外部调用失败，降级为本地启发式分析: {}", ex.getMessage());
                content = buildHeuristicJson(fingerprint);
                modelName = "heuristic-local";
            }

            AiAnalysisResultDto dto = parseJson(content);
            entity.setTitle(buildTitle(fingerprint));
            entity.setReportStatus("COMPLETED");
            entity.setModelName(modelName);
            entity.setSummary(dto.getProbableRootCause());
            entity.setAnalysisContent(content);
            entity.setAnalysisTime(LocalDateTime.now());
            entity.setTriggerSource(triggerSource);
            entity.setErrorMessage(null);
            resultRepository.save(entity);
            return dto;
        } catch (Exception ex) {
            entity.setReportStatus("FAILED");
            entity.setAnalysisTime(LocalDateTime.now());
            entity.setErrorMessage(ex.getMessage());
            resultRepository.save(entity);
            throw ex;
        }
    }

    public List<AiReportSummaryDto> getReports(String keyword, String status) {
        return resultRepository.findAll().stream()
                .sorted(Comparator.comparing(AiAnalysisResult::getRequestedAt).reversed())
                .filter(item -> !StringUtils.hasText(status) || item.getReportStatus().equalsIgnoreCase(status))
                .filter(item -> matchKeyword(item, keyword))
                .map(this::toSummaryDto)
                .toList();
    }

    public long averageAnalysisMinutes() {
        List<AiAnalysisResult> items = resultRepository.findAll();
        if (items.isEmpty()) {
            return 0L;
        }
        return Math.max(1L, Math.round(items.stream()
                .filter(item -> "COMPLETED".equalsIgnoreCase(item.getReportStatus()))
                .filter(item -> item.getStartedAt() != null && item.getAnalysisTime() != null)
                .map(item -> Duration.between(item.getStartedAt(), item.getAnalysisTime()).toMinutes())
                .mapToLong(Long::longValue)
                .average()
                .orElse(8D)));
    }

    private boolean matchKeyword(AiAnalysisResult item, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return true;
        }
        String pattern = keyword.trim().toLowerCase(Locale.ROOT);
        return item.getTitle().toLowerCase(Locale.ROOT).contains(pattern)
                || item.getFingerprint().toLowerCase(Locale.ROOT).contains(pattern)
                || contains(item.getSummary(), pattern)
                || contains(item.getErrorMessage(), pattern);
    }

    private String buildPrompt(ExceptionFingerprint fingerprint) {
        return String.format(
                "你是异常分析平台主管工程师，请根据以下数据输出 JSON。\n" +
                        "异常类型: %s\n" +
                        "服务: %s\n" +
                        "摘要: %s\n" +
                        "栈顶方法: %s\n" +
                        "方法签名: %s\n" +
                        "发生次数: %d\n" +
                        "首次发生: %s\n" +
                        "最近发生: %s\n" +
                        "请返回 probableRootCause、impactScope、troubleshootingSteps、fixSuggestion 四个字段。",
                fingerprint.getExceptionClass(),
                fingerprint.getServiceName(),
                fingerprint.getSummary(),
                fingerprint.getTopStackFrame(),
                fingerprint.getMethodSignature(),
                fingerprint.getOccurrenceCount(),
                fingerprint.getFirstSeen(),
                fingerprint.getLastSeen()
        );
    }

    private String buildHeuristicJson(ExceptionFingerprint fingerprint) {
        String probableRootCause = "异常集中出现在 " + fingerprint.getServiceName()
                + " 的 " + fingerprint.getTopStackFrame() + "，初步判断为代码路径缺少边界校验或下游依赖不稳定。";
        String impactScope = "该指纹累计出现 " + fingerprint.getOccurrenceCount()
                + " 次，影响范围至少覆盖 " + fingerprint.getServiceName() + " 的调用链。";
        List<String> steps = List.of(
                "优先复现 " + fingerprint.getTopStackFrame() + " 相关调用，核对输入参数与线程上下文",
                "检查最近一次发布、配置变更和依赖服务响应超时情况",
                "对异常前后的 traceId 与日志片段做串联，确认是否存在固定触发条件"
        );
        String fixSuggestion = "在 " + fingerprint.getMethodSignature()
                + " 增加参数校验、兜底分支和异常降级策略，并为关键依赖补充超时与重试边界。";
        try {
            return objectMapper.writeValueAsString(new AiAnalysisResultDtoBuilder(probableRootCause, impactScope, steps, fixSuggestion).build());
        } catch (JsonProcessingException e) {
            throw new RuntimeException("构造本地 AI 报告失败", e);
        }
    }

    private String buildTitle(ExceptionFingerprint fingerprint) {
        return fingerprint.getServiceName() + " / " + fingerprint.getExceptionClass() + " 根因分析";
    }

    private AiReportSummaryDto toSummaryDto(AiAnalysisResult item) {
        AiAnalysisResultDto dto = item.getAnalysisContent() == null ? null : parseJson(item.getAnalysisContent());
        return new AiReportSummaryDto(
                item.getFingerprint(),
                item.getTitle(),
                item.getReportStatus(),
                item.getModelName(),
                item.getTriggerSource(),
                item.getSummary(),
                dto == null ? null : dto.getFixSuggestion(),
                item.getErrorMessage(),
                item.getRequestedAt(),
                item.getStartedAt(),
                item.getAnalysisTime()
        );
    }

    private AiAnalysisResultDto buildResultDto(AiAnalysisResult entity) {
        if (entity.getAnalysisContent() != null) {
            return parseJson(entity.getAnalysisContent());
        }
        AiAnalysisResultDto dto = new AiAnalysisResultDto();
        if ("FAILED".equalsIgnoreCase(entity.getReportStatus())) {
            dto.setProbableRootCause(entity.getErrorMessage());
            dto.setFixSuggestion("请修复失败原因后重新触发 AI 分析。");
            return dto;
        }
        dto.setProbableRootCause("AI 报告正在生成中");
        dto.setFixSuggestion("稍后刷新列表查看最新状态。");
        return dto;
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(keyword);
    }

    private AiAnalysisResultDto parseJson(String json) {
        try {
            return objectMapper.readValue(json, AiAnalysisResultDto.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("AI 返回格式无法解析", e);
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

    private static final class AiAnalysisResultDtoBuilder {
        private final AiAnalysisResultDto dto = new AiAnalysisResultDto();

        private AiAnalysisResultDtoBuilder(String probableRootCause,
                                           String impactScope,
                                           List<String> troubleshootingSteps,
                                           String fixSuggestion) {
            dto.setProbableRootCause(probableRootCause);
            dto.setImpactScope(impactScope);
            dto.setTroubleshootingSteps(troubleshootingSteps);
            dto.setFixSuggestion(fixSuggestion);
        }

        private AiAnalysisResultDto build() {
            return dto;
        }
    }
}

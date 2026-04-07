package com.platform.analyze.controller;

import com.platform.analyze.common.Result;
import com.platform.analyze.dto.AgentRuntimeConfigDto;
import com.platform.analyze.dto.AgentRuntimeConfirmRequest;
import com.platform.analyze.dto.AgentSyncStatusDto;
import com.platform.analyze.dto.RuleSettingsDto;
import com.platform.analyze.service.AgentSyncStatusService;
import com.platform.analyze.service.RuleSettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Rule Settings API", description = "规则配置与 Agent 运行时配置接口")
@RestController
@RequestMapping("/api/settings")
public class RuleSettingsController {

    private final RuleSettingsService ruleSettingsService;
    private final AgentSyncStatusService agentSyncStatusService;

    public RuleSettingsController(RuleSettingsService ruleSettingsService,
                                  AgentSyncStatusService agentSyncStatusService) {
        this.ruleSettingsService = ruleSettingsService;
        this.agentSyncStatusService = agentSyncStatusService;
    }

    @Operation(summary = "查询规则配置")
    @GetMapping
    public Result<RuleSettingsDto> getSettings() {
        return Result.success(ruleSettingsService.getSettings());
    }

    @Operation(summary = "保存规则配置")
    @PutMapping
    public Result<RuleSettingsDto> saveSettings(@Valid @RequestBody RuleSettingsDto dto) {
        return Result.success(ruleSettingsService.save(dto));
    }

    @Operation(summary = "Agent 拉取运行时配置")
    @GetMapping("/agent-runtime")
    public Result<AgentRuntimeConfigDto> getAgentRuntime(@RequestParam String serviceName,
                                                         @RequestParam String appName) {
        return Result.success(ruleSettingsService.getAgentRuntimeConfig(serviceName, appName));
    }

    @Operation(summary = "Agent 回传配置同步状态")
    @PostMapping("/agent-runtime/confirm")
    public Result<Void> confirmAgentRuntime(@Valid @RequestBody AgentRuntimeConfirmRequest request) {
        agentSyncStatusService.confirm(request);
        return Result.success();
    }

    @Operation(summary = "查询 Agent 配置同步状态")
    @GetMapping("/agent-sync-status")
    public Result<List<AgentSyncStatusDto>> getAgentSyncStatuses() {
        return Result.success(agentSyncStatusService.listStatuses());
    }
}

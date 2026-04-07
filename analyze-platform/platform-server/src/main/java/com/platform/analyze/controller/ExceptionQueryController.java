package com.platform.analyze.controller;

import com.platform.analyze.common.Result;
import com.platform.analyze.dto.ExceptionDetailDto;
import com.platform.analyze.dto.ExceptionListItemDto;
import com.platform.analyze.dto.ExceptionSuggestionDto;
import com.platform.analyze.dto.ExceptionTrendDto;
import com.platform.analyze.dto.StatusUpdateRequest;
import com.platform.analyze.entity.ExceptionFingerprint;
import com.platform.analyze.service.ExceptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Exception Query API", description = "异常查询接口")
@RestController
@RequestMapping("/api/exceptions")
public class ExceptionQueryController {

    private final ExceptionService exceptionService;

    public ExceptionQueryController(ExceptionService exceptionService) {
        this.exceptionService = exceptionService;
    }

    @Operation(summary = "分页查询异常事件")
    @GetMapping
    public Result<Page<ExceptionListItemDto>> getExceptions(
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String serviceName,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer days,
            @PageableDefault(size = 20, sort = "occurrenceTime", direction = Sort.Direction.DESC) Pageable pageable) {
        return Result.success(exceptionService.getEvents(severity, status, serviceName, keyword, days, pageable));
    }

    @Operation(summary = "查询异常详情")
    @GetMapping("/{id}")
    public Result<ExceptionDetailDto> getExceptionById(@PathVariable Long id) {
        return Result.success(exceptionService.getEventDetail(id));
    }

    @Operation(summary = "更新异常状态")
    @PatchMapping("/{id}/status")
    public Result<ExceptionDetailDto> updateStatus(@PathVariable Long id, @Valid @RequestBody StatusUpdateRequest request) {
        return Result.success(exceptionService.updateStatus(id, request.getStatus()));
    }

    @Operation(summary = "为异常生成处理建议")
    @PostMapping("/{id}/suggestion")
    public Result<ExceptionSuggestionDto> generateSuggestion(@PathVariable Long id) {
        return Result.success(exceptionService.generateSuggestion(id));
    }

    @Operation(summary = "查询异常指纹聚合结果")
    @GetMapping("/fingerprints")
    public Result<List<ExceptionFingerprint>> getFingerprints() {
        return Result.success(exceptionService.getAllFingerprints());
    }

    @Operation(summary = "查询异常趋势")
    @GetMapping("/trends")
    public Result<List<ExceptionTrendDto>> getTrends(@RequestParam(required = false) Integer days) {
        return Result.success(exceptionService.getTrends(days));
    }
}

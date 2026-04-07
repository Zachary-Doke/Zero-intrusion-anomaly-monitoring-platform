package com.platform.analyze.controller;

import com.platform.analyze.common.Result;
import com.platform.analyze.dto.ExceptionOverviewDto;
import com.platform.analyze.service.ExceptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Dashboard API", description = "总览仪表盘接口")
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final ExceptionService exceptionService;

    public DashboardController(ExceptionService exceptionService) {
        this.exceptionService = exceptionService;
    }

    @Operation(summary = "获取总览仪表盘数据")
    @GetMapping("/overview")
    public Result<ExceptionOverviewDto> getOverview() {
        return Result.success(exceptionService.buildOverview());
    }
}

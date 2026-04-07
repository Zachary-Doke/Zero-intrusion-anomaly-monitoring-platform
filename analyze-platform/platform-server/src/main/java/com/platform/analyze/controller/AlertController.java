package com.platform.analyze.controller;

import com.platform.analyze.common.Result;
import com.platform.analyze.dto.AlertRecordDto;
import com.platform.analyze.service.AlertRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Alert API", description = "告警记录接口")
@RestController
@RequestMapping("/api/alerts")
public class AlertController {

    private final AlertRecordService alertRecordService;

    public AlertController(AlertRecordService alertRecordService) {
        this.alertRecordService = alertRecordService;
    }

    @Operation(summary = "查询告警记录")
    @GetMapping
    public Result<List<AlertRecordDto>> getAlerts(@RequestParam(required = false) String keyword,
                                                  @RequestParam(required = false) String alertStatus,
                                                  @RequestParam(required = false) String sendStatus) {
        return Result.success(alertRecordService.listAlerts(keyword, alertStatus, sendStatus));
    }
}

package com.platform.analyze.controller;

import com.platform.analyze.common.Result;
import com.platform.analyze.dto.ExceptionEventReq;
import com.platform.analyze.service.ExceptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Exception Event API", description = "异常事件上报接口")
@RestController
@RequestMapping("/api/events")
public class ExceptionEventController {

    private final ExceptionService exceptionService;

    public ExceptionEventController(ExceptionService exceptionService) {
        this.exceptionService = exceptionService;
    }

    @Operation(summary = "接收异常事件并存储")
    @PostMapping
    public Result<Void> receiveEvent(@Valid @RequestBody ExceptionEventReq req) {
        exceptionService.saveEvent(req);
        return Result.success();
    }

    @Operation(summary = "批量接收异常事件并存储")
    @PostMapping("/batch")
    public Result<Void> receiveEvents(@Valid @RequestBody List<@Valid ExceptionEventReq> requests) {
        exceptionService.saveEvents(requests);
        return Result.success();
    }
}

package com.platform.analyze.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class StatusUpdateRequest {

    @NotBlank(message = "状态不能为空")
    private String status;
}

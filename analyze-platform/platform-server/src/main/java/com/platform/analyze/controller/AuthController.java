package com.platform.analyze.controller;

import com.platform.analyze.auth.PlatformAuthService;
import com.platform.analyze.common.Result;
import com.platform.analyze.dto.LoginRequest;
import com.platform.analyze.dto.LoginResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth API", description = "平台登录接口")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final PlatformAuthService platformAuthService;

    public AuthController(PlatformAuthService platformAuthService) {
        this.platformAuthService = platformAuthService;
    }

    @Operation(summary = "平台登录")
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(platformAuthService.login(request));
    }
}

package com.platform.analyze.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class LoginResponse {

    private String token;
    private String username;
    private String displayName;
    private String role;
    private LocalDateTime expiresAt;
}

package com.platform.analyze.auth;

import java.time.LocalDateTime;

public record AuthenticatedUser(
        String username,
        String displayName,
        UserRole role,
        LocalDateTime expiresAt
) {
}

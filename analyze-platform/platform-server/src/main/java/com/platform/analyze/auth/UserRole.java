package com.platform.analyze.auth;

import java.util.Locale;

public enum UserRole {
    VIEWER,
    OPERATOR,
    ADMIN;

    public boolean covers(UserRole requiredRole) {
        return this.ordinal() >= requiredRole.ordinal();
    }

    public static UserRole from(String value) {
        if (value == null || value.trim().isEmpty()) {
            return VIEWER;
        }
        return UserRole.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}

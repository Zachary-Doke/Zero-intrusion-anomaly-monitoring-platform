package com.platform.analyze.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.analyze.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;

@Component
public class ApiAuthInterceptor implements HandlerInterceptor {

    private final PlatformAuthService authService;
    private final ObjectMapper objectMapper;

    public ApiAuthInterceptor(PlatformAuthService authService, ObjectMapper objectMapper) {
        this.authService = authService;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String path = request.getRequestURI();
        try {
            if ("/api/auth/login".equals(path)) {
                return true;
            }
            if (requiresAgentAuth(path)) {
                authService.authenticateAgent(request.getHeader("X-Agent-Key"));
                return true;
            }
            AuthenticatedUser user = authService.authenticateBearer(request.getHeader("Authorization"));
            authService.authorize(user, resolveRequiredRole(path, request.getMethod()));
            return true;
        } catch (ForbiddenException ex) {
            writeError(response, HttpServletResponse.SC_FORBIDDEN, 403, ex.getMessage());
            return false;
        } catch (UnauthorizedException ex) {
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, 401, ex.getMessage());
            return false;
        }
    }

    private boolean requiresAgentAuth(String path) {
        return path.startsWith("/api/events")
                || "/api/settings/agent-runtime".equals(path)
                || "/api/settings/agent-runtime/confirm".equals(path);
    }

    private UserRole resolveRequiredRole(String path, String method) {
        if (path.startsWith("/api/dashboard")) {
            return UserRole.VIEWER;
        }
        if (path.startsWith("/api/exceptions")) {
            if ("PATCH".equalsIgnoreCase(method) || path.endsWith("/suggestion")) {
                return UserRole.OPERATOR;
            }
            return UserRole.VIEWER;
        }
        if (path.startsWith("/api/settings")) {
            return UserRole.ADMIN;
        }
        return UserRole.ADMIN;
    }

    private void writeError(HttpServletResponse response, int httpStatus, int resultCode, String message) throws Exception {
        response.setStatus(httpStatus);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(Result.error(resultCode, message)));
    }
}

package com.platform.analyze.auth;

import com.platform.analyze.dto.LoginRequest;
import com.platform.analyze.dto.LoginResponse;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;

@Service
public class PlatformAuthService {

    private final AuthProperties authProperties;
    private final TokenService tokenService;

    public PlatformAuthService(AuthProperties authProperties, TokenService tokenService) {
        this.authProperties = authProperties;
        this.tokenService = tokenService;
    }

    public LoginResponse login(LoginRequest request) {
        AuthProperties.UserConfig userConfig = authProperties.getUsers().stream()
                .filter(item -> item.getUsername().equals(request.getUsername()))
                .findFirst()
                .orElseThrow(() -> new UnauthorizedException("账号或密码错误"));
        if (!safeEquals(userConfig.getPassword(), request.getPassword())) {
            throw new UnauthorizedException("账号或密码错误");
        }
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(Math.max(30L, authProperties.getTokenTtlMinutes()));
        AuthenticatedUser user = new AuthenticatedUser(
                userConfig.getUsername(),
                userConfig.getDisplayName(),
                UserRole.from(userConfig.getRole()),
                expiresAt
        );
        return new LoginResponse(
                tokenService.issueToken(user),
                user.username(),
                user.displayName(),
                user.role().name(),
                expiresAt
        );
    }

    public AuthenticatedUser authenticateBearer(String authorizationHeader) {
        if (!StringUtils.hasText(authorizationHeader) || !authorizationHeader.startsWith("Bearer ")) {
            throw new UnauthorizedException("缺少 Bearer Token");
        }
        return tokenService.parseToken(authorizationHeader.substring("Bearer ".length()).trim());
    }

    public void authorize(AuthenticatedUser user, UserRole requiredRole) {
        if (!user.role().covers(requiredRole)) {
            throw new ForbiddenException("当前账号无权访问该接口");
        }
    }

    public void authenticateAgent(String agentApiKey) {
        if (!safeEquals(authProperties.getAgentApiKey(), agentApiKey)) {
            throw new UnauthorizedException("Agent API Key 校验失败");
        }
    }

    private boolean safeEquals(String expected, String actual) {
        if (expected == null || actual == null) {
            return false;
        }
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8));
    }
}

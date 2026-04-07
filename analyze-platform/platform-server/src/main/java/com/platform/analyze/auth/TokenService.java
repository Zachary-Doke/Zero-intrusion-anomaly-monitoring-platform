package com.platform.analyze.auth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class TokenService {

    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();

    private final AuthProperties authProperties;
    private final ObjectMapper objectMapper;

    public TokenService(AuthProperties authProperties, ObjectMapper objectMapper) {
        this.authProperties = authProperties;
        this.objectMapper = objectMapper;
    }

    public String issueToken(AuthenticatedUser user) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("username", user.username());
        payload.put("displayName", user.displayName());
        payload.put("role", user.role().name());
        payload.put("expiresAt", user.expiresAt().toString());
        try {
            String encodedPayload = URL_ENCODER.encodeToString(objectMapper.writeValueAsBytes(payload));
            String signature = sign(encodedPayload);
            return encodedPayload + "." + signature;
        } catch (JsonProcessingException e) {
            throw new RuntimeException("生成登录令牌失败", e);
        }
    }

    public AuthenticatedUser parseToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new UnauthorizedException("缺少登录令牌");
        }
        String[] parts = token.split("\\.", 2);
        if (parts.length != 2) {
            throw new UnauthorizedException("登录令牌格式非法");
        }
        String expectedSignature = sign(parts[0]);
        if (!MessageDigest.isEqual(expectedSignature.getBytes(StandardCharsets.UTF_8), parts[1].getBytes(StandardCharsets.UTF_8))) {
            throw new UnauthorizedException("登录令牌校验失败");
        }
        try {
            JsonNode payload = objectMapper.readTree(URL_DECODER.decode(parts[0]));
            LocalDateTime expiresAt = LocalDateTime.parse(payload.path("expiresAt").asText());
            if (expiresAt.isBefore(LocalDateTime.now())) {
                throw new UnauthorizedException("登录已过期，请重新登录");
            }
            return new AuthenticatedUser(
                    payload.path("username").asText(),
                    payload.path("displayName").asText(),
                    UserRole.from(payload.path("role").asText()),
                    expiresAt
            );
        } catch (UnauthorizedException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new UnauthorizedException("登录令牌解析失败");
        }
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(authProperties.getTokenSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return URL_ENCODER.encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException("登录令牌签名失败", e);
        }
    }
}

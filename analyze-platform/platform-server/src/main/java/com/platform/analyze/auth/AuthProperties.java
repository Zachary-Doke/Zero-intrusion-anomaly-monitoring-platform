package com.platform.analyze.auth;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "platform.auth")
public class AuthProperties {

    private String tokenSecret = "zero-intrusion-local-secret";
    private long tokenTtlMinutes = 480L;
    private String agentApiKey = "local-agent-key";
    private List<UserConfig> users = new ArrayList<>();

    @Data
    public static class UserConfig {
        private String username;
        private String password;
        private String displayName;
        private String role = "VIEWER";
    }
}

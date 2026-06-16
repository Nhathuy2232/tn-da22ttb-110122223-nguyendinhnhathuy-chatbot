package com.kltn.chatbot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration cho JWT
 * Đọc từ application.yml section jwt
 * 
 * @author Nguyễn Đình Nhật Huy
 */
@Configuration
@ConfigurationProperties(prefix = "jwt")
@Data
public class JwtConfig {

    /**
     * Secret key để sign JWT token
     */
    private String secret;

    /**
     * Thời gian expire của token (milliseconds)
     * Default: 86400000 (24 hours)
     */
    private Long expiration = 86400000L;

    /**
     * Issuer của token
     */
    private String issuer = "chatbot-kltn-system";
}

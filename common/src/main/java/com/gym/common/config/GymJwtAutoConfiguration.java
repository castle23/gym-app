package com.gym.common.config;

import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

import javax.crypto.SecretKey;

@AutoConfiguration
@ConditionalOnProperty("gym.jwt.issuer")
public class GymJwtAutoConfiguration {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration-ms:86400000}")
    private long expirationMs;

    @Value("${jwt.refresh-expiration-ms:604800000}")
    private long refreshExpirationMs;

    @Bean
    public SecretKey secretKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    @Bean
    public Long jwtExpirationMs() {
        return expirationMs;
    }

    @Bean
    public Long jwtRefreshExpirationMs() {
        return refreshExpirationMs;
    }
}

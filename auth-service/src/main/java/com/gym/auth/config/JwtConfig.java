package com.gym.auth.config;

import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.crypto.SecretKey;

@Configuration
public class JwtConfig {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration-ms}")
    private long expirationMs;

    @Bean
    public SecretKey secretKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    @Bean
    public Long jwtExpirationMs() {
        return expirationMs;
    }
}

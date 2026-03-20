package com.gym.auth.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

@Slf4j
@Service
public class JwtService {

    private final SecretKey secretKey;
    private final Long expirationMs;
    private final Long refreshExpirationMs;

    public JwtService(SecretKey secretKey, 
                      @Qualifier("jwtExpirationMs") Long expirationMs,
                      @Qualifier("jwtRefreshExpirationMs") Long refreshExpirationMs) {
        this.secretKey = secretKey;
        this.expirationMs = expirationMs;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    /**
     * Generate access token
     */
    public String generateToken(String userId, String roles) {
        try {
            return Jwts.builder()
                    .subject(userId)
                    .claim("roles", roles)
                    .claim("type", "access")
                    .issuedAt(new Date())
                    .expiration(new Date(System.currentTimeMillis() + expirationMs))
                    .signWith(secretKey)
                    .compact();
        } catch (Exception e) {
            log.error("Error generating JWT token", e);
            throw new RuntimeException("Failed to generate JWT token", e);
        }
    }

    /**
     * Generate refresh token (longer expiration time)
     */
    public String generateRefreshToken(String userId) {
        try {
            return Jwts.builder()
                    .subject(userId)
                    .claim("type", "refresh")
                    .issuedAt(new Date())
                    .expiration(new Date(System.currentTimeMillis() + refreshExpirationMs))
                    .signWith(secretKey)
                    .compact();
        } catch (Exception e) {
            log.error("Error generating refresh token", e);
            throw new RuntimeException("Failed to generate refresh token", e);
        }
    }

    /**
     * Extract subject (user ID) from token
     */
    public String extractSubject(String token) {
        try {
            Jws<Claims> jws = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return jws.getPayload().getSubject();
        } catch (Exception e) {
            log.error("Error extracting subject from JWT token", e);
            return null;
        }
    }

    /**
     * Extract roles from token
     */
    public String extractRoles(String token) {
        try {
            Jws<Claims> jws = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return (String) jws.getPayload().get("roles");
        } catch (Exception e) {
            log.error("Error extracting roles from JWT token", e);
            return null;
        }
    }

    /**
     * Extract token type (access or refresh)
     */
    public String extractTokenType(String token) {
        try {
            Jws<Claims> jws = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return (String) jws.getPayload().get("type");
        } catch (Exception e) {
            log.error("Error extracting token type from JWT token", e);
            return null;
        }
    }

    /**
     * Check if token is valid and not expired
     */
    public boolean isTokenValid(String token) {
        try {
            Jws<Claims> jws = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return !jws.getPayload().getExpiration().before(new Date());
        } catch (Exception e) {
            log.debug("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Check if token is a valid refresh token
     */
    public boolean isRefreshTokenValid(String token) {
        try {
            if (!isTokenValid(token)) {
                return false;
            }

            String tokenType = extractTokenType(token);
            return "refresh".equals(tokenType);
        } catch (Exception e) {
            log.debug("Invalid refresh token: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get token expiration time
     */
    public Long getTokenExpiration(String token) {
        try {
            Jws<Claims> jws = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            Date expiration = jws.getPayload().getExpiration();
            return expiration.getTime();
        } catch (Exception e) {
            log.debug("Error extracting expiration from token", e);
            return null;
        }
    }
}

package com.gym.auth.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtService {

    private final SecretKey secretKey;
    private final Long expirationMs;

    public String generateToken(String userId, String roles) {
        try {
            return Jwts.builder()
                    .setSubject(userId)
                    .claim("roles", roles)
                    .setIssuedAt(new Date())
                    .setExpiration(new Date(System.currentTimeMillis() + expirationMs))
                    .signWith(secretKey)
                    .compact();
        } catch (Exception e) {
            log.error("Error generating JWT token", e);
            throw new RuntimeException("Failed to generate JWT token", e);
        }
    }

    public String extractSubject(String token) {
        try {
            Jws<Claims> jws = Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token);
            return jws.getBody().getSubject();
        } catch (Exception e) {
            log.error("Error extracting subject from JWT token", e);
            return null;
        }
    }

    public String extractRoles(String token) {
        try {
            Jws<Claims> jws = Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token);
            return (String) jws.getBody().get("roles");
        } catch (Exception e) {
            log.error("Error extracting roles from JWT token", e);
            return null;
        }
    }

    public boolean isTokenValid(String token) {
        try {
            Jws<Claims> jws = Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token);
            return !jws.getBody().getExpiration().before(new Date());
        } catch (Exception e) {
            log.debug("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }
}

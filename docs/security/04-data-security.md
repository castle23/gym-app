# Data Security

## Overview

Data protection mechanisms in the Gym Platform: encryption at rest, encryption in transit, PII handling, data classification, and secure data disposal. Protecting user data is critical for compliance and user trust.

> **Note**: This document describes data security patterns and aspirational features. Field-level encryption with JPA converters, pgcrypto, and HTTPS/TLS configuration are target practices. The current deployment uses Docker Compose without TLS termination at the application layer.

**Data Security Approach:**
- Encryption at rest for sensitive data
- Encryption in transit (TLS 1.2+)
- PII identification and protection
- Data classification framework
- Secure deletion procedures

---

## Encryption at Rest

### JPA Entity Encryption

```java
@Entity
@Table(name = "users")
public class User {
    @Id
    private Long id;
    
    // Encrypted PII fields
    @Convert(converter = EncryptionConverter.class)
    private String email;
    
    @Convert(converter = EncryptionConverter.class)
    private String phoneNumber;
    
    private String passwordHash;  // Already hashed
}

@Converter
public class EncryptionConverter implements AttributeConverter<String, String> {
    
    @Autowired
    private EncryptionService encryptionService;
    
    @Override
    public String convertToDatabaseColumn(String attribute) {
        return attribute != null ? encryptionService.encrypt(attribute) : null;
    }
    
    @Override
    public String convertToEntityAttribute(String encrypted) {
        return encrypted != null ? encryptionService.decrypt(encrypted) : null;
    }
}

@Service
public class EncryptionService {
    
    @Value("${encryption.key}")
    private String encryptionKey;
    
    public String encrypt(String plaintext) {
        try {
            // Generate IV
            byte[] iv = new byte[16];
            new SecureRandom().nextBytes(iv);
            
            // Create cipher
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            SecretKey key = new SecretKeySpec(
                Base64.getDecoder().decode(encryptionKey), 0, 32, "AES"
            );
            cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));
            
            // Encrypt
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            
            // Combine IV + ciphertext
            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);
            
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }
    
    public String decrypt(String encrypted) {
        try {
            byte[] combined = Base64.getDecoder().decode(encrypted);
            byte[] iv = new byte[16];
            System.arraycopy(combined, 0, iv, 0, 16);
            
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            SecretKey key = new SecretKeySpec(
                Base64.getDecoder().decode(encryptionKey), 0, 32, "AES"
            );
            cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
            
            byte[] decrypted = cipher.doFinal(combined, 16, combined.length - 16);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }
}
```

### PostgreSQL Configuration

```sql
-- Enable pgcrypto for server-side encryption
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- SSL/TLS connections
-- In postgresql.conf:
ssl = on
ssl_cert_file = '/etc/ssl/certs/server.crt'
ssl_key_file = '/etc/ssl/private/server.key'
```

---

## Encryption in Transit

### HTTPS/TLS Setup

```properties
# application.properties
server.ssl.enabled=true
server.ssl.key-store=/app/keystore.p12
server.ssl.key-store-password=${KEYSTORE_PASSWORD}
server.ssl.key-store-type=PKCS12
server.ssl.key-alias=gym-cert
server.port=8443
server.http2.enabled=true
```

### HSTS Headers

```java
@Configuration
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .requiresChannel()
                .anyRequest().requiresSecure()
            .and()
            .headers()
                .httpStrictTransportSecurity()
                    .includeSubDomains(true)
                    .preload(true)
                    .maxAgeInSeconds(31536000);
        
        return http.build();
    }
}
```

---

## PII Protection

### Detection and Masking

```java
@Component
public class PiiService {
    
    public String maskEmail(String email) {
        int at = email.indexOf('@');
        return email.substring(0, Math.min(2, at)) + "***@***";
    }
    
    public String maskPhone(String phone) {
        return phone.substring(0, 3) + "-***-****";
    }
    
    public String maskSSN(String ssn) {
        return "***-**-" + ssn.substring(ssn.length() - 4);
    }
}
```

### Logging Protection

```java
@Service
public class AuditLogService {
    
    public void log(String message) {
        String sanitized = message
            .replaceAll("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+", "[email]")
            .replaceAll("\\+?1?\\d{9,15}", "[phone]")
            .replaceAll("\\d{3}-\\d{2}-\\d{4}", "[ssn]");
        
        logger.info(sanitized);
    }
}
```

---

## Data Classification

### Levels and Protection

| Level | Type | Examples | Protection |
|-------|------|----------|-----------|
| 1 | Public | Documentation | No encryption |
| 2 | Internal | Policies | Transit only |
| 3 | Confidential | Health data | At rest + transit |
| 4 | Secret | Passwords, Keys | Full encryption |

---

## Access Control

### Field-level Access

```java
@Service
public class DataAccessService {
    
    public UserDTO getUser(Long userId, Long requestingUserId, Set<String> roles) {
        User user = userRepository.findById(userId).orElseThrow();
        UserDTO dto = new UserDTO();
        
        // Public info
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        
        // Own data
        if (userId.equals(requestingUserId)) {
            dto.setEmail(user.getEmail());
            dto.setPhoneNumber(user.getPhoneNumber());
        }
        // Admins see all
        else if (roles.contains("ROLE_ADMIN")) {
            dto.setAllData(user);
        }
        
        return dto;
    }
}
```

---

## Secure Deletion

### GDPR Implementation

```java
@Service
@Transactional
public class DataDeletionService {
    
    public void deleteUserData(Long userId) {
        // Delete all related data
        sessionRepository.deleteByUserId(userId);
        exerciseRepository.deleteByUserId(userId);
        
        // Anonymize audit logs
        auditLogRepository.findByUserId(userId).forEach(log -> {
            log.setUserId(null);
            log.setUsername("DELETED_USER_" + UUID.randomUUID());
            auditLogRepository.save(log);
        });
        
        // Delete user
        userRepository.deleteById(userId);
        log.info("User data fully deleted");
    }
}
```

---

## Related Documentation

- [01-security-overview.md](01-security-overview.md)
- [06-compliance.md](06-compliance.md) - GDPR
- docs/troubleshooting/06-security-troubleshooting.md

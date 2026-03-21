# Java & Spring Boot

## Overview

Gym Platform uses **Java 17+** with **Spring Boot 3.x** as the core technology stack. This document covers the Java features, Spring Boot framework architecture, dependency management, and configuration strategies used across all microservices.

**Current Versions:**
- Java: 17 (LTS)
- Spring Boot: 3.x
- Spring Framework: 6.x
- Maven: 3.8.x

## Java 17 Features

### Records
Used for immutable data objects and DTOs to reduce boilerplate:

```java
public record UserRequest(
    String username,
    String email,
    String password
) {}
```

Benefits:
- Automatic `equals()`, `hashCode()`, `toString()`
- Final and immutable by default
- Compact constructor support
- Reduced memory footprint

### Sealed Classes
Define restricted class hierarchies for type safety:

```java
public sealed class Response permits SuccessResponse, ErrorResponse {
    public abstract void handle();
}

public final class SuccessResponse extends Response {
    @Override
    public void handle() { /* ... */ }
}
```

Usage:
- Type-safe enums with behavior
- Domain model restrictions
- Pattern matching exhaustiveness

### Text Blocks
Multi-line string literals for JSON, SQL, and documentation:

```java
String query = """
    SELECT u.id, u.username, u.email
    FROM users u
    WHERE u.active = true
    ORDER BY u.created_at DESC
    """;
```

### Pattern Matching
Enhanced instanceof with type narrowing:

```java
if (response instanceof SuccessResponse(String message)) {
    logger.info("Success: {}", message);
} else if (response instanceof ErrorResponse(String error, int code)) {
    logger.error("Error [{}]: {}", code, error);
}
```

### var Type Inference
Local variable type inference reduces verbosity:

```java
var users = userRepository.findAll();
var mapper = objectMapper.writerWithDefaultPrettyPrinter();
var response = restTemplate.postForObject(url, request, String.class);
```

### Module System (java.base, etc.)
Explicit module declarations for microservices:

```
module gym.auth.service {
    requires spring.boot;
    requires spring.security.core;
    requires java.base;
    exports com.gym.auth.api;
}
```

## Spring Boot 3.x Architecture

### Application Structure

```
auth-service/
├── src/main/java/com/gym/auth/
│   ├── AuthApplication.java (Main bootstrap class)
│   ├── config/
│   │   ├── SecurityConfig.java
│   │   ├── JwtConfig.java
│   │   └── CorsConfig.java
│   ├── controller/
│   │   ├── AuthController.java
│   │   └── UserController.java
│   ├── service/
│   │   ├── AuthService.java
│   │   ├── TokenService.java
│   │   └── UserService.java
│   ├── repository/
│   │   ├── UserRepository.java
│   │   └── TokenRepository.java
│   ├── dto/
│   │   ├── LoginRequest.java
│   │   ├── LoginResponse.java
│   │   └── UserDTO.java
│   ├── entity/
│   │   ├── User.java
│   │   └── RefreshToken.java
│   ├── exception/
│   │   ├── AuthenticationException.java
│   │   └── GlobalExceptionHandler.java
│   └── security/
│       ├── JwtProvider.java
│       ├── JwtFilter.java
│       └── CustomUserDetailsService.java
├── src/main/resources/
│   ├── application.yml (Primary config)
│   ├── application-dev.yml (Development)
│   ├── application-prod.yml (Production)
│   └── db/migration/ (Flyway migrations)
└── pom.xml (Maven dependencies)
```

### Bootstrap Configuration

**AuthApplication.java:**
```java
@SpringBootApplication
@EnableWebSecurity
@EnableConfigurationProperties(JwtProperties.class)
public class AuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthApplication.class, args);
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
}
```

### Configuration Management

**application.yml (Common):**
```yaml
spring:
  application:
    name: auth-service
  profiles:
    active: ${PROFILE:dev}
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQL13Dialect
        jdbc:
          batch_size: 20
  datasource:
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
      connection-timeout: 30000
  security:
    user:
      password: admin123
  mvc:
    log-request-details: false

management:
  endpoints:
    web:
      exposure: include: health,metrics,info
  endpoint:
    health:
      show-details: when-authorized

logging:
  level:
    root: INFO
    com.gym: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
  file:
    name: logs/auth-service.log
    max-size: 10MB
    max-history: 30

server:
  port: 8081
  servlet:
    context-path: /api/v1
```

**application-prod.yml (Production):**
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 10
      leak-detection-threshold: 60000

management:
  endpoints:
    web:
      exposure: include: health,info
  endpoint:
    health:
      show-details: always

logging:
  level:
    root: WARN
    com.gym: INFO
  file:
    name: /var/log/auth-service.log
    max-size: 100MB
    max-history: 90
```

## Dependency Management

### Core Dependencies (pom.xml)

```xml
<dependencies>
    <!-- Spring Boot Starters -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
        <version>3.1.5</version>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
        <version>3.1.5</version>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
        <version>3.1.5</version>
    </dependency>

    <!-- Database -->
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
        <version>42.7.1</version>
    </dependency>

    <!-- JWT -->
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-api</artifactId>
        <version>0.12.3</version>
    </dependency>

    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-impl</artifactId>
        <version>0.12.3</version>
        <scope>runtime</scope>
    </dependency>

    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-jackson</artifactId>
        <version>0.12.3</version>
        <scope>runtime</scope>
    </dependency>

    <!-- Lombok -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <version>1.18.30</version>
        <scope>provided</scope>
    </dependency>

    <!-- MapStruct -->
    <dependency>
        <groupId>org.mapstruct</groupId>
        <artifactId>mapstruct</artifactId>
        <version>1.5.5.Final</version>
    </dependency>

    <!-- Testing -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <version>3.1.5</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```

### Build Configuration

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
            <version>3.1.5</version>
            <configuration>
                <excludes>
                    <exclude>
                        <groupId>org.projectlombok</groupId>
                        <artifactId>lombok</artifactId>
                    </exclude>
                </excludes>
            </configuration>
        </plugin>

        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <version>3.11.0</version>
            <configuration>
                <source>17</source>
                <target>17</target>
                <annotationProcessorPaths>
                    <path>
                        <groupId>org.projectlombok</groupId>
                        <artifactId>lombok</artifactId>
                        <version>1.18.30</version>
                    </path>
                    <path>
                        <groupId>org.mapstruct</groupId>
                        <artifactId>mapstruct-processor</artifactId>
                        <version>1.5.5.Final</version>
                    </path>
                </annotationProcessorPaths>
            </configuration>
        </plugin>
    </plugins>
</build>
```

## Common Annotations

| Annotation | Purpose | Example |
|------------|---------|---------|
| `@SpringBootApplication` | Main application class | Enables auto-configuration |
| `@RestController` | REST endpoint controller | Combines `@Controller` + `@ResponseBody` |
| `@Service` | Business logic layer | Marks service beans |
| `@Repository` | Data access layer | Spring Data repository beans |
| `@Configuration` | Beans configuration | `@Bean` methods |
| `@Autowired` | Dependency injection | Constructor or field injection |
| `@Transactional` | Transaction management | Method-level transactions |
| `@Entity` | JPA entity | Database table mapping |
| `@RequestMapping` | Route mapping | HTTP method routing |
| `@Validated` | Bean validation | Parameter validation |

## Build and Run Commands

### Build
```bash
# Clean and compile
mvn clean compile

# Build JAR
mvn clean package

# Build with tests skipped
mvn clean package -DskipTests

# Build specific module
mvn clean package -f auth-service/pom.xml
```

### Run
```bash
# Run locally (development)
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"

# Run from JAR
java -jar auth-service-1.0.0.jar --spring.profiles.active=prod

# Run with environment variables
POSTGRES_PASSWORD=secret java -jar auth-service-1.0.0.jar
```

### Debug
```bash
# Run with debug port
java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005 \
  -jar auth-service-1.0.0.jar
```

## Performance Tuning

### JVM Flags (Production)
```bash
java -Xms1G -Xmx2G \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -XX:+ParallelRefProcEnabled \
  -XX:+UnlockDiagnosticVMOptions \
  -XX:G1SummarizeRSetStatsPeriod=1 \
  -jar auth-service-1.0.0.jar
```

### Connection Pooling
```java
@Configuration
public class DataSourceConfig {

    @Bean
    public HikariConfig hikariConfig() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(System.getenv("DB_URL"));
        config.setUsername(System.getenv("DB_USER"));
        config.setPassword(System.getenv("DB_PASSWORD"));
        config.setMaximumPoolSize(20);
        config.setMinimumIdle(5);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        config.setLeakDetectionThreshold(60000);
        return config;
    }
}
```

## Key References

- [Spring Boot Official Documentation](https://spring.io/projects/spring-boot)
- [Java 17 Features Guide](https://www.oracle.com/java/technologies/javase/17-relnote.html)
- [Spring Security Documentation](https://spring.io/projects/spring-security)
- See also: [docs/arquitectura/02-microservices-architecture.md](../arquitectura/02-microservices-architecture.md)
- See also: [docs/development/01-getting-started.md](../development/01-getting-started.md)

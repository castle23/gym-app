# Network Troubleshooting

## Overview

This guide addresses network-level issues in the Gym Platform microservices: DNS resolution failures, service-to-service communication problems, external API connectivity, and network configuration issues. Network problems often manifest as intermittent failures and timeouts.

**Network Architecture:**
- Internal: Docker Compose network (gym_default)
- Services: Auth, Training, Tracking, Notification (TCP 8080+)
- External: PostgreSQL, RabbitMQ
- API Gateway: Nginx (optional, for production)
- Monitoring: Prometheus scraping services on /actuator/prometheus

---

## Table of Contents

1. [DNS Resolution](#dns-resolution)
2. [Service-to-Service Communication](#service-to-service-communication)
3. [External Service Connectivity](#external-service-connectivity)
4. [Port and Firewall Issues](#port-and-firewall-issues)
5. [Network Performance](#network-performance)
6. [Load Balancing](#load-balancing)

---

## DNS Resolution

### Issue: Cannot Resolve Service Names

**Symptoms:**
```
Cannot connect to gym-training: Name or service not known
nslookup: unknown host
DNS resolution failed
```

**Diagnostic Steps:**

1. **Test DNS inside container:**
```bash
docker exec gym-auth nslookup gym-training
docker exec gym-auth nslookup postgres
docker exec gym-auth nslookup rabbitmq
```

2. **Check /etc/resolv.conf inside container:**
```bash
docker exec gym-auth cat /etc/resolv.conf
# Should have nameserver entries pointing to Docker's DNS
```

3. **Test with ping:**
```bash
docker exec gym-auth ping gym-training
# Should resolve and respond
```

4. **Inspect Docker network:**
```bash
docker network ls
docker network inspect gym_default
# Should show all service containers with their IPs
```

**Resolution:**

**Ensure containers on same network:**
```yaml
# docker-compose.yml
version: '3.8'
services:
  gym-auth:
    networks:
      - gym_network

  gym-training:
    networks:
      - gym_network

  postgres:
    networks:
      - gym_network

networks:
  gym_network:
    driver: bridge
```

**Verify container names match docker-compose service names:**
```bash
# Service name in docker-compose.yml
services:
  gym-auth:  # This is the DNS name inside network

# Access as: http://gym-auth:8080
```

**Check Docker daemon DNS:**
```bash
# If using custom DNS servers
docker run --dns 8.8.8.8 --dns 8.8.4.4 -it gym-auth /bin/bash
nslookup google.com  # Should resolve external names
```

---

## Service-to-Service Communication

### Issue: Connection Refused Between Services

**Symptoms:**
```
ERROR: Connection refused connecting to http://gym-training:8080
java.net.ConnectException: Connection refused
ERROR: 111 (Connection refused)
```

**Diagnostic Steps:**

1. **Verify service is running and listening:**
```bash
# Inside service container
docker exec gym-training netstat -tlnp | grep 8080

# Or from another container
docker exec gym-auth curl -v http://gym-training:8080/actuator/health
```

2. **Check service logs:**
```bash
docker logs gym-training | tail -50
# Look for "Tomcat started", errors, or "not listening"
```

3. **Verify network connectivity:**
```bash
# From source service
docker exec gym-auth nc -zv gym-training 8080
# Expected: succeeded! (if service running)

docker exec gym-auth ping gym-training
# Should return IP address
```

4. **Check service port configuration:**
```bash
# Verify internal port is correct
docker exec gym-training env | grep PORT
docker exec gym-training curl http://localhost:8080/actuator/health
```

**Resolution:**

**Fix service URL in calling service:**
```java
// Before: Wrong port/hostname
@Configuration
public class RestClientConfig {
    
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
    
    public void callService() {
        // Wrong: 127.0.0.1 only works inside container
        // Wrong: 8081 might not be mapped
        restTemplate.getForObject("http://127.0.0.1:8081/api/...", String.class);
    }
}

// After: Use service name and internal port
@Configuration
public class RestClientConfig {
    
    @Value("${training.service.url:http://gym-training:8080}")
    private String trainingServiceUrl;
    
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
    
    public void callService() {
        restTemplate.getForObject(
            trainingServiceUrl + "/api/training/sessions",
            String.class
        );
    }
}
```

**Configure through environment:**
```yaml
# docker-compose.yml
services:
  gym-auth:
    environment:
      - TRAINING_SERVICE_URL=http://gym-training:8080
      - TRACKING_SERVICE_URL=http://gym-tracking:8080
      - NOTIFICATION_SERVICE_URL=http://gym-notification:8080
```

**Add connection retry logic:**
```java
@Configuration
public class RestClientConfig {
    
    @Bean
    public RestTemplate restTemplate() {
        HttpClientHttpRequestFactory factory = new HttpClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(5000);
        
        return new RestTemplate(factory);
    }
    
    @Bean
    public Resilience4jCircuitBreakerFactory resilience4jFactory() {
        return new Resilience4jCircuitBreakerFactory();
    }
}

@Service
public class TrainingServiceClient {
    
    @CircuitBreaker(name = "trainingService", fallbackMethod = "trainingServiceFallback")
    public List<Session> getSessions() {
        return restTemplate.getForObject(
            trainingServiceUrl + "/api/sessions",
            new ParameterizedTypeReference<List<Session>>() {}
        );
    }
    
    public List<Session> trainingServiceFallback(Exception e) {
        log.warn("Training service unavailable, returning empty list");
        return Collections.emptyList();
    }
}
```

---

## External Service Connectivity

### Issue: Cannot Reach External APIs

**Symptoms:**
```
Connection timeout to external API
SSL certificate verification failed
Connection refused from external service
```

**Diagnostic Steps:**

1. **Test DNS resolution:**
```bash
docker exec gym-auth nslookup api.external-service.com
docker exec gym-auth dig api.external-service.com
```

2. **Test TCP connectivity:**
```bash
docker exec gym-auth nc -zv api.external-service.com 443
docker exec gym-auth curl -v https://api.external-service.com/health
```

3. **Check SSL certificate:**
```bash
docker exec gym-auth openssl s_client -connect api.external-service.com:443
```

4. **Verify proxy settings (if applicable):**
```bash
docker exec gym-auth env | grep -i proxy
```

5. **Check firewall from host:**
```bash
# From host machine
curl -v https://api.external-service.com/health
nslookup api.external-service.com
```

**Resolution:**

**Fix certificate verification issues:**
```java
// Before: SSL verification error
@Bean
public RestTemplate restTemplate() {
    // This bypasses SSL verification - NOT RECOMMENDED
    return new RestTemplate();
}

// After: Proper certificate handling
@Bean
public RestTemplate restTemplate() {
    HttpClientHttpRequestFactory factory = new HttpClientHttpRequestFactory();
    
    // Option 1: Add certificate to truststore
    // Option 2: Use system default truststore
    
    HttpClient httpClient = HttpClientBuilder.create()
        .setSSLContext(SSLContexts.createSystemDefault())
        .build();
    
    factory.setHttpClient(httpClient);
    factory.setConnectTimeout(5000);
    
    return new RestTemplate(factory);
}
```

**Configure proxy (if required):**
```yaml
# docker-compose.yml
services:
  gym-auth:
    environment:
      - HTTP_PROXY=http://proxy.example.com:8080
      - HTTPS_PROXY=http://proxy.example.com:8080
      - NO_PROXY=localhost,127.0.0.1,gym-training,gym-tracking
```

**Add timeout and retry:**
```java
@Service
public class ExternalApiService {
    
    @Retryable(
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public ResponseEntity<ExternalData> callExternalApi() {
        try {
            return restTemplate.getForEntity(
                "https://api.external-service.com/data",
                ExternalData.class
            );
        } catch (RestClientException e) {
            log.error("External API call failed", e);
            throw new ServiceUnavailableException("External service unavailable");
        }
    }
}
```

---

## Port and Firewall Issues

### Issue: Port Conflicts or Blocked Ports

**Symptoms:**
```
Address already in use: bind
Cannot reach service on exposed port
Port mapping not working
```

**Diagnostic Steps:**

1. **Check port availability:**
```bash
# Check if port is in use on host
netstat -tlnp | grep 8080
lsof -i :8080

# Check if port is in use in Docker
docker exec gym-auth netstat -tlnp | grep 8080
```

2. **Verify port mappings:**
```bash
docker-compose ps
docker port gym-auth
# Expected: 8080/tcp -> 0.0.0.0:8080
```

3. **Test connectivity to mapped port:**
```bash
# From host machine
curl http://localhost:8080/actuator/health

# From another container
docker exec gym-training curl http://localhost:8080/actuator/health
# Won't work - 8080 is inside gym-auth container
```

**Resolution:**

**Fix port mapping in docker-compose:**
```yaml
# docker-compose.yml
services:
  gym-auth:
    ports:
      - "8080:8080"  # host:container - maps host 8080 to container 8080
    environment:
      - SERVER_PORT=8080  # Internal port

  gym-training:
    ports:
      - "8081:8080"  # Different host port, same container port

  gym-tracking:
    ports:
      - "8082:8080"

  gym-notification:
    ports:
      - "8083:8080"

  postgres:
    ports:
      - "5432:5432"

  rabbitmq:
    ports:
      - "5672:5672"
      - "15672:15672"  # Management console
```

**Kill process using port:**
```bash
# Linux/macOS
kill -9 $(lsof -t -i :8080)

# Windows
netstat -ano | findstr :8080
taskkill /PID <PID> /F
```

---

## Network Performance

### Issue: High Latency or Packet Loss

**Symptoms:**
```
API responses slow (>5 seconds)
Intermittent failures between services
Timeouts on normally fast operations
```

**Diagnostic Steps:**

1. **Measure latency:**
```bash
# Ping service
docker exec gym-auth ping gym-training -c 5

# Measure HTTP latency
docker exec gym-auth curl -w "Time: %{time_total}s\n" http://gym-training:8080/actuator/health
```

2. **Check network stats:**
```bash
# Docker network stats
docker exec gym-auth cat /proc/net/dev

# Network interface stats
docker exec gym-auth netstat -s
```

3. **Monitor traffic:**
```bash
# From host, capture traffic between containers
tcpdump -i docker0 -n host gym-auth
```

**Resolution:**

**Increase network buffer sizes:**
```yaml
# docker-compose.yml
services:
  gym-auth:
    sysctls:
      - net.core.rmem_max=134217728
      - net.core.wmem_max=134217728
      - net.ipv4.tcp_rmem=4096 87380 67108864
      - net.ipv4.tcp_wmem=4096 65536 67108864
```

**Optimize request batching:**
```java
// Before: Multiple requests
for (Long userId : userIds) {
    User user = restTemplate.getForObject(
        trainingServiceUrl + "/api/users/" + userId,
        User.class
    );
    processUser(user);
}

// After: Single batch request
UserBatchRequest request = new UserBatchRequest(userIds);
List<User> users = restTemplate.postForObject(
    trainingServiceUrl + "/api/users/batch",
    request,
    new ParameterizedTypeReference<List<User>>() {}
);
```

---

## Load Balancing

### Using Nginx as API Gateway

**Purpose:** Route requests to multiple service instances

**Configuration:**
```nginx
# nginx.conf
upstream gym_auth {
    server gym-auth-1:8080;
    server gym-auth-2:8080;
    server gym-auth-3:8080;
}

upstream gym_training {
    server gym-training-1:8080;
    server gym-training-2:8080;
}

server {
    listen 80;
    
    location /api/auth {
        proxy_pass http://gym_auth;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
    
    location /api/training {
        proxy_pass http://gym_training;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

**Docker Compose with Nginx:**
```yaml
services:
  nginx:
    image: nginx:latest
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf:ro
      - ./ssl:/etc/nginx/ssl:ro

  gym-auth-1:
    image: gym/auth:latest

  gym-auth-2:
    image: gym/auth:latest

  gym-training-1:
    image: gym/training:latest

  gym-training-2:
    image: gym/training:latest
```

---

## Network Troubleshooting Checklist

- [ ] DNS resolves service names correctly
- [ ] Service-to-service communication working
- [ ] External API connectivity verified
- [ ] All ports properly mapped and not conflicting
- [ ] Network latency <100ms between services
- [ ] No packet loss between services
- [ ] Firewall not blocking required ports
- [ ] Load balancer distributing traffic evenly

---

## Related Documentation

- [02-debugging-techniques.md](02-debugging-techniques.md) - Network debugging
- [03-common-issues.md](03-common-issues.md) - Common network issues
- [04-diagnostic-procedures.md](04-diagnostic-procedures.md) - Network diagnostics
- docs/deployment/04-scaling.md - Load balancing configuration
- docs/architecture/01-overview.md - Network architecture

# Network Troubleshooting

## Overview

This guide addresses network-level issues in the Gym Platform microservices: DNS resolution failures, service-to-service communication problems, external API connectivity, and network configuration issues. Network problems often manifest as intermittent failures and timeouts.

**Network Architecture:**
- Internal: Docker Compose network (gym_default)
- Services: API Gateway (8080), Auth (8081), Training (8082), Tracking (8083), Notification (8084)
- Database: PostgreSQL (5432)
- No service-to-service direct communication — all requests go through API Gateway

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
docker exec auth-service nslookup postgres
docker exec api-gateway nslookup auth-service
```

2. **Check /etc/resolv.conf inside container:**
```bash
docker exec auth-service cat /etc/resolv.conf
```

3. **Test with ping:**
```bash
docker exec auth-service ping postgres
```

4. **Inspect Docker network:**
```bash
docker network ls
docker network inspect gym_default
```

**Resolution:**

**Ensure containers on same network:**
```yaml
services:
  api-gateway:
    networks:
      - gym_network
  auth-service:
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

> **Note**: Services in this platform do not communicate directly with each other. All requests flow through the API Gateway (port 8080), which validates JWT and injects `X-User-Id`/`X-User-Roles` headers. The diagnostics below apply to gateway-to-service connectivity.

### Issue: Connection Refused Between Services

**Symptoms:**
```
ERROR: Connection refused connecting to http://auth-service:8081
java.net.ConnectException: Connection refused
```

**Diagnostic Steps:**

1. **Verify service is running and listening:**
```bash
docker exec auth-service netstat -tlnp | grep 8081
docker exec api-gateway curl -v http://auth-service:8081/auth/actuator/health
```

2. **Check service logs:**
```bash
docker logs auth-service | tail -50
```

3. **Verify network connectivity:**
```bash
docker exec api-gateway nc -zv auth-service 8081
docker exec api-gateway ping auth-service
```

**Resolution:**

Ensure all services are on the same Docker network and use the correct service name and port as defined in `docker-compose.yml`.

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
services:
  api-gateway:
    ports:
      - "8080:8080"

  auth-service:
    ports:
      - "8081:8081"

  training-service:
    ports:
      - "8082:8082"

  tracking-service:
    ports:
      - "8083:8083"

  notification-service:
    ports:
      - "8084:8084"

  postgres:
    ports:
      - "5432:5432"
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
docker exec api-gateway ping auth-service -c 5
docker exec api-gateway curl -w "Time: %{time_total}s\n" http://auth-service:8081/auth/actuator/health
```

2. **Check network stats:**
```bash
docker exec auth-service cat /proc/net/dev
```

3. **Monitor traffic:**
```bash
tcpdump -i docker0 -n host auth-service
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

> **Note**: The current platform uses a single Spring Boot API Gateway (port 8080) — not Nginx. Load balancing with multiple instances is not part of the current setup.

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

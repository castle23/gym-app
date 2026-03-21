# ADR-007: API Gateway Pattern

## Status
Accepted

## Date
2026-03-21

## Context

With microservices architecture (ADR-001), we have:
- Auth Service (port 8081)
- Training Service (port 8082)
- Tracking Service (port 8083)
- Notification Service (port 8084)

The challenges:
1. **Multiple Endpoints**: Clients would need to know about all 4 services
2. **Cross-Cutting Concerns**: Authentication, rate limiting, logging needed everywhere
3. **API Versioning**: Hard to manage across multiple services
4. **CORS**: Each service needs separate CORS configuration
5. **Load Balancing**: How do clients find services?
6. **Protocol Differences**: Services might use different protocols

## Decision

We implemented an **API Gateway** as the single entry point:

1. **Single URL**: Clients talk to one gateway (e.g., `api.gym.com`)
2. **Routing**: Gateway routes requests to appropriate microservice
3. **Cross-Cutting Concerns**: Gateway handles auth, rate limiting, logging
4. **Load Balancing**: Gateway load balances across service instances
5. **API Aggregation**: Can combine data from multiple services

## Rationale

### 1. Single Entry Point
Clients only need to know one URL:

```
OLD (Without Gateway):
  Client talks to: Auth (8081), Training (8082), Tracking (8083), Notifications (8084)
  Complex, error-prone

NEW (With Gateway):
  Client talks to: API Gateway (443)
  Gateway routes internally
  Simple and clean
```

### 2. Cross-Cutting Concerns
Gateway centralizes:
- **Authentication**: Verify JWT once at gateway
- **Rate Limiting**: Protect services from overload
- **Request Logging**: Central audit trail
- **Request/Response Transform**: Adapt formats if needed
- **CORS**: Single place to configure

### 3. Load Balancing
Gateway can:
- Distribute requests across service instances
- Health check services
- Route around failed instances
- Auto-scale services independently

### 4. API Versioning
Gateway can:
- Route v1 requests to v1 service
- Route v2 requests to v2 service
- Gradually migrate clients
- Support multiple versions simultaneously

### 5. Security
Gateway provides:
- SSL/TLS termination (encrypt in transit)
- WAF (Web Application Firewall) rules
- DDoS protection
- API key validation

## Consequences

### Positive
- ✅ Single entry point for clients
- ✅ Centralized cross-cutting concerns
- ✅ Easy API versioning
- ✅ Better security (centralized)
- ✅ Load balancing
- ✅ Rate limiting
- ✅ Easier monitoring (one place)

### Negative
- ❌ Additional infrastructure component
- ❌ Gateway becomes potential bottleneck
- ❌ Gateway is single point of failure (needs HA)
- ❌ Added latency (all requests through gateway)
- ❌ Gateway configuration complexity
- ❌ Increased operational overhead

## Alternatives Considered

### 1. No Gateway (Direct Service Calls)
- **Pros**: Simple, fewer components
- **Cons**: Clients must know all service URLs, no centralized concerns, complex
- **Why not**: Doesn't scale, poor DX

### 2. Simple Load Balancer
- **Pros**: Simple, lightweight
- **Cons**: Doesn't handle routing, auth, versioning
- **Why not**: Insufficient for our needs

### 3. Service Mesh (Istio, Linkerd)
- **Pros**: Advanced routing, retry logic, circuit breaking
- **Cons**: Overkill for our current needs, steep learning curve
- **Why not**: API Gateway simpler; can add service mesh later if needed

## Related ADRs

- **Depends on**: ADR-001 (Microservices need gateway)
- **Depends on**: ADR-003 (JWT validation at gateway)
- **Related to**: ADR-004 (Gateway runs in Kubernetes)
- **Related to**: ADR-006 (Coordinates with event-driven architecture)

## Implementation Details

### Gateway Architecture

```
Internet
   ↓
API Gateway (HTTPS, 443)
   ├→ /auth/*        → Auth Service (8081)
   ├→ /training/*    → Training Service (8082)
   ├→ /tracking/*    → Tracking Service (8083)
   └→ /notifications/* → Notification Service (8084)
```

### Request Flow

```
1. Client sends request to gateway
2. Gateway validates JWT
3. Gateway checks rate limits
4. Gateway routes to appropriate service
5. Service processes request
6. Gateway returns response to client
7. Gateway logs request/response
```

### Rate Limiting Strategy

```
Per API Key:
- 1000 requests/minute
- 100 requests/second burst

Per User:
- 100 API calls/hour

Special Limits:
- Public endpoints: 1000 requests/hour
- Authenticated endpoints: 10000 requests/hour
```

### Configuration Example

```yaml
routes:
  - path: /auth/*
    service: auth-service
    port: 8081
    
  - path: /training/*
    service: training-service
    port: 8082
    rateLimit: 1000/minute
    
  - path: /tracking/*
    service: tracking-service
    port: 8083
    
  - path: /notifications/*
    service: notification-service
    port: 8084
    rateLimit: 500/minute
    
globals:
  authentication: jwt
  timeout: 30s
  cors: true
```

### Gateway HA Setup

```
Load Balancer
   ↙         ↘
Gateway-1   Gateway-2
   ↓         ↓
Services
```

## Monitoring & Alerting

Alert on:
- Gateway response time > 500ms
- Gateway error rate > 5%
- Gateway CPU > 80%
- Service connectivity issues

## Future Considerations

- Add API versioning headers
- Add request/response transformation rules
- Add API documentation/OpenAPI integration
- Consider adding Developer Portal for API documentation
- Add GraphQL support if needed

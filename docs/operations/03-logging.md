# Logging

## Overview

Comprehensive logging strategy for Gym Platform microservices including log aggregation, centralized logging, log levels, and log analysis for debugging and monitoring.

**Logging Stack:**
- SLF4J (logging facade)
- Logback (logging implementation)
- ELK Stack (Elasticsearch, Logstash, Kibana)
- Structured logging (JSON format)

## Logging Architecture

```
┌────────────────────────────────────────────────────┐
│ Gym Microservices                                  │
│ (All services output logs to STDOUT + files)       │
└──────────────────┬─────────────────────────────────┘
                   │
         ┌─────────▼─────────┐
         │   Docker Logs     │
         │ (json-file driver)│
         └─────────┬─────────┘
                   │
         ┌─────────▼─────────────┐
         │ Filebeat/Fluentd      │
         │ (log shipper)         │
         └─────────┬─────────────┘
                   │
         ┌─────────▼──────────────┐
         │     Logstash           │
         │ (parse & transform)    │
         └─────────┬──────────────┘
                   │
         ┌─────────▼──────────────┐
         │   Elasticsearch        │
         │ (storage & indexing)   │
         └─────────┬──────────────┘
                   │
         ┌─────────▼──────────────┐
         │      Kibana            │
         │ (visualization & search)
         └────────────────────────┘
```

## Application Logging Configuration

### logback-spring.xml

Shared configuration in `gym-common/src/main/resources/logback-spring.xml`, inherited by all services automatically via classpath.

Log pattern includes MDC fields for distributed tracing:
```
%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level [%property{appName}] [traceId=%X{traceId:-}] [spanId=%X{spanId:-}] [userId=%X{userId:-}] %logger{36} - %msg%n
```

**Spring profiles:**
- `!prod` (default/dev): console only, `DEBUG` for `com.gym`
- `prod`: console + rolling file (`logs/{appName}.log`, 100MB/30 days), `INFO`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <springProperty scope="context" name="appName" source="spring.application.name" defaultValue="gym-service"/>

    <property name="LOG_PATTERN"
              value="%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level [${appName}] [traceId=%X{traceId:-}] [spanId=%X{spanId:-}] [userId=%X{userId:-}] %logger{36} - %msg%n"/>

    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder><pattern>${LOG_PATTERN}</pattern></encoder>
    </appender>

    <springProfile name="!prod">
        <logger name="com.gym" level="DEBUG"/>
        <root level="INFO"><appender-ref ref="CONSOLE"/></root>
    </springProfile>

    <springProfile name="prod">
        <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
            <file>logs/${appName}.log</file>
            <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
                <fileNamePattern>logs/${appName}.%d{yyyy-MM-dd}.%i.log.gz</fileNamePattern>
                <maxFileSize>100MB</maxFileSize>
                <maxHistory>30</maxHistory>
            </rollingPolicy>
            <encoder><pattern>${LOG_PATTERN}</pattern></encoder>
        </appender>
        <root level="INFO">
            <appender-ref ref="CONSOLE"/>
            <appender-ref ref="FILE"/>
        </root>
    </springProfile>
</configuration>
```

## MDC — Distributed Tracing

MDC fields are populated automatically by `GymMdcFilter` and `GymRoleInterceptor` from `gym-common`, registered via `GymMdcAutoConfiguration`.

| MDC Key | Source | Description |
|---------|--------|-------------|
| `traceId` | `X-Trace-Id` request header (or generated UUID) | Correlates a request across all services |
| `spanId` | `X-Span-Id` request header (or generated UUID) | Identifies a single service hop |
| `userId` | `X-User-Id` request header (injected by gateway) | Authenticated user ID |

`GymMdcFilter` runs at `HIGHEST_PRECEDENCE`, propagates `X-Trace-Id` and `X-Span-Id` back in response headers, and clears MDC in `finally`.

`GymRoleInterceptor` adds `userId` to MDC and populates `UserContextHolder`. Both are excluded from `/actuator/**` paths.

### Searching logs by trace

```bash
# Follow a request across services by traceId
grep "traceId=abc-123" auth-service.log training-service.log

# Docker: filter by traceId
docker-compose logs | grep "traceId=abc-123"
```

### Using MDC in application code

```java
// MDC is already populated by the filter/interceptor.
// Just use @Slf4j and log normally — traceId/spanId/userId appear automatically.
log.info("Processing request");  
// Output: ... [traceId=abc-123] [spanId=def-456] [userId=789] ... Processing request
```

## ELK Stack Setup

### Docker Compose Configuration

```yaml
version: '3.8'

services:
  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:8.0.0
    container_name: gym-elasticsearch
    environment:
      - discovery.type=single-node
      - xpack.security.enabled=false
      - xpack.security.enrollment.enabled=false
      - ELASTIC_PASSWORD=${ELASTIC_PASSWORD:-changeMe}
    ports:
      - "9200:9200"
    volumes:
      - elasticsearch_data:/usr/share/elasticsearch/data
    networks:
      - gym-network
    restart: unless-stopped

  logstash:
    image: docker.elastic.co/logstash/logstash:8.0.0
    container_name: gym-logstash
    volumes:
      - ./logstash/pipeline:/usr/share/logstash/pipeline:ro
      - ./logstash/patterns:/usr/share/logstash/patterns:ro
    environment:
      LS_JAVA_OPTS: "-Xmx256m -Xms256m"
    ports:
      - "5000:5000/udp"
      - "9600:9600"
    depends_on:
      - elasticsearch
    networks:
      - gym-network
    restart: unless-stopped

  kibana:
    image: docker.elastic.co/kibana/kibana:8.0.0
    container_name: gym-kibana
    environment:
      ELASTICSEARCH_HOSTS: http://elasticsearch:9200
      ELASTICSEARCH_USERNAME: elastic
      ELASTICSEARCH_PASSWORD: ${ELASTIC_PASSWORD:-changeMe}
    ports:
      - "5601:5601"
    depends_on:
      - elasticsearch
    networks:
      - gym-network
    restart: unless-stopped

  filebeat:
    image: docker.elastic.co/beats/filebeat:8.0.0
    container_name: gym-filebeat
    user: root
    volumes:
      - ./filebeat/filebeat.yml:/usr/share/filebeat/filebeat.yml:ro
      - /var/lib/docker/containers:/var/lib/docker/containers:ro
      - /var/run/docker.sock:/var/run/docker.sock:ro
    environment:
      ELASTICSEARCH_HOST: elasticsearch
      ELASTICSEARCH_PORT: "9200"
    depends_on:
      - elasticsearch
    networks:
      - gym-network
    restart: unless-stopped
```

### Logstash Pipeline Configuration

**logstash/pipeline/gym.conf:**
```
input {
  tcp {
    port => 5000
    codec => json
  }
  
  beats {
    port => 5044
  }
}

filter {
  # Parse JSON logs
  if [message] {
    json {
      source => "message"
    }
  }

  # Add timestamp
  date {
    match => [ "timestamp", "ISO8601" ]
    target => "@timestamp"
  }

  # Mutate fields
  mutate {
    add_field => {
      "environment" => "${LOG_ENVIRONMENT:production}"
      "cluster" => "gym-platform"
    }
    remove_field => ["message"]
  }

  # Grok patterns for parsing
  if [type] == "app" {
    grok {
      match => { "message" => "%{TIMESTAMP_ISO8601:timestamp} \[%{DATA:thread}\] %{LOGLEVEL:level} %{DATA:logger} - %{GREEDYDATA:msg}" }
    }
  }
}

output {
  elasticsearch {
    hosts => ["elasticsearch:9200"]
    index => "gym-logs-%{+YYYY.MM.dd}"
  }

  # Send errors to separate index
  if [level] == "ERROR" {
    elasticsearch {
      hosts => ["elasticsearch:9200"]
      index => "gym-errors-%{+YYYY.MM.dd}"
    }
  }
}
```

### Filebeat Configuration

**filebeat/filebeat.yml:**
```yaml
filebeat.inputs:
  - type: container
    paths:
      - '/var/lib/docker/containers/*/*.log'
    processors:
      - add_docker_metadata:
          host: "unix:///var/run/docker.sock"
      - add_kubernetes_metadata:
          in_cluster: false

processors:
  - add_host_metadata:
      when.not.regexp.re: '\.image\.name: ghcr\.io/logz-io/logz-io-k8s-quickstart'
  - add_log_metadata:
  - add_fields:
      target: fields
      fields:
        service: gym-platform
        environment: production

output.elasticsearch:
  enabled: false

output.logstash:
  hosts:
    - "logstash:5044"

logging.level: info
```

## Log Levels

| Level | Usage | Example |
|-------|-------|---------|
| **TRACE** | Very detailed diagnostic info | Variable values in loops |
| **DEBUG** | Development/debugging info | Service method calls |
| **INFO** | General informational messages | User login, data creation |
| **WARN** | Warning messages | Deprecated API use, slow queries |
| **ERROR** | Error events that might still allow continuation | Database connection failed |
| **FATAL** | Very serious error events that likely cause shutdown | Out of memory, critical failure |

## Log Queries in Kibana

### Search Examples

```
# Find all errors in last hour
level:"ERROR" AND @timestamp:[now-1h TO now]

# Find errors by service
level:"ERROR" AND service:"auth-service"

# Find slow requests
response_time_ms:>1000

# Find database errors
message:"SQLException" OR message:"Connection refused"

# Find by request ID
requestId:"550e8400-e29b-41d4-a716-446655440000"

# Find exceptions
error.message:* AND message:"Exception"

# Authentication failures
operation:"login" AND level:"ERROR"
```

### Custom Dashboards

**Errors Dashboard:**
- Error rate over time
- Top 10 most common errors
- Error distribution by service
- Error response times

**Performance Dashboard:**
- Request latency percentiles
- Database query duration
- Cache hit rate
- GC pause times

## Log Retention Policy

```yaml
# Elasticsearch Index Lifecycle Management
PUT _ilm/policy/gym-logs-policy
{
  "policy": "gym-logs-policy",
  "phases": {
    "hot": {
      "min_age": "0d",
      "actions": {
        "rollover": {
          "max_primary_shard_size": "50GB"
        }
      }
    },
    "warm": {
      "min_age": "3d",
      "actions": {
        "set_priority": {
          "priority": 50
        }
      }
    },
    "cold": {
      "min_age": "30d",
      "actions": {
        "set_priority": {
          "priority": 0
        }
      }
    },
    "delete": {
      "min_age": "90d",
      "actions": {
        "delete": {}
      }
    }
  }
}
```

## Logging Best Practices

1. **Use appropriate log levels** - INFO for business events, DEBUG for diagnostics
2. **Include context** - Use MDC for request IDs, user IDs, operation names
3. **Structured logging** - Use JSON format for easier parsing
4. **Avoid sensitive data** - Never log passwords, PII, or sensitive tokens
5. **Log at boundaries** - Log when entering/leaving services
6. **Include correlation IDs** - Track requests across services
7. **Use async appenders** - Don't block application for logging
8. **Rotate logs** - Use rolling file appenders
9. **Alert on errors** - Create dashboards for critical errors
10. **Monitor log volume** - Prevent disk space exhaustion

## Troubleshooting Logs

```bash
# Tail logs from specific service
docker-compose logs -f auth-service

# Tail logs with timestamps
docker-compose logs --timestamps -f auth-service

# View only errors
docker-compose logs -f auth-service 2>&1 | grep ERROR

# Count log lines per service
docker-compose logs | wc -l

# Search logs locally
grep "ERROR" auth-service.log | wc -l

# View logs from specific time range
journalctl --since "2024-01-15 10:00:00" --until "2024-01-15 11:00:00"
```

## Key References

- [SLF4J Documentation](http://www.slf4j.org/)
- [Logback Documentation](https://logback.qos.ch/)
- [Elasticsearch Documentation](https://www.elastic.co/guide/en/elasticsearch/reference/current/index.html)
- [Kibana Query Language](https://www.elastic.co/guide/en/kibana/current/kuery.html)
- See also: [docs/operations/02-monitoring.md](02-monitoring.md)
- See also: [docs/troubleshooting/](../troubleshooting/)

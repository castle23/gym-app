# ADR-006: Event-Driven Architecture

## Status
Accepted

## Date
2026-03-21

## Context

With microservices (ADR-001), services need to communicate and stay synchronized:

1. **Loose Coupling**: Services shouldn't directly depend on each other
2. **Async Communication**: Real-time synchronous calls create bottlenecks
3. **Event Notification**: When something happens in one service, others need to know
4. **Scalability**: Should handle high-throughput event streams
5. **Reliability**: Events shouldn't be lost
6. **Examples**:
   - User created in Auth → notify Training Service (send welcome emails)
   - Workout completed in Training → notify Tracking Service (update stats)
   - Weight updated in Tracking → notify Training Service (adjust recommendations)

Traditional approaches (direct API calls) create tight coupling and availability issues.

## Decision

We adopted **event-driven architecture** using message queues:

1. **Message Queue**: RabbitMQ or Apache Kafka for event distribution
2. **Event Producers**: Services publish events when things happen
3. **Event Consumers**: Other services subscribe to events they care about
4. **Event Format**: Standardized JSON events with versioning
5. **Delivery Guarantees**: At-least-once delivery semantics

## Rationale

### 1. Loose Coupling
Services don't know about each other:

```
OLD (Tight Coupling):
  Auth Service → POST /training/users/new → Training Service
  (If Training Service is down, Auth fails)

NEW (Event-Driven):
  Auth Service → publish UserCreated event → message queue
  Training Service → subscribes to UserCreated → processes independently
  (If Training Service is down, event waits, doesn't affect Auth)
```

### 2. Asynchronous Processing
Events allow async workflows:
- User creation → eventually welcome email sent
- Workout completed → eventually stats updated
- No waiting for dependent services

### 3. Scalability
Event-driven enables:
- Independent scaling of producers and consumers
- Multiple consumers for same event
- Replay of events if needed
- High-throughput communication

### 4. Reliability
Message queues provide:
- Durability (events persisted)
- Retry logic
- Dead letter queues for failed events
- Ordering guarantees (per partition)

### 5. Event Sourcing
As a bonus, event stream becomes audit log:
- Full history of what happened
- Can replay events to rebuild state
- Debugging and compliance

## Consequences

### Positive
- ✅ Loose coupling between services
- ✅ Asynchronous, non-blocking communication
- ✅ Scalable to many services and events
- ✅ Natural audit trail (events)
- ✅ Can replay events
- ✅ Supports high throughput

### Negative
- ❌ Added operational complexity (message broker)
- ❌ Harder to debug (async, distributed)
- ❌ Eventual consistency (not immediate)
- ❌ Duplicate event handling needed
- ❌ Message broker is single point of failure (needs HA)
- ❌ Learning curve for event-driven thinking

## Alternatives Considered

### 1. Direct API Calls (Synchronous)
- **Pros**: Simple, immediate feedback
- **Cons**: Tight coupling, cascading failures, hard to scale
- **Why not**: Doesn't work well with microservices

### 2. Polling
- **Pros**: Simple to implement
- **Cons**: Wasteful, high latency, hard to scale
- **Why not**: Inefficient compared to events

### 3. Service Mesh Communication
- **Pros**: Handles retries, circuit breaking
- **Cons**: Only for synchronous calls, doesn't solve coupling
- **Why not**: Complementary to events, not an alternative

## Related ADRs

- **Depends on**: ADR-001 (Needed for microservices)
- **Related to**: ADR-007 (API Gateway coordinates events)
- **Related to**: ADR-011 (Event security and encryption)

## Implementation Details

### Event Format

```json
{
  "eventId": "evt-123456",
  "eventType": "UserCreated",
  "version": 1,
  "timestamp": "2026-03-21T10:30:00Z",
  "source": "auth-service",
  "data": {
    "userId": "usr-789",
    "email": "user@example.com",
    "firstName": "John",
    "lastName": "Doe"
  },
  "metadata": {
    "correlationId": "corr-456",
    "traceId": "trace-789"
  }
}
```

### Event Types (Examples)

**Auth Service:**
- UserCreated
- UserAuthenticated
- PasswordChanged

**Training Service:**
- ExerciseAdded
- WorkoutStarted
- WorkoutCompleted

**Tracking Service:**
- WeightLogged
- MealLogged
- StatsUpdated

**Notification Service:**
- NotificationSent
- EmailDelivered

### Event Consumers Pattern

```java
@EventListener
public void handleUserCreated(UserCreatedEvent event) {
    // Add user to Training Service
    trainingService.createUserProfile(event.getUserId());
    
    // Log analytics
    analyticsService.trackEvent("user_created", event.getUserId());
}
```

### Error Handling

1. **Retry**: Automatically retry failed messages (configurable backoff)
2. **Dead Letter Queue**: Move messages that fail repeatedly
3. **Idempotency**: Handle duplicate events gracefully
4. **Monitoring**: Alert on dead letter queue growth

## Deployment Considerations

1. **Message Broker HA**: RabbitMQ cluster or Kafka broker replication
2. **Consumer Groups**: Multiple instances consume same events
3. **Partitioning**: Events partitioned by key for ordering
4. **Monitoring**: Track event lag, consumer health

## Future Considerations

- Consider CQRS (Command Query Responsibility Segregation) if read/write patterns diverge
- Consider event store for full event sourcing
- Consider Kafka if we need extremely high throughput
- Consider stream processing (Kafka Streams) for complex event workflows

# Phase 6: Notification Service - Complete Implementation

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Complete Notification Service with Firebase Cloud Messaging integration, achieving 85%+ test coverage.

**Architecture:** Event-driven notifications with push tokens management and Firebase integration.

**Tech Stack:** Spring Boot 3.2.0, Firebase Admin SDK, JUnit 5, Mockito, TestContainers

---

## Entities to Create

1. **Notification**
   - id: Long
   - userId: Long (recipient)
   - title: String
   - body: String
   - type: NotificationType (WORKOUT_REMINDER, ACHIEVEMENT, MESSAGE, ALERT, OTHER)
   - isRead: Boolean
   - createdAt: LocalDateTime
   - sentAt: LocalDateTime (nullable)

2. **PushToken**
   - id: Long
   - userId: Long
   - token: String (Firebase token)
   - deviceType: String (iOS, Android, Web)
   - isActive: Boolean
   - lastUsedAt: LocalDateTime
   - createdAt: LocalDateTime

3. **NotificationPreference**
   - id: Long
   - userId: Long
   - notificationType: NotificationType
   - isEnabled: Boolean
   - quietHoursStart: LocalTime (nullable)
   - quietHoursEnd: LocalTime (nullable)

---

## Implementation Steps (Abbreviated)

### Step 1-2: Create Entities with Annotations

```java
@Entity
@Table(name = "notifications")
public class Notification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private Long userId;
    
    @Column(nullable = false)
    private String title;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;
    
    @Enumerated(EnumType.STRING)
    private NotificationType type;
    
    @Column(nullable = false)
    private Boolean isRead = false;
    
    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @Column
    private LocalDateTime sentAt;
    
    // ... getters, setters, builder
}

// Similar for PushToken and NotificationPreference
```

### Step 2-3: Create Repositories

```java
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserId(Long userId);
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Notification> findByUserIdAndIsRead(Long userId, Boolean isRead);
}

@Repository
public interface PushTokenRepository extends JpaRepository<PushToken, Long> {
    List<PushToken> findByUserIdAndIsActive(Long userId, Boolean isActive);
    Optional<PushToken> findByToken(String token);
}

@Repository
public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, Long> {
    Optional<NotificationPreference> findByUserIdAndNotificationType(Long userId, NotificationType type);
    List<NotificationPreference> findByUserId(Long userId);
}
```

### Step 3-4: Create Firebase Configuration

```java
@Configuration
public class FirebaseConfig {
    
    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        ClassPathResource serviceAccountFile = new ClassPathResource("firebase-config.json");
        
        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccountFile.getInputStream()))
                .setProjectId("your-project-id")
                .build();
        
        return FirebaseApp.initializeApp(options);
    }
    
    @Bean
    public FirebaseMessaging firebaseMessaging(FirebaseApp firebaseApp) {
        return FirebaseMessaging.getInstance(firebaseApp);
    }
}
```

### Step 4-5: Create DTOs

```java
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class NotificationDTO {
    private Long id;
    private Long userId;
    private String title;
    private String body;
    private NotificationType type;
    private Boolean isRead;
    private LocalDateTime createdAt;
    private LocalDateTime sentAt;
}

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class NotificationRequestDTO {
    @NotBlank private String title;
    @NotBlank private String body;
    @NotNull private NotificationType type;
}

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PushTokenDTO {
    private Long id;
    private String token;
    private String deviceType;
    private Boolean isActive;
    private LocalDateTime lastUsedAt;
}

// Similar for other DTOs
```

### Step 5-6: Create Services

```java
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class NotificationService {
    
    private final NotificationRepository notificationRepository;
    private final FirebaseMessaging firebaseMessaging;
    private final PushTokenRepository pushTokenRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    
    /**
     * Send notification to user via Firebase
     */
    public void sendNotification(Long userId, NotificationRequestDTO request) {
        log.info("Sending notification to user: {}", userId);
        
        // Check user preferences
        if (!isNotificationEnabled(userId, request.getType())) {
            log.info("User {} has disabled notifications for type: {}", userId, request.getType());
            return;
        }
        
        // Get user's push tokens
        List<PushToken> tokens = pushTokenRepository.findByUserIdAndIsActive(userId, true);
        
        // Send via Firebase
        for (PushToken token : tokens) {
            try {
                Message message = Message.builder()
                        .setToken(token.getToken())
                        .setNotification(com.google.firebase.messaging.Notification.builder()
                                .setTitle(request.getTitle())
                                .setBody(request.getBody())
                                .build())
                        .build();
                
                String messageId = firebaseMessaging.send(message);
                log.info("Message {} sent to device token", messageId);
                
                token.setLastUsedAt(LocalDateTime.now());
                pushTokenRepository.save(token);
                
            } catch (FirebaseMessagingException e) {
                log.error("Failed to send message to token {}: {}", token.getToken(), e.getMessage());
                if (e.getMessagingErrorCode() == MessagingErrorCode.INVALID_ARGUMENT) {
                    token.setIsActive(false);
                    pushTokenRepository.save(token);
                }
            }
        }
        
        // Save notification record
        Notification notification = Notification.builder()
                .userId(userId)
                .title(request.getTitle())
                .body(request.getBody())
                .type(request.getType())
                .isRead(false)
                .sentAt(LocalDateTime.now())
                .build();
        notificationRepository.save(notification);
    }
    
    /**
     * Register push token for a device
     */
    public void registerPushToken(Long userId, String token, String deviceType) {
        log.info("Registering push token for user: {}", userId);
        
        PushToken pushToken = pushTokenRepository.findByToken(token)
                .orElseGet(() -> PushToken.builder()
                        .userId(userId)
                        .token(token)
                        .deviceType(deviceType)
                        .isActive(true)
                        .createdAt(LocalDateTime.now())
                        .build());
        
        pushToken.setIsActive(true);
        pushToken.setLastUsedAt(LocalDateTime.now());
        pushTokenRepository.save(pushToken);
    }
    
    /**
     * Get user notifications
     */
    public List<NotificationDTO> getUserNotifications(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Mark notification as read
     */
    public void markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found"));
        
        if (!notification.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Unauthorized");
        }
        
        notification.setIsRead(true);
        notificationRepository.save(notification);
    }
    
    /**
     * Delete notification
     */
    public void deleteNotification(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found"));
        
        if (!notification.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Unauthorized");
        }
        
        notificationRepository.delete(notification);
    }
    
    private boolean isNotificationEnabled(Long userId, NotificationType type) {
        return preferenceRepository.findByUserIdAndNotificationType(userId, type)
                .map(NotificationPreference::getIsEnabled)
                .orElse(true); // Default: enabled
    }
    
    private NotificationDTO toDTO(Notification notification) {
        return NotificationDTO.builder()
                .id(notification.getId())
                .userId(notification.getUserId())
                .title(notification.getTitle())
                .body(notification.getBody())
                .type(notification.getType())
                .isRead(notification.getIsRead())
                .createdAt(notification.getCreatedAt())
                .sentAt(notification.getSentAt())
                .build();
    }
}

// Similar services for: PushTokenService, NotificationPreferenceService
```

### Step 6-7: Create Controllers

```java
@RestController
@RequestMapping("/api/v1/notifications")
@Slf4j
@RequiredArgsConstructor
public class NotificationController {
    
    private final NotificationService notificationService;
    
    @GetMapping
    public ResponseEntity<List<NotificationDTO>> getNotifications(
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(notificationService.getUserNotifications(userId));
    }
    
    @PostMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId) {
        try {
            notificationService.markAsRead(id, userId);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId) {
        try {
            notificationService.deleteNotification(id, userId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}

@RestController
@RequestMapping("/api/v1/push-tokens")
@Slf4j
@RequiredArgsConstructor
public class PushTokenController {
    
    private final PushTokenService pushTokenService;
    
    @PostMapping
    public ResponseEntity<Void> registerToken(
            @RequestBody Map<String, String> request,
            @RequestHeader("X-User-Id") Long userId) {
        String token = request.get("token");
        String deviceType = request.get("deviceType");
        pushTokenService.registerToken(userId, token, deviceType);
        return ResponseEntity.ok().build();
    }
}
```

### Step 7-8: Create Unit Tests (60+ tests)

```java
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {
    
    @Mock private NotificationRepository notificationRepository;
    @Mock private FirebaseMessaging firebaseMessaging;
    @Mock private PushTokenRepository pushTokenRepository;
    @Mock private NotificationPreferenceRepository preferenceRepository;
    
    @InjectMocks private NotificationService notificationService;
    
    // Test 1: Send notification successfully
    @Test
    void testSendNotification() { /* ... */ }
    
    // Test 2: Notification disabled by preference
    @Test
    void testSendNotificationDisabled() { /* ... */ }
    
    // Test 3: Invalid token handling
    @Test
    void testSendNotificationInvalidToken() { /* ... */ }
    
    // ... 57+ more tests covering all methods and edge cases
}
```

### Step 8-9: Create Integration Tests

- TestContainers PostgreSQL setup
- Repository tests
- Firebase mock/stub for testing
- End-to-end notification flow tests

### Step 9-10: Verify Coverage & Commit

Run: `mvn clean test jacoco:report -pl notification-service`
Target: 85%+ coverage

```bash
git add notification-service/
git commit -m "feat: complete Phase 6 - Notification Service with Firebase and 85%+ coverage"
```

---

## Firebase Setup Notes

1. **Create Firebase Project:**
   - Go to https://console.firebase.google.com
   - Create new project
   - Download service account JSON key
   - Save to `notification-service/src/main/resources/firebase-config.json`

2. **Environment Variables:**
   - Add to `.env`:
     ```
     FIREBASE_PROJECT_ID=your-project-id
     FIREBASE_CONFIG_PATH=/path/to/firebase-config.json
     ```

3. **pom.xml Dependency:**
   ```xml
   <dependency>
       <groupId>com.google.firebase</groupId>
       <artifactId>firebase-admin</artifactId>
       <version>9.1.0</version>
   </dependency>
   ```

---

## Expected Output

✅ 3 Entities created (Notification, PushToken, NotificationPreference)
✅ 3 Repositories created and tested
✅ 3 DTOs created (Request + Response for each)
✅ 3 Services created with full functionality
✅ 2 Controllers created with REST endpoints
✅ 60+ Unit tests with 85%+ coverage
✅ Integration tests with TestContainers
✅ Firebase Cloud Messaging integration working
✅ All tests passing
✅ Code committed to git

**Notification Service is now COMPLETE and PRODUCTION-READY**

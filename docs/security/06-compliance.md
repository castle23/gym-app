# Compliance

## Overview

The Gym Platform operates in compliance with data protection regulations including GDPR (General Data Protection Regulation), CCPA (California Consumer Privacy Act), and other regional privacy laws. This guide covers compliance requirements, data retention policies, audit logging, privacy-by-design principles, data processing agreements, and breach notification procedures for the Gym Platform's microservices architecture.

Compliance is a shared responsibility across development, operations, and legal teams. The Gym Platform enforces compliance controls at the application, database, and infrastructure levels.

## Table of Contents

- [GDPR Compliance](#gdpr-compliance)
- [Data Retention Policies](#data-retention-policies)
- [Privacy by Design](#privacy-by-design)
- [Audit Logging and Monitoring](#audit-logging-and-monitoring)
- [Data Processing Agreements](#data-processing-agreements)
- [Breach Notification](#breach-notification)
- [Data Subject Rights](#data-subject-rights)
- [Compliance Checklist](#compliance-checklist)
- [Best Practices](#best-practices)

---

## GDPR Compliance

### GDPR Principles

The Gym Platform implements the six GDPR principles for data processing:

#### 1. Lawfulness, Fairness, and Transparency

**Legal Basis:** Processing must have a lawful basis from Article 6:

```yaml
lawful-bases:
  consent:
    description: "Explicit user consent for data processing"
    example: "User opt-in for marketing emails"
    withdrawable: true
  contract:
    description: "Processing necessary to fulfill a contract"
    example: "Process workout data to provide training service"
    withdrawable: false
  legal-obligation:
    description: "Legal requirement"
    example: "Tax reporting, fraud prevention"
    withdrawable: false
  vital-interests:
    description: "Protect vital interests"
    example: "Emergency health information"
    withdrawable: false
  public-task:
    description: "Task in the public interest"
    example: "Health statistics research"
    withdrawable: false
  legitimate-interest:
    description: "Legitimate business interests"
    example: "Fraud detection, platform security"
    withdrawable: true
```

**Implementation:**
```java
@Entity
@Table(name = "data_processing")
public class DataProcessingRecord {

    @Id
    private Long id;

    @Enumerated(EnumType.STRING)
    private LegalBasis legalBasis;

    @Column(nullable = false)
    private String processingPurpose;

    @Column(nullable = false)
    private LocalDateTime consentDate;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime revokedAt;

    @Column(length = 1000)
    private String consentDetails;

    // Getters and setters
}

public enum LegalBasis {
    CONSENT("User Consent"),
    CONTRACT("Contract Performance"),
    LEGAL_OBLIGATION("Legal Obligation"),
    VITAL_INTERESTS("Vital Interests"),
    PUBLIC_TASK("Public Task"),
    LEGITIMATE_INTEREST("Legitimate Interest");

    private final String description;

    LegalBasis(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
```

#### 2. Purpose Limitation

Data processing must be for specified, explicit, and legitimate purposes:

```java
@Component
public class PurposeLimitationEnforcer {

    private static final Map<String, Set<String>> ALLOWED_PURPOSES = Map.of(
        "user_id", Set.of("authentication", "authorization", "workout_tracking"),
        "email", Set.of("authentication", "notifications", "password_reset"),
        "health_data", Set.of("workout_tracking", "health_insights", "performance_analysis"),
        "location_data", Set.of("distance_tracking", "route_analysis"),
        "payment_info", Set.of("billing", "subscription_management")
    );

    public boolean isPurposeAllowed(String dataField, String purpose) {
        Set<String> allowedPurposes = ALLOWED_PURPOSES.getOrDefault(dataField, Set.of());
        return allowedPurposes.contains(purpose);
    }

    public void validateDataAccess(String dataField, String requestingService, String purpose) {
        if (!isPurposeAllowed(dataField, purpose)) {
            throw new PurposeLimitationViolation(
                String.format("Service %s cannot access %s for purpose %s",
                    requestingService, dataField, purpose));
        }
        
        auditLog(dataField, requestingService, purpose);
    }

    private void auditLog(String dataField, String requestingService, String purpose) {
        // Log all data access attempts
    }
}
```

#### 3. Data Minimization

Collect only data necessary for specified purposes:

```java
public class CreateUserRequest {

    @NotBlank(message = "Email is required")
    @Email
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 12, message = "Minimum 12 characters")
    private String password;

    @NotBlank(message = "Full name is required")
    private String fullName;

    // Only collect data essential for service
    // Don't collect: social security number, full history, etc.

    public static class DataMinimizationValidator {
        
        public static void validate(CreateUserRequest request) {
            // Reject requests with unnecessary data fields
            if (request.getAdditionalFields() != null && 
                !request.getAdditionalFields().isEmpty()) {
                throw new DataMinimizationViolation(
                    "Request contains unnecessary data fields");
            }
        }
    }
}
```

#### 4. Accuracy

Keep personal data accurate and up to date:

```java
@Component
public class DataAccuracyEnforcer {

    @Autowired
    private UserRepository userRepository;

    public void validateAndUpdateUserData(Long userId, UpdateUserRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(UserNotFoundException::new);

        // Track changes
        List<AuditEntry> changes = new ArrayList<>();

        if (!user.getEmail().equals(request.getEmail())) {
            changes.add(new AuditEntry("email", user.getEmail(), request.getEmail()));
            user.setEmail(request.getEmail());
        }

        // Verify data before updating
        if (validateEmail(request.getEmail())) {
            user.setEmail(request.getEmail());
            user.setLastDataUpdateAt(LocalDateTime.now());
            userRepository.save(user);
            
            auditDataChange(userId, changes);
        }
    }

    private boolean validateEmail(String email) {
        // Verify email is actually correct
        return true;
    }

    private void auditDataChange(Long userId, List<AuditEntry> changes) {
        // Log all data modifications
    }
}
```

#### 5. Integrity and Confidentiality

Keep data secure and protected:

See [Data Security](04-data-security.md) for encryption, access controls, and secure deletion.

#### 6. Accountability

Demonstrate compliance with GDPR:

```java
@Component
public class ComplianceReportingService {

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Autowired
    private DataProcessingRecordRepository processingRecordRepository;

    public ComplianceReport generateAnnualReport(LocalDate year) {
        ComplianceReport report = new ComplianceReport();

        // 1. Data Processing Inventory
        report.setDataInventory(
            processingRecordRepository.findByYear(year).stream()
                .collect(Collectors.groupingBy(DataProcessingRecord::getPurpose)));

        // 2. Audit Trail
        List<AuditEvent> auditTrail = auditEventRepository.findByYear(year);
        report.setAuditEvents(auditTrail);

        // 3. Data Subject Rights Requests
        report.setDeletionRequests(countDeletionRequests(year));
        report.setAccessRequests(countAccessRequests(year));
        report.setRectificationRequests(countRectificationRequests(year));

        // 4. Breach Incidents
        report.setSecurityIncidents(getSecurityIncidents(year));

        // 5. Consent Records
        report.setConsentRecords(
            processingRecordRepository.findByConsent(year));

        return report;
    }

    private int countDeletionRequests(LocalDate year) {
        // Count right-to-be-forgotten requests
        return 0;
    }

    private int countAccessRequests(LocalDate year) {
        // Count data access requests
        return 0;
    }

    private int countRectificationRequests(LocalDate year) {
        // Count data correction requests
        return 0;
    }

    private List<SecurityIncident> getSecurityIncidents(LocalDate year) {
        // List all security incidents/breaches
        return List.of();
    }
}
```

### Privacy Notice

Users must be informed about data processing:

```java
@Component
public class PrivacyNoticeService {

    private static final String PRIVACY_NOTICE = """
        Privacy Notice - Gym Platform
        
        1. Data Controller: Gym Platform Inc., contact@gym.com
        
        2. Data Processing:
           - Purpose: Provide fitness training and workout tracking services
           - Legal Basis: User consent and contract performance
           - Data Retention: 3 years after service termination
        
        3. Data Categories:
           - Personal identification (name, email, phone)
           - Health and fitness data (workouts, performance metrics)
           - Usage data (login history, feature usage)
        
        4. Data Recipients:
           - Internal: Training Service, Notification Service
           - External: None (no third-party sharing)
        
        5. Your Rights:
           - Right of access (download your data)
           - Right to rectification (correct inaccurate data)
           - Right to erasure (delete your data)
           - Right to restrict processing
           - Right to data portability
           - Right to object
           - Right to withdraw consent
        
        6. To Exercise Rights:
           Contact privacy@gym.com with "Data Subject Right Request" in the subject
        """;

    public String getPrivacyNotice() {
        return PRIVACY_NOTICE;
    }
}
```

---

## Data Retention Policies

### Retention Schedule

Define how long each data type is retained:

```yaml
data-retention:
  user-profile:
    retention-period: indefinite-until-deletion
    deletion-trigger: account-deletion-request
    backup-retention: 30-days

  workout-data:
    retention-period: 3-years
    deletion-trigger: user-deletion-or-expiration
    backup-retention: 30-days
    archival: yearly-snapshots

  authentication-logs:
    retention-period: 90-days
    deletion-trigger: automatic-expiration
    backup-retention: 1-year

  audit-logs:
    retention-period: 7-years  # Legal/compliance requirement
    deletion-trigger: never
    backup-retention: 10-years

  api-access-logs:
    retention-period: 30-days
    deletion-trigger: automatic-expiration
    backup-retention: 90-days

  payment-data:
    retention-period: 7-years  # Tax compliance
    deletion-trigger: never-for-tax-records
    backup-retention: 10-years

  failed-login-attempts:
    retention-period: 90-days
    deletion-trigger: automatic-expiration
    backup-retention: 1-year
```

**Implementation:**

```java
@Component
public class DataRetentionScheduler {

    @Autowired
    private UserWorkoutRepository workoutRepository;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Scheduled(cron = "0 2 * * * MON")  // Weekly on Monday 2 AM
    public void purgeExpiredData() {
        purgeWorkoutData();
        purgeAuthenticationLogs();
        purgeApiAccessLogs();
        purgeFailedLoginAttempts();
    }

    private void purgeWorkoutData() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusYears(3);
        List<Workout> expiredWorkouts = workoutRepository
            .findByCreatedAtBefore(cutoffDate);

        for (Workout workout : expiredWorkouts) {
            // Archive before deletion
            archiveWorkout(workout);
            
            // Secure deletion (overwrite, zero out)
            workoutRepository.secureDelete(workout.getId());
            
            log.info("Purged workout data for user {}", workout.getUserId());
        }
    }

    private void purgeAuthenticationLogs() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(90);
        int deleted = auditEventRepository
            .deleteByEventTypeAndCreatedAtBefore("LOGIN", cutoffDate);
        
        log.info("Purged {} authentication logs older than 90 days", deleted);
    }

    private void purgeApiAccessLogs() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);
        int deleted = auditEventRepository
            .deleteByEventTypeAndCreatedAtBefore("API_ACCESS", cutoffDate);
        
        log.info("Purged {} API access logs older than 30 days", deleted);
    }

    private void purgeFailedLoginAttempts() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(90);
        int deleted = auditEventRepository
            .deleteByEventTypeAndCreatedAtBefore("FAILED_LOGIN", cutoffDate);
        
        log.info("Purged {} failed login records older than 90 days", deleted);
    }

    private void archiveWorkout(Workout workout) {
        // Archive to long-term storage before deletion
        // E.g., AWS S3, Azure Blob Storage
    }
}
```

### Data Deletion Procedures

Implement secure deletion that overwrites data multiple times:

```java
@Component
public class SecureDataDeletionService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkoutRepository workoutRepository;

    @Autowired
    private AuditEventRepository auditEventRepository;

    /**
     * Securely delete user and all associated data (Right to be Forgotten)
     */
    public void deleteUserData(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(UserNotFoundException::new);

        // 1. Create deletion record for audit trail
        DeletionRecord deletionRecord = new DeletionRecord();
        deletionRecord.setUserId(userId);
        deletionRecord.setUsername(user.getUsername());
        deletionRecord.setEmail(user.getEmail());
        deletionRecord.setDeletionReason("User-initiated deletion request");
        deletionRecord.setDeletionDate(LocalDateTime.now());
        deletionRecord.setDeletedBy(SecurityContextHolder.getContext()
            .getAuthentication().getName());

        // 2. Archive user data
        archiveUserData(user);

        // 3. Delete all user data
        deleteWorkoutData(userId);
        deleteProfileData(userId);
        deletePreferences(userId);
        deleteActivityLogs(userId);

        // 4. Overwrite storage (7-pass Gutmann algorithm)
        performSecureDeletion(userId);

        // 5. Verify deletion
        verifyDataDeletion(userId);

        log.info("Securely deleted all data for user {}", userId);
    }

    private void archiveUserData(User user) {
        String archiveId = UUID.randomUUID().toString();
        // Store encrypted archive for compliance audit trail
    }

    private void deleteWorkoutData(Long userId) {
        workoutRepository.deleteByUserId(userId);
    }

    private void deleteProfileData(Long userId) {
        // Delete personal information, contact details, etc.
    }

    private void deletePreferences(Long userId) {
        // Delete user preferences, settings, etc.
    }

    private void deleteActivityLogs(Long userId) {
        // Delete user activity logs (but keep audit trail for compliance)
    }

    private void performSecureDeletion(Long userId) {
        // Implement 7-pass Gutmann algorithm or similar
        // Overwrite data multiple times with random patterns
    }

    private void verifyDataDeletion(Long userId) {
        // Verify user data is completely deleted
        boolean exists = userRepository.existsById(userId);
        if (exists) {
            throw new DataDeletionVerificationFailedException(
                "User data still exists after deletion");
        }
    }
}
```

---

## Privacy by Design

### Privacy Impact Assessment

Before implementing features that process data, conduct a Privacy Impact Assessment:

```java
public class PrivacyImpactAssessment {

    private String featureName;
    private String description;
    private List<String> dataCategories;
    private String legalBasis;
    private String purpose;
    private List<String> risks;
    private List<String> mitigations;
    private LocalDate assessmentDate;
    private String assessedBy;

    public PrivacyImpactAssessment(String featureName) {
        this.featureName = featureName;
        this.assessmentDate = LocalDate.now();
    }

    public void addDataCategory(String category) {
        dataCategories.add(category);
    }

    public void addRisk(String risk) {
        risks.add(risk);
    }

    public void addMitigation(String mitigation) {
        mitigations.add(mitigation);
    }

    public void validate() {
        if (risks.size() > mitigations.size()) {
            throw new PrivacyRiskUnmitigatedException(
                "Not all privacy risks have mitigations");
        }
    }

    /**
     * Example PIA for new fitness tracking feature
     */
    public static PrivacyImpactAssessment fitnessTrackingExample() {
        PrivacyImpactAssessment pia = new PrivacyImpactAssessment("Real-time Fitness Tracking");
        
        pia.setDescription("Track user real-time location, heart rate, and performance metrics");
        pia.setLegalBasis("User Consent + Contract Performance");
        pia.setPurpose("Provide personalized workout guidance and performance analytics");

        pia.addDataCategory("Location Data");
        pia.addDataCategory("Biometric Data (Heart Rate)");
        pia.addDataCategory("Performance Metrics");

        pia.addRisk("Location data could reveal home address");
        pia.addRisk("Heart rate data is sensitive health information");
        pia.addRisk("Data breach could compromise user privacy");

        pia.addMitigation("Anonymize location data to 100m accuracy");
        pia.addMitigation("Encrypt heart rate data at rest and in transit");
        pia.addMitigation("Implement encryption, access controls, audit logging");

        return pia;
    }
}
```

---

## Audit Logging and Monitoring

### Comprehensive Audit Trail

Log all data processing activities for compliance:

```java
@Entity
@Table(name = "audit_events")
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String action;

    @Column(nullable = false)
    private String dataCategory;

    @Column(length = 500)
    private String details;

    @Column(nullable = false)
    private String ipAddress;

    @Column(nullable = false)
    private String userAgent;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column
    private String result;  // SUCCESS, FAILURE, DENIED

    @Column
    private String reason;

    // Getters and setters
}

@Component
public class AuditLogger {

    @Autowired
    private AuditEventRepository auditEventRepository;

    public void logDataAccess(Long userId, String dataCategory, String purpose) {
        AuditEvent event = new AuditEvent();
        event.setUserId(userId);
        event.setAction("DATA_ACCESS");
        event.setDataCategory(dataCategory);
        event.setDetails(String.format("Accessed %s for %s", dataCategory, purpose));
        event.setTimestamp(LocalDateTime.now());
        event.setResult("SUCCESS");
        event.setIpAddress(getClientIp());
        event.setUserAgent(getClientUserAgent());

        auditEventRepository.save(event);
    }

    public void logDataModification(Long userId, String dataCategory, 
                                     String oldValue, String newValue) {
        AuditEvent event = new AuditEvent();
        event.setUserId(userId);
        event.setAction("DATA_MODIFICATION");
        event.setDataCategory(dataCategory);
        event.setDetails(String.format("Changed %s from %s to %s", 
            dataCategory, oldValue, newValue));
        event.setTimestamp(LocalDateTime.now());
        event.setResult("SUCCESS");

        auditEventRepository.save(event);
    }

    public void logAccessDenied(Long userId, String dataCategory, String reason) {
        AuditEvent event = new AuditEvent();
        event.setUserId(userId);
        event.setAction("ACCESS_DENIED");
        event.setDataCategory(dataCategory);
        event.setDetails("Access attempt was denied");
        event.setResult("DENIED");
        event.setReason(reason);
        event.setTimestamp(LocalDateTime.now());

        auditEventRepository.save(event);
    }

    private String getClientIp() {
        // Implementation to get client IP
        return "0.0.0.0";
    }

    private String getClientUserAgent() {
        // Implementation to get client user agent
        return "";
    }
}
```

### Compliance Monitoring Dashboard

```java
@RestController
@RequestMapping("/api/v1/admin/compliance")
public class ComplianceMonitoringController {

    @Autowired
    private ComplianceReportingService reportingService;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ComplianceDashboard> getComplianceDashboard() {
        ComplianceDashboard dashboard = new ComplianceDashboard();

        // 1. Data access requests
        dashboard.setDataAccessRequests(
            auditEventRepository.countByAction("DATA_ACCESS")
                .stream()
                .collect(Collectors.groupingBy(e -> e.getDataCategory(),
                    Collectors.counting())));

        // 2. Access denied events
        dashboard.setAccessDeniedEvents(
            auditEventRepository.countByResult("DENIED"));

        // 3. Data modifications
        dashboard.setDataModifications(
            auditEventRepository.countByAction("DATA_MODIFICATION")
                .stream()
                .collect(Collectors.groupingBy(e -> e.getDataCategory(),
                    Collectors.counting())));

        // 4. Deletion requests
        dashboard.setDeletionRequests(
            auditEventRepository.countByAction("DATA_DELETION"));

        // 5. Compliance violations
        dashboard.setComplianceViolations(
            auditEventRepository.findViolations());

        return ResponseEntity.ok(dashboard);
    }

    @GetMapping("/audit-trail/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AuditEvent>> getUserAuditTrail(@PathVariable Long userId) {
        return ResponseEntity.ok(
            auditEventRepository.findByUserIdOrderByTimestampDesc(userId));
    }

    @GetMapping("/report")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ComplianceReport> getComplianceReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate year) {
        return ResponseEntity.ok(
            reportingService.generateAnnualReport(year));
    }
}
```

---

## Data Processing Agreements

### Data Processing Addendum (DPA)

For any third-party processors, require a DPA:

```java
@Entity
@Table(name = "data_processing_agreements")
public class DataProcessingAgreement {

    @Id
    private Long id;

    @Column(nullable = false)
    private String processor;  // Third-party company name

    @Column(nullable = false)
    private String description;  // What data is processed

    @Column(length = 2000)
    private String dataCategories;

    @Column(length = 2000)
    private String processingPurposes;

    @Column(nullable = false)
    private LocalDate effectiveDate;

    @Column
    private LocalDate expirationDate;

    @Column(nullable = false)
    private String status;  // ACTIVE, EXPIRED, TERMINATED

    @Column
    private boolean subProcessorsAllowed;

    @Column
    private boolean dataTransferToThirdCountry;

    @Column(length = 500)
    private String safeguards;  // GDPR adequacy decision, standard contractual clauses

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime lastReviewedAt;

    // Getters and setters
}

@Component
public class DataProcessingAgreementService {

    @Autowired
    private DataProcessingAgreementRepository dpaRepository;

    public void validateDpa(String processorName) {
        DataProcessingAgreement dpa = dpaRepository.findByProcessor(processorName)
            .orElseThrow(() -> new NoDpaFoundException(
                "No DPA found for processor: " + processorName));

        if ("EXPIRED".equals(dpa.getStatus())) {
            throw new ExpiredDpaException(
                "DPA for processor " + processorName + " has expired");
        }

        if (dpa.getExpirationDate() != null &&
            dpa.getExpirationDate().isBefore(LocalDate.now())) {
            throw new ExpiredDpaException(
                "DPA for processor " + processorName + " expired on " +
                dpa.getExpirationDate());
        }
    }

    public List<DataProcessingAgreement> getDpasExpiringInDays(int days) {
        LocalDate expirationCutoff = LocalDate.now().plusDays(days);
        return dpaRepository.findByExpirationDateBetweenAndStatus(
            LocalDate.now(), expirationCutoff, "ACTIVE");
    }
}
```

---

## Breach Notification

### Breach Response Procedures

Comply with breach notification requirements (72 hours to authorities):

```java
@Entity
@Table(name = "security_breaches")
public class SecurityBreach {

    @Id
    private Long id;

    @Column(nullable = false)
    private LocalDateTime detectedAt;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private int affectedRecords;

    @Column(length = 500)
    private String dataCategories;

    @Column(length = 1000)
    private String cause;

    @Column(nullable = false)
    private String severity;  // LOW, MEDIUM, HIGH, CRITICAL

    @Column(nullable = false)
    private String status;  // DETECTED, INVESTIGATED, REPORTED, RESOLVED

    @Column
    private LocalDateTime investigationCompletedAt;

    @Column
    private LocalDateTime notifiedAuthoritiesAt;

    @Column
    private LocalDateTime notifiedUsersAt;

    @Column(length = 1000)
    private String remedialActions;

    // Getters and setters
}

@Component
public class BreachNotificationService {

    @Autowired
    private SecurityBreachRepository breachRepository;

    @Autowired
    private NotificationService notificationService;

    /**
     * Log security breach and initiate notification process
     * Must notify authorities within 72 hours
     */
    public void reportBreach(SecurityBreach breach) {
        breach.setDetectedAt(LocalDateTime.now());
        breach.setStatus("DETECTED");

        SecurityBreach saved = breachRepository.save(breach);

        // 1. Immediate containment
        containBreach(breach);

        // 2. Investigation
        investigateBreach(breach);

        // 3. Notify authorities if required (72-hour deadline)
        if (requiresAuthorityNotification(breach)) {
            notifyAuthorities(breach);
        }

        // 4. Notify affected users (high risk only)
        if (requiresUserNotification(breach)) {
            notifyAffectedUsers(breach);
        }
    }

    private void containBreach(SecurityBreach breach) {
        log.warn("Containing breach: {}", breach.getDescription());
        // Disable affected accounts, revoke tokens, etc.
    }

    private void investigateBreach(SecurityBreach breach) {
        log.info("Investigating breach: {}", breach.getDescription());
        // Analyze logs, forensics, root cause analysis
        breach.setInvestigationCompletedAt(LocalDateTime.now());
    }

    private boolean requiresAuthorityNotification(SecurityBreach breach) {
        // Notify if affects more than 250 records or is high severity
        return breach.getAffectedRecords() >= 250 ||
               "CRITICAL".equals(breach.getSeverity());
    }

    private boolean requiresUserNotification(SecurityBreach breach) {
        // Notify if high likelihood of harm
        return "CRITICAL".equals(breach.getSeverity()) ||
               "HIGH".equals(breach.getSeverity());
    }

    private void notifyAuthorities(SecurityBreach breach) {
        try {
            // Contact data protection authority (e.g., ICO in UK, CNIL in France)
            String notification = generateBreachNotification(breach);
            // Send to data protection authority email
            log.info("Notified authorities of breach: {}", breach.getId());
            breach.setNotifiedAuthoritiesAt(LocalDateTime.now());
        } catch (Exception e) {
            log.error("Failed to notify authorities", e);
        }
    }

    private void notifyAffectedUsers(SecurityBreach breach) {
        try {
            // Send notification email to affected users
            String notification = generateUserNotification(breach);
            // Send email via notification service
            log.info("Notified users of breach: {}", breach.getId());
            breach.setNotifiedUsersAt(LocalDateTime.now());
        } catch (Exception e) {
            log.error("Failed to notify affected users", e);
        }
    }

    private String generateBreachNotification(SecurityBreach breach) {
        return String.format("""
            Security Breach Notification - Gym Platform
            
            Detected: %s
            Affected Records: %d
            Data Categories: %s
            Severity: %s
            
            Description: %s
            
            Cause: %s
            
            Remedial Actions: %s
            """,
            breach.getDetectedAt(), breach.getAffectedRecords(),
            breach.getDataCategories(), breach.getSeverity(),
            breach.getDescription(), breach.getCause(),
            breach.getRemedialActions());
    }

    private String generateUserNotification(SecurityBreach breach) {
        return String.format("""
            Security Alert - Your Data Security
            
            We're writing to notify you of a security incident that affected your account.
            
            What happened: %s
            
            What we're doing: %s
            
            What you should do: Change your password, monitor your account
            
            Contact us: security@gym.com
            """,
            breach.getDescription(), breach.getRemedialActions());
    }
}
```

---

## Data Subject Rights

### Right of Access

Users can request download of their data:

```java
@Component
public class DataExportService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkoutRepository workoutRepository;

    /**
     * Export user data in portable format (JSON/CSV)
     * Right of access under GDPR Article 15
     */
    public DataExport exportUserData(Long userId, String format) {
        User user = userRepository.findById(userId)
            .orElseThrow(UserNotFoundException::new);

        DataExport export = new DataExport();
        export.setExportDate(LocalDateTime.now());
        export.setFormat(format);

        // 1. Personal data
        export.setPersonalData(exportPersonalData(user));

        // 2. Workout history
        export.setWorkoutHistory(exportWorkoutHistory(userId));

        // 3. Activity logs
        export.setActivityLogs(exportActivityLogs(userId));

        // 4. Preferences and settings
        export.setPreferences(exportPreferences(userId));

        if ("JSON".equalsIgnoreCase(format)) {
            return generateJsonExport(export);
        } else if ("CSV".equalsIgnoreCase(format)) {
            return generateCsvExport(export);
        }

        return export;
    }

    private Map<String, Object> exportPersonalData(User user) {
        return Map.of(
            "id", user.getId(),
            "username", user.getUsername(),
            "email", user.getEmail(),
            "firstName", user.getFirstName(),
            "lastName", user.getLastName(),
            "createdAt", user.getCreatedAt()
        );
    }

    private List<Map<String, Object>> exportWorkoutHistory(Long userId) {
        return workoutRepository.findByUserId(userId).stream()
            .map(w -> Map.of(
                "id", w.getId(),
                "name", w.getName(),
                "duration", w.getDuration(),
                "startTime", w.getStartTime(),
                "endTime", w.getEndTime()
            ))
            .collect(Collectors.toList());
    }

    private List<Map<String, Object>> exportActivityLogs(Long userId) {
        // Export user activity logs
        return List.of();
    }

    private Map<String, Object> exportPreferences(Long userId) {
        // Export user preferences and settings
        return Map.of();
    }

    private DataExport generateJsonExport(DataExport export) {
        // Convert to JSON format
        return export;
    }

    private DataExport generateCsvExport(DataExport export) {
        // Convert to CSV format
        return export;
    }
}
```

### Right to Rectification

Users can correct inaccurate data:

```java
@Component
public class DataRectificationService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuditLogger auditLogger;

    public void rectifyUserData(Long userId, RectificationRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(UserNotFoundException::new);

        // Track changes for audit trail
        String oldEmail = user.getEmail();
        String oldName = user.getFirstName();

        if (request.getEmail() != null && !request.getEmail().equals(oldEmail)) {
            user.setEmail(request.getEmail());
            auditLogger.logDataModification(userId, "email", oldEmail, request.getEmail());
        }

        if (request.getFirstName() != null && !request.getFirstName().equals(oldName)) {
            user.setFirstName(request.getFirstName());
            auditLogger.logDataModification(userId, "firstName", oldName,
                request.getFirstName());
        }

        userRepository.save(user);
    }
}
```

### Right to Erasure (Right to be Forgotten)

See Secure Data Deletion above.

### Right to Restrict Processing

Users can restrict how their data is processed:

```java
@Entity
@Table(name = "data_restrictions")
public class DataRestriction {

    @Id
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private RestrictionType type;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime removedAt;

    // Getters and setters
}

public enum RestrictionType {
    MARKETING_EMAILS("Restrict marketing emails"),
    ANALYTICS("Restrict analytics tracking"),
    PROFILING("Restrict automated profiling"),
    DATA_SHARING("Restrict data sharing with partners");

    private final String description;

    RestrictionType(String description) {
        this.description = description;
    }
}

@Component
public class DataRestrictionService {

    @Autowired
    private DataRestrictionRepository restrictionRepository;

    public void addRestriction(Long userId, RestrictionType type) {
        DataRestriction restriction = new DataRestriction();
        restriction.setUserId(userId);
        restriction.setType(type);
        restriction.setCreatedAt(LocalDateTime.now());

        restrictionRepository.save(restriction);
    }

    public boolean isRestricted(Long userId, RestrictionType type) {
        return restrictionRepository.existsByUserIdAndTypeAndRemovedAtIsNull(
            userId, type);
    }
}
```

---

## Compliance Checklist

### Development Phase

- [ ] Conduct Privacy Impact Assessment
- [ ] Define legal basis for data processing
- [ ] Identify all data categories collected
- [ ] Design data minimization (collect only necessary data)
- [ ] Implement purpose limitation checks
- [ ] Add audit logging for all data access
- [ ] Implement encryption for sensitive data
- [ ] Design secure data deletion procedure
- [ ] Create privacy notice/policy
- [ ] Implement consent management
- [ ] Add data retention policies

### Pre-Deployment

- [ ] Review compliance requirements
- [ ] Verify all audit logging is working
- [ ] Test data deletion procedures
- [ ] Verify encryption is enabled
- [ ] Review error messages (don't leak data)
- [ ] Check HTTPS/TLS enforcement
- [ ] Verify rate limiting is configured
- [ ] Test access controls

### Production

- [ ] Monitor data access patterns
- [ ] Review audit logs regularly
- [ ] Verify data retention policies are enforced
- [ ] Test breach notification procedures
- [ ] Keep DPA updated
- [ ] Document data flows
- [ ] Conduct regular compliance audits
- [ ] Train staff on data protection

---

## Best Practices

### 1. Privacy by Design

Build privacy into systems from the start, not as an afterthought.

### 2. Data Minimization

Collect only data necessary for the service purpose.

### 3. Encryption Everywhere

Encrypt data at rest, in transit, and during processing.

### 4. Audit Everything

Log all data access and modifications for compliance review.

### 5. User Control

Give users control over their data: access, modify, delete, export.

### 6. Regular Reviews

Regularly review compliance and update policies.

### 7. Staff Training

Train all staff on data protection responsibilities.

### 8. Incident Readiness

Have procedures ready for responding to breaches.

---

## Related Documentation

- [Security Overview](01-security-overview.md) - Security architecture
- [Authentication](02-authentication.md) - User authentication
- [Data Security](04-data-security.md) - Encryption and data protection
- [Incident Response](07-incident-response.md) - Security incident procedures
- [Audit Logging](../operations/03-logging.md) - Logging and monitoring

## References

- [GDPR Regulation (EU) 2016/679](https://gdpr-info.eu/)
- [CCPA - California Consumer Privacy Act](https://oag.ca.gov/privacy/ccpa)
- [ICO GDPR Guidance](https://ico.org.uk/for-organisations/gdpr/)
- [EDPB Guidelines](https://edpb.ec.europa.eu/)
- [OWASP Privacy Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Privacy_Cheat_Sheet.html)

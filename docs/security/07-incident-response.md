# Security Incident Response

## Overview

The Gym Platform's security incident response procedure provides a structured approach to detecting, investigating, containing, and remediating security incidents. This guide covers incident classification, severity levels, escalation procedures, communication templates, forensics procedures, post-incident reviews, and recovery steps for the Gym Platform's microservices architecture.

Incident response is critical to minimizing damage, meeting compliance obligations (GDPR 72-hour breach notification), and maintaining user trust. The Gym Platform maintains 24/7 incident response capabilities with defined roles, responsibilities, and communication protocols.

## Table of Contents

- [Incident Classification](#incident-classification)
- [Severity Levels](#severity-levels)
- [Incident Response Team](#incident-response-team)
- [Detection and Reporting](#detection-and-reporting)
- [Incident Containment](#incident-containment)
- [Investigation Procedures](#investigation-procedures)
- [Evidence Collection](#evidence-collection)
- [Communication and Escalation](#communication-and-escalation)
- [Recovery Steps](#recovery-steps)
- [Post-Incident Review](#post-incident-review)
- [Incident Response Plan](#incident-response-plan)

---

## Incident Classification

### Incident Types

The Gym Platform classifies incidents into categories:

#### 1. Data Breach

Unauthorized access to or disclosure of user data.

**Examples:**
- Unauthorized user data access
- Database exposure
- API credential compromise
- Customer data exfiltration

**Response Time Target:** < 30 minutes to containment

```java
public enum IncidentType {
    DATA_BREACH("Unauthorized access to user data"),
    AUTHENTICATION_BYPASS("Authentication or authorization bypass"),
    MALWARE("Malware detection"),
    DDoS_ATTACK("Distributed denial of service attack"),
    INSIDER_THREAT("Suspicious internal activity"),
    INFRASTRUCTURE_COMPROMISE("Infrastructure or system compromise"),
    APPLICATION_EXPLOIT("Application vulnerability exploitation"),
    MISCONFIGURATION("Security misconfiguration"),
    THIRD_PARTY_COMPROMISE("Third-party compromise"),
    CRYPTOGRAPHIC_FAILURE("Cryptographic system failure");

    private final String description;

    IncidentType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
```

#### 2. Authentication/Authorization Bypass

Circumventing authentication or gaining unauthorized privileges.

**Examples:**
- Privilege escalation
- JWT token forgery
- Role-based access control (RBAC) bypass
- Session hijacking

**Response Time Target:** < 15 minutes to containment

#### 3. Infrastructure Compromise

Unauthorized access to servers, databases, or cloud infrastructure.

**Examples:**
- SSH key compromise
- Database credentials leaked
- Kubernetes cluster access
- Cloud API key exposure

**Response Time Target:** < 1 hour to containment

#### 4. Denial of Service (DoS/DDoS)

Service unavailability due to malicious traffic or resource exhaustion.

**Examples:**
- Layer 7 (application) DDoS
- Layer 3/4 (network) attacks
- Resource exhaustion attacks
- API rate limit abuse

**Response Time Target:** < 5 minutes to mitigation

#### 5. Misconfiguration

Security issues caused by incorrect configuration.

**Examples:**
- Database publicly accessible
- Cloud storage bucket open to public
- Debug mode enabled in production
- Secrets exposed in logs

**Response Time Target:** < 1 hour to remediation

---

## Severity Levels

### Severity Classification Matrix

Incidents are classified by severity based on impact and scope:

| Severity | Impact | Scope | Response Time | Escalation |
|----------|--------|-------|----------------|------------|
| **CRITICAL** | Service down, major data breach, compliance violation | All users or large segment | < 5 minutes | Immediate to executives |
| **HIGH** | Significant data exposure, authentication bypass, large service degradation | > 10% of users or sensitive data | < 15 minutes | Within 30 minutes |
| **MEDIUM** | Partial service impact, minor data exposure, moderate privilege elevation | 1-10% of users or non-critical data | < 1 hour | Within 2 hours |
| **LOW** | Minor service impact, no data exposure, configuration issue, low-impact vulnerability | < 1% of users | < 4 hours | Within 1 business day |

### Severity Scoring

```java
@Component
public class IncidentSeverityCalculator {

    public IncidentSeverity calculateSeverity(IncidentDetails details) {
        int score = 0;

        // Impact factors
        score += calculateImpactScore(details);
        score += calculateDataExposureScore(details);
        score += calculateScopeScore(details);
        score += calculateComplianceRiskScore(details);

        if (score >= 90) return IncidentSeverity.CRITICAL;
        if (score >= 70) return IncidentSeverity.HIGH;
        if (score >= 40) return IncidentSeverity.MEDIUM;
        return IncidentSeverity.LOW;
    }

    private int calculateImpactScore(IncidentDetails details) {
        if (details.isServiceDown()) return 40;
        if (details.hasAuthenticationBypass()) return 35;
        if (details.hasHighDataExposure()) return 30;
        return 10;
    }

    private int calculateDataExposureScore(IncidentDetails details) {
        if (details.exposedUserCount() > 100000) return 40;
        if (details.exposedUserCount() > 10000) return 30;
        if (details.exposedUserCount() > 100) return 20;
        if (details.exposesSensitiveData()) return 25;
        return 0;
    }

    private int calculateScopeScore(IncidentDetails details) {
        double percentageAffected = details.getAffectedUserPercentage();
        if (percentageAffected > 50) return 20;
        if (percentageAffected > 25) return 15;
        if (percentageAffected > 10) return 10;
        if (percentageAffected > 1) return 5;
        return 0;
    }

    private int calculateComplianceRiskScore(IncidentDetails details) {
        if (details.violatesGDPR()) return 30;
        if (details.affectsPaymentData()) return 25;
        if (details.requiresNotification()) return 20;
        return 0;
    }
}
```

---

## Incident Response Team

### Roles and Responsibilities

#### Incident Commander (IC)

- **Responsibility:** Overall incident coordination and decision-making
- **On-Call:** Always available during incident
- **Decision Authority:** Approve containment, remediation, and communication decisions
- **Actions:**
  - Declare incident severity level
  - Activate incident response team
  - Make containment decisions
  - Authorize communications
  - Authorize recovery steps

#### Security Lead

- **Responsibility:** Investigate security incident, determine root cause
- **Expertise:** Security vulnerabilities, attack vectors, forensics
- **Actions:**
  - Analyze logs and evidence
  - Determine attack method and scope
  - Identify affected systems/data
  - Recommend containment steps

#### Infrastructure Lead

- **Responsibility:** System and infrastructure incident response
- **Expertise:** Cloud infrastructure, Kubernetes, databases, networking
- **Actions:**
  - Isolate affected systems
  - Kill suspicious processes
  - Reset compromised credentials
  - Perform system hardening

#### Communications Lead

- **Responsibility:** Internal and external communications
- **Expertise:** Incident communication, stakeholder management
- **Actions:**
  - Notify executives and legal
  - Prepare customer communications
  - Interface with regulators (if required)
  - Update stakeholders periodically

#### Data Protection Officer (DPO)

- **Responsibility:** GDPR compliance, breach notification
- **Expertise:** Data protection regulations, breach thresholds
- **Actions:**
  - Assess GDPR notification requirements
  - Advise on breach notifications
  - Coordinate with data protection authorities
  - Document compliance actions

#### Forensics/Investigation Lead

- **Responsibility:** Detailed incident investigation and evidence preservation
- **Expertise:** Digital forensics, evidence handling, chain of custody
- **Actions:**
  - Preserve evidence
  - Conduct detailed investigation
  - Document findings
  - Prepare incident report

### On-Call Schedule

```yaml
incident-response-on-call:
  primary-incident-commander:
    rotation: weekly
    backup: secondary-ic
    escalation-path: [cto, ceo]

  security-lead:
    rotation: weekly
    backup: senior-security-engineer
    expertise: penetration-testing, vulnerability-analysis

  infrastructure-lead:
    rotation: 24/7
    backup: infrastructure-team
    expertise: kubernetes, databases, cloud-infrastructure

  communications-lead:
    rotation: 24/7
    backup: communications-team
    availability: always-available

  dpo:
    rotation: on-call
    backup: legal-team
    expertise: gdpr, data-protection-regulations

# Contact information
incident-channels:
  slack:
    channel: '#incident-response'
    webhook: ${INCIDENT_WEBHOOK_URL}
  
  pagerduty:
    api-key: ${PAGERDUTY_API_KEY}
    service-id: ${INCIDENT_RESPONSE_SERVICE_ID}
  
  oncall-phone:
    primary: +1-555-0100
    escalation: +1-555-0101
```

---

## Detection and Reporting

### Incident Detection

The Gym Platform uses multiple detection methods:

#### Automated Alerts

```java
@Component
public class SecurityAlertMonitor {

    @Autowired
    private AlertService alertService;

    @Autowired
    private IncidentRepository incidentRepository;

    // Alert on multiple failed authentication attempts
    @Scheduled(fixedRate = 60000)
    public void monitorFailedAuthentication() {
        List<FailedLoginAggregation> suspiciousActivity =
            auditEventRepository.findFailedLoginsSince(LocalDateTime.now().minusMinutes(5));

        for (FailedLoginAggregation activity : suspiciousActivity) {
            if (activity.getAttemptCount() > 10) {
                createBruteForceIncident(activity);
            }
        }
    }

    // Alert on unauthorized data access
    @Scheduled(fixedRate = 60000)
    public void monitorUnauthorizedDataAccess() {
        List<AccessDenialAggregation> denials =
            auditEventRepository.findAccessDenialsSince(LocalDateTime.now().minusMinutes(5));

        for (AccessDenialAggregation denial : denials) {
            if (denial.getDenialCount() > 50) {
                createUnauthorizedAccessIncident(denial);
            }
        }
    }

    // Alert on unusual API activity
    @Scheduled(fixedRate = 60000)
    public void monitorAnomalousApiActivity() {
        List<ApiAnomalyDetection> anomalies =
            apiActivityRepository.detectAnomalies(LocalDateTime.now().minusMinutes(5));

        for (ApiAnomalyDetection anomaly : anomalies) {
            if (anomaly.getSuspicionScore() > 80) {
                createApiAnomalyIncident(anomaly);
            }
        }
    }

    // Alert on database access patterns
    @Scheduled(fixedRate = 60000)
    public void monitorDatabaseAccess() {
        List<DatabaseAccessAnomalyDetection> anomalies =
            databaseActivityRepository.detectAnomalies(LocalDateTime.now().minusMinutes(5));

        for (DatabaseAccessAnomalyDetection anomaly : anomalies) {
            if (anomaly.isUnusualAccessPattern()) {
                createDatabaseAccessIncident(anomaly);
            }
        }
    }

    private void createBruteForceIncident(FailedLoginAggregation activity) {
        Incident incident = new Incident();
        incident.setType(IncidentType.AUTHENTICATION_BYPASS);
        incident.setTitle("Possible Brute Force Attack");
        incident.setDescription(String.format(
            "Detected %d failed login attempts from IP %s",
            activity.getAttemptCount(), activity.getSourceIp()));
        incident.setSourceIp(activity.getSourceIp());
        incident.setStatus(IncidentStatus.DETECTED);
        incident.setCreatedAt(LocalDateTime.now());

        incidentRepository.save(incident);
        alertService.alertIncidentResponse(incident);
    }

    private void createUnauthorizedAccessIncident(AccessDenialAggregation denial) {
        Incident incident = new Incident();
        incident.setType(IncidentType.AUTHENTICATION_BYPASS);
        incident.setTitle("Possible Privilege Escalation Attempt");
        incident.setDescription(String.format(
            "Detected %d unauthorized access attempts from user %s",
            denial.getDenialCount(), denial.getUserId()));
        incident.setUserId(denial.getUserId());
        incident.setStatus(IncidentStatus.DETECTED);
        incident.setCreatedAt(LocalDateTime.now());

        incidentRepository.save(incident);
        alertService.alertIncidentResponse(incident);
    }

    private void createApiAnomalyIncident(ApiAnomalyDetection anomaly) {
        Incident incident = new Incident();
        incident.setType(IncidentType.APPLICATION_EXPLOIT);
        incident.setTitle("Anomalous API Activity Detected");
        incident.setDescription(String.format(
            "Detected anomalous API access pattern from IP %s",
            anomaly.getSourceIp()));
        incident.setSourceIp(anomaly.getSourceIp());
        incident.setStatus(IncidentStatus.DETECTED);
        incident.setCreatedAt(LocalDateTime.now());

        incidentRepository.save(incident);
        alertService.alertIncidentResponse(incident);
    }

    private void createDatabaseAccessIncident(DatabaseAccessAnomalyDetection anomaly) {
        Incident incident = new Incident();
        incident.setType(IncidentType.DATA_BREACH);
        incident.setTitle("Unusual Database Access Pattern");
        incident.setDescription(String.format(
            "Detected unusual database access pattern: %s",
            anomaly.getDescription()));
        incident.setStatus(IncidentStatus.DETECTED);
        incident.setCreatedAt(LocalDateTime.now());

        incidentRepository.save(incident);
        alertService.alertIncidentResponse(incident);
    }
}
```

#### Manual Reporting

Employees can report suspected incidents:

```java
@RestController
@RequestMapping("/api/v1/security/incident-report")
public class IncidentReportingController {

    @Autowired
    private IncidentService incidentService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SECURITY')")
    public ResponseEntity<IncidentDto> reportIncident(
            @RequestBody ReportIncidentRequest request) {
        
        Incident incident = incidentService.createIncident(request);
        alertService.alertIncidentResponse(incident);
        
        return ResponseEntity.ok(IncidentMapper.toDto(incident));
    }
}

public class ReportIncidentRequest {
    private String title;
    private String description;
    private IncidentType type;
    private String sourceIp;
    private String affectedSystems;
    private LocalDateTime discoveryTime;
    private List<String> attachments;

    // Getters and setters
}
```

---

## Incident Containment

### Immediate Actions (First 5 Minutes)

#### Step 1: Declare Incident and Activate Team

```java
@Component
public class IncidentDeclareService {

    @Autowired
    private IncidentRepository incidentRepository;

    @Autowired
    private PagerDutyService pagerDutyService;

    public Incident declareIncident(IncidentRequest request) {
        // 1. Create incident record
        Incident incident = new Incident();
        incident.setType(request.getType());
        incident.setTitle(request.getTitle());
        incident.setDescription(request.getDescription());
        incident.setDiscoveryTime(LocalDateTime.now());
        incident.setStatus(IncidentStatus.ACTIVE);
        incident.setSeverity(calculateSeverity(request));

        Incident saved = incidentRepository.save(incident);

        // 2. Activate incident response team
        activateIncidentResponseTeam(saved);

        // 3. Create incident war room
        createIncidentWarRoom(saved);

        // 4. Start timeline logging
        logIncidentTimeline(saved, "Incident declared", LocalDateTime.now());

        return saved;
    }

    private void activateIncidentResponseTeam(Incident incident) {
        // Trigger PagerDuty alerts
        pagerDutyService.triggerAlert(
            String.format("CRITICAL: %s", incident.getTitle()),
            PagerDutyUrgency.HIGH,
            incident.getId().toString());

        // Send Slack notifications
        slackService.notifyChannel(
            "#incident-response",
            String.format("🚨 INCIDENT DECLARED: %s\nSeverity: %s\nID: %s",
                incident.getTitle(), incident.getSeverity(), incident.getId()));
    }

    private void createIncidentWarRoom(Incident incident) {
        // Create dedicated Slack channel for incident
        slackService.createChannel(
            String.format("incident-war-room-%d", incident.getId()));
    }

    private void logIncidentTimeline(Incident incident, String action, LocalDateTime time) {
        IncidentTimeline timeline = new IncidentTimeline();
        timeline.setIncidentId(incident.getId());
        timeline.setAction(action);
        timeline.setTimestamp(time);
        timelineRepository.save(timeline);
    }
}
```

#### Step 2: Assess Scope and Impact

```java
@Component
public class IncidentScopeAssessmentService {

    public ScopeAssessment assessScope(Incident incident) {
        ScopeAssessment assessment = new ScopeAssessment();

        // 1. Determine affected systems
        Set<String> affectedSystems = identifyAffectedSystems(incident);
        assessment.setAffectedSystems(affectedSystems);

        // 2. Estimate affected users
        long affectedUsers = estimateAffectedUsers(affectedSystems);
        assessment.setEstimatedAffectedUsers(affectedUsers);

        // 3. Check for data exposure
        boolean hasDataExposure = checkDataExposure(incident, affectedSystems);
        assessment.setHasDataExposure(hasDataExposure);

        // 4. Determine compliance impact
        ComplianceImpact complianceImpact = assessComplianceImpact(
            affectedUsers, hasDataExposure);
        assessment.setComplianceImpact(complianceImpact);

        return assessment;
    }

    private Set<String> identifyAffectedSystems(Incident incident) {
        Set<String> systems = new HashSet<>();
        
        if (incident.getSourceIp() != null) {
            // Determine which services are accessed from this IP
            systems.addAll(identifyServicesBySourceIp(incident.getSourceIp()));
        }

        if (incident.getType() == IncidentType.DATA_BREACH) {
            // All data-handling services may be affected
            systems.addAll(List.of("auth-service", "training-service",
                "tracking-service", "database"));
        }

        return systems;
    }

    private long estimateAffectedUsers(Set<String> affectedSystems) {
        // Query to estimate number of users whose data may be affected
        return 0;
    }

    private boolean checkDataExposure(Incident incident, Set<String> affectedSystems) {
        return incident.getType() == IncidentType.DATA_BREACH ||
               affectedSystems.contains("database");
    }

    private ComplianceImpact assessComplianceImpact(long affectedUsers, 
                                                     boolean hasDataExposure) {
        if (hasDataExposure && affectedUsers >= 250) {
            return ComplianceImpact.GDPR_NOTIFICATION_REQUIRED;
        }
        return ComplianceImpact.NO_NOTIFICATION;
    }
}
```

#### Step 3: Initial Containment

```java
@Component
public class IncidentContainmentService {

    @Autowired
    private KubernetesService kubernetesService;

    @Autowired
    private DatabaseService databaseService;

    public void initiateContainment(Incident incident) {
        // 1. Kill suspicious processes
        terminateSuspiciousProcesses(incident);

        // 2. Revoke compromised credentials
        revokeCompromisedCredentials(incident);

        // 3. Isolate affected systems
        isolateAffectedSystems(incident);

        // 4. Enable enhanced monitoring
        enableEnhancedMonitoring(incident);
    }

    private void terminateSuspiciousProcesses(Incident incident) {
        log.info("Terminating suspicious processes related to incident {}", 
            incident.getId());
        
        // Kill processes based on incident details
        if (incident.getSourceIp() != null) {
            kubernetesService.killConnectionsFromIp(incident.getSourceIp());
        }
    }

    private void revokeCompromisedCredentials(Incident incident) {
        log.info("Revoking compromised credentials for incident {}", 
            incident.getId());
        
        // 1. Revoke API keys
        apiKeyService.revokeAllKeys();

        // 2. Revoke JWT tokens (issue new secret)
        tokenService.rotateSigningKeys();

        // 3. Reset database passwords
        databaseService.rotateCredentials();

        // 4. Rotate cloud credentials
        cloudCredentialService.rotateAllCredentials();
    }

    private void isolateAffectedSystems(Incident incident) {
        log.info("Isolating affected systems for incident {}", 
            incident.getId());
        
        // 1. Cut network connections from compromised system
        if (incident.getSourceIp() != null) {
            networkService.blockIp(incident.getSourceIp());
        }

        // 2. Remove compromised nodes from cluster
        // kubernetesService.quarantineNode(nodeName);
    }

    private void enableEnhancedMonitoring(Incident incident) {
        log.info("Enabling enhanced monitoring for incident {}", 
            incident.getId());
        
        // 1. Increase log verbosity
        logConfigService.setVerbosityLevel(LogLevel.DEBUG);

        // 2. Enable real-time alerting
        monitoringService.enableRealTimeAlerts();

        // 3. Start packet capture if applicable
        // packetCaptureService.startCapture();
    }
}
```

---

## Investigation Procedures

### Log and Evidence Collection

```java
@Component
public class IncidentInvestigationService {

    @Autowired
    private LogAggregationService logAggregationService;

    @Autowired
    private DatabaseService databaseService;

    public IncidentInvestigationReport investigate(Incident incident) {
        IncidentInvestigationReport report = new IncidentInvestigationReport();
        report.setIncidentId(incident.getId());
        report.setStartTime(LocalDateTime.now());

        // 1. Collect logs around incident time
        collectLogs(incident, report);

        // 2. Analyze access patterns
        analyzeAccessPatterns(incident, report);

        // 3. Examine audit trail
        examineAuditTrail(incident, report);

        // 4. Check for persistence mechanisms
        checkForPersistenceMechanisms(incident, report);

        // 5. Determine scope of compromise
        determineCompromiseScope(incident, report);

        report.setEndTime(LocalDateTime.now());
        return report;
    }

    private void collectLogs(Incident incident, IncidentInvestigationReport report) {
        LocalDateTime startTime = incident.getDiscoveryTime().minusHours(1);
        LocalDateTime endTime = incident.getDiscoveryTime().plusHours(6);

        // 1. Collect application logs
        List<LogEntry> appLogs = logAggregationService.getLogs(
            "app", startTime, endTime);
        report.setApplicationLogs(appLogs);

        // 2. Collect database logs
        List<LogEntry> dbLogs = logAggregationService.getLogs(
            "database", startTime, endTime);
        report.setDatabaseLogs(dbLogs);

        // 3. Collect authentication logs
        List<LogEntry> authLogs = logAggregationService.getLogs(
            "authentication", startTime, endTime);
        report.setAuthenticationLogs(authLogs);

        // 4. Collect infrastructure logs
        List<LogEntry> infraLogs = logAggregationService.getLogs(
            "infrastructure", startTime, endTime);
        report.setInfrastructureLogs(infraLogs);
    }

    private void analyzeAccessPatterns(Incident incident, 
                                        IncidentInvestigationReport report) {
        // Analyze API access patterns before/during/after incident
        LocalDateTime beforeIncident = incident.getDiscoveryTime().minusHours(24);
        LocalDateTime afterIncident = incident.getDiscoveryTime().plusHours(1);

        List<ApiAccess> accessPatterns = apiAuditRepository.getAccessesBetween(
            beforeIncident, afterIncident);

        AccessPatternAnalysis analysis = new AccessPatternAnalysis();
        analysis.setAnomalousAccess(identifyAnomalousAccess(accessPatterns));
        analysis.setCompromisedAccounts(identifyCompromisedAccounts(accessPatterns));
        analysis.setUnusualTimings(identifyUnusualTimings(accessPatterns));

        report.setAccessAnalysis(analysis);
    }

    private void examineAuditTrail(Incident incident, 
                                    IncidentInvestigationReport report) {
        // Get complete audit trail for affected systems/users
        List<AuditEvent> auditTrail = auditEventRepository.findByIncident(
            incident.getId());

        report.setAuditTrail(auditTrail);

        // Identify suspicious actions
        List<AuditEvent> suspiciousActions = auditTrail.stream()
            .filter(e -> isSuspiciousAction(e))
            .collect(Collectors.toList());

        report.setSuspiciousActions(suspiciousActions);
    }

    private void checkForPersistenceMechanisms(Incident incident, 
                                                IncidentInvestigationReport report) {
        // Check for backdoors, scheduled tasks, etc.
        List<PersistenceMechanism> mechanisms = new ArrayList<>();

        // 1. Check for unusual cron jobs
        List<CronJob> cronJobs = systemService.getCronJobs();
        mechanisms.addAll(identifyUnusualCronJobs(cronJobs));

        // 2. Check for unauthorized users
        List<UserAccount> users = systemService.getUserAccounts();
        mechanisms.addAll(identifyUnauthorizedUsers(users));

        // 3. Check for unauthorized SSH keys
        List<SshKey> sshKeys = systemService.getSshKeys();
        mechanisms.addAll(identifyUnauthorizedSshKeys(sshKeys));

        report.setPersistenceMechanisms(mechanisms);
    }

    private void determineCompromiseScope(Incident incident, 
                                          IncidentInvestigationReport report) {
        // Determine full extent of compromise
        CompromiseScope scope = new CompromiseScope();

        scope.setCompromisedSystems(identifyCompromisedSystems(incident));
        scope.setCompromisedAccounts(identifyCompromisedAccounts(incident));
        scope.setExposedData(identifyExposedData(incident));
        scope.setLikelihood(assessCompromiseLikelihood(incident));

        report.setCompromiseScope(scope);
    }

    private boolean isSuspiciousAction(AuditEvent event) {
        // Determine if action is suspicious
        return event.getAction().equals("PRIVILEGE_ESCALATION") ||
               event.getAction().equals("DATA_EXPORT") ||
               event.getAction().equals("UNAUTHORIZED_ACCESS");
    }

    private List<PersistenceMechanism> identifyUnusualCronJobs(List<CronJob> cronJobs) {
        // Identify suspicious cron jobs
        return List.of();
    }

    private List<PersistenceMechanism> identifyUnauthorizedUsers(List<UserAccount> users) {
        // Identify unauthorized user accounts
        return List.of();
    }

    private List<PersistenceMechanism> identifyUnauthorizedSshKeys(List<SshKey> sshKeys) {
        // Identify unauthorized SSH keys
        return List.of();
    }
}
```

---

## Evidence Collection

### Chain of Custody

Maintain proper chain of custody for evidence:

```java
@Entity
@Table(name = "evidence_chain_of_custody")
public class EvidenceChainOfCustody {

    @Id
    private Long id;

    @Column(nullable = false)
    private Long incidentId;

    @Column(nullable = false)
    private String evidenceDescription;

    @Column(nullable = false)
    private String evidenceHash;  // SHA-256 hash for integrity

    @Column(nullable = false)
    private String collectedBy;

    @Column(nullable = false)
    private LocalDateTime collectionTime;

    @Column(nullable = false)
    private String location;  // Where evidence is stored

    @OneToMany(cascade = CascadeType.ALL)
    private List<CustodyTransfer> transfers;

    // Getters and setters
}

@Entity
@Table(name = "custody_transfers")
public class CustodyTransfer {

    @Id
    private Long id;

    @Column(nullable = false)
    private Long custodyRecordId;

    @Column(nullable = false)
    private String transferredFrom;

    @Column(nullable = false)
    private String transferredTo;

    @Column(nullable = false)
    private LocalDateTime transferTime;

    @Column(length = 500)
    private String reason;

    @Column(nullable = false)
    private String verificationHash;  // Hash verification upon transfer

    // Getters and setters
}

@Component
public class EvidenceCollectionService {

    @Autowired
    private EvidenceRepository evidenceRepository;

    public void collectEvidence(Long incidentId, String evidenceType, 
                                String evidenceData) {
        // 1. Compute hash for integrity verification
        String hash = computeSHA256(evidenceData);

        // 2. Create chain of custody record
        EvidenceChainOfCustody custody = new EvidenceChainOfCustody();
        custody.setIncidentId(incidentId);
        custody.setEvidenceDescription(evidenceType);
        custody.setEvidenceHash(hash);
        custody.setCollectedBy(getCurrentUser());
        custody.setCollectionTime(LocalDateTime.now());
        custody.setLocation(getEvidenceStorageLocation());

        evidenceRepository.save(custody);

        // 3. Store evidence securely
        storeEvidenceSecurely(incidentId, evidenceType, evidenceData, hash);
    }

    private String computeSHA256(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    private void storeEvidenceSecurely(Long incidentId, String evidenceType, 
                                       String evidenceData, String hash) {
        // Store in encrypted storage with access controls
        // Example: AWS S3 with encryption and versioning
    }

    private String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
```

---

## Communication and Escalation

### Escalation Procedure

```java
@Component
public class IncidentEscalationService {

    @Autowired
    private IncidentRepository incidentRepository;

    @Autowired
    private NotificationService notificationService;

    public void escalateIncident(Long incidentId, IncidentSeverity newSeverity) {
        Incident incident = incidentRepository.findById(incidentId)
            .orElseThrow(IncidentNotFoundException::new);

        IncidentSeverity oldSeverity = incident.getSeverity();
        incident.setSeverity(newSeverity);
        incident.setLastUpdated(LocalDateTime.now());

        incidentRepository.save(incident);

        // Notify based on severity
        switch (newSeverity) {
            case CRITICAL:
                notifyCriticalEscalation(incident, oldSeverity);
                break;
            case HIGH:
                notifyHighEscalation(incident, oldSeverity);
                break;
            case MEDIUM:
                notifyMediumEscalation(incident, oldSeverity);
                break;
            default:
                break;
        }
    }

    private void notifyCriticalEscalation(Incident incident, IncidentSeverity oldSeverity) {
        // Immediate notification to executive team
        notificationService.sendCriticalAlert(
            "🚨 CRITICAL INCIDENT ESCALATION\n" +
            "ID: " + incident.getId() + "\n" +
            "Previous: " + oldSeverity + "\n" +
            "Current: CRITICAL\n" +
            "Title: " + incident.getTitle() + "\n" +
            "Description: " + incident.getDescription(),
            List.of(
                "cto@gym.com",
                "cso@gym.com",
                "ceo@gym.com"));

        // Page on-call executive
        pagerDutyService.triggerCriticalPage();
    }

    private void notifyHighEscalation(Incident incident, IncidentSeverity oldSeverity) {
        // Notify security and operations teams
        notificationService.sendAlert(
            "⚠️ HIGH SEVERITY INCIDENT ESCALATION\n" +
            "ID: " + incident.getId() + "\n" +
            "Title: " + incident.getTitle(),
            List.of("security-team@gym.com", "ops-team@gym.com"));
    }

    private void notifyMediumEscalation(Incident incident, IncidentSeverity oldSeverity) {
        // Notify relevant teams
        notificationService.sendAlert(
            "⚠️ MEDIUM SEVERITY INCIDENT\n" +
            "ID: " + incident.getId() + "\n" +
            "Title: " + incident.getTitle(),
            List.of("security-team@gym.com"));
    }
}
```

### Communication Templates

#### Internal Notification Template

```java
public class InternalNotificationTemplate {

    public static String generateIncidentUpdate(Incident incident, 
                                                 LocalDateTime lastUpdate) {
        return String.format("""
            INCIDENT UPDATE
            
            ID: %s
            Severity: %s
            Status: %s
            
            Title: %s
            
            Description:
            %s
            
            Timeline:
            %s
            
            Current Actions:
            %s
            
            Next Update: %s
            
            Incident Commander: %s
            Contact: %s
            """,
            incident.getId(),
            incident.getSeverity(),
            incident.getStatus(),
            incident.getTitle(),
            incident.getDescription(),
            formatTimeline(incident),
            formatCurrentActions(incident),
            lastUpdate.plusMinutes(30),
            incident.getIncidentCommanderId(),
            getIncidentCommanderContact(incident.getIncidentCommanderId())
        );
    }

    private static String formatTimeline(Incident incident) {
        // Format incident timeline
        return "";
    }

    private static String formatCurrentActions(Incident incident) {
        // Format current remediation actions
        return "";
    }

    private static String getIncidentCommanderContact(Long commanderId) {
        // Get IC contact information
        return "";
    }
}
```

#### Customer Notification Template

```java
public class CustomerNotificationTemplate {

    public static String generateBreach Notification(Incident incident, 
                                                      List<String> affectedEmails) {
        return String.format("""
            Security Alert - Action Required
            
            We are writing to inform you of a security incident that affected your account.
            
            WHAT HAPPENED
            %s
            
            WHAT DATA WAS AFFECTED
            %s
            
            WHAT WE'RE DOING
            We have immediately:
            - Investigated the incident
            - Contained the threat
            - Implemented additional security measures
            
            WHAT YOU SHOULD DO
            - Change your password immediately
            - Review your account activity
            - Monitor your accounts for suspicious activity
            - Enable two-factor authentication
            
            For more information, please contact: security@gym.com
            
            We sincerely apologize for this incident and any inconvenience caused.
            """,
            incident.getDescription(),
            formatAffectedData(incident)
        );
    }

    private static String formatAffectedData(Incident incident) {
        // List what data was affected
        return "";
    }
}
```

---

## Recovery Steps

### Recovery Procedure

```java
@Component
public class IncidentRecoveryService {

    @Autowired
    private BackupService backupService;

    @Autowired
    private DatabaseService databaseService;

    @Autowired
    private KubernetesService kubernetesService;

    public void initiateRecovery(Incident incident) {
        log.info("Initiating recovery for incident {}", incident.getId());

        // 1. Restore from clean backup
        restoreFromBackup(incident);

        // 2. Verify system integrity
        verifySystemIntegrity(incident);

        // 3. Rebuild compromised systems
        rebuildCompromisedSystems(incident);

        // 4. Restore normal operations
        restoreNormalOperations(incident);

        // 5. Continuous monitoring
        enableContinuousMonitoring(incident);
    }

    private void restoreFromBackup(Incident incident) {
        log.info("Restoring from backup for incident {}", incident.getId());

        // 1. Find appropriate backup point (before compromise)
        Backup backup = backupService.findLatestCleanBackup(
            incident.getDiscoveryTime().minusHours(24));

        // 2. Verify backup integrity
        if (!backupService.verifyIntegrity(backup)) {
            throw new BackupIntegrityException(
                "Backup integrity verification failed");
        }

        // 3. Restore database
        databaseService.restore(backup);

        // 4. Restore application state
        restoreApplicationState(backup);
    }

    private void verifySystemIntegrity(Incident incident) {
        log.info("Verifying system integrity for incident {}", incident.getId());

        // 1. Verify file checksums
        verifyFileIntegrity();

        // 2. Check for unauthorized changes
        checkForUnauthorizedModifications();

        // 3. Verify configuration files
        verifyConfigurationFiles();
    }

    private void rebuildCompromisedSystems(Incident incident) {
        log.info("Rebuilding compromised systems for incident {}", incident.getId());

        // 1. Rebuild Kubernetes nodes
        kubernetesService.rebuildNodes();

        // 2. Deploy fresh containers
        kubernetesService.redeploy();

        // 3. Verify connectivity
        kubernetesService.verifyConnectivity();
    }

    private void restoreNormalOperations(Incident incident) {
        log.info("Restoring normal operations for incident {}", incident.getId());

        // 1. Resume normal traffic routing
        loadBalancerService.resumeNormalRouting();

        // 2. Re-enable user access
        userAccessService.restoreAccess();

        // 3. Verify service health
        healthCheckService.verifyAllServicesHealthy();
    }

    private void enableContinuousMonitoring(Incident incident) {
        log.info("Enabling continuous monitoring for incident {}", incident.getId());

        // Keep enhanced monitoring in place for 24-48 hours
        monitoringService.enableExtendedMonitoring(Duration.ofHours(48));
    }

    private void restoreApplicationState(Backup backup) {
        // Restore application configuration and state
    }

    private void verifyFileIntegrity() {
        // Verify file checksums against known good hashes
    }

    private void checkForUnauthorizedModifications() {
        // Check for unauthorized system modifications
    }

    private void verifyConfigurationFiles() {
        // Verify configuration files are secure
    }
}
```

---

## Post-Incident Review

### Blameless Root Cause Analysis

```java
@Entity
@Table(name = "incident_post_mortems")
public class PostMortemReport {

    @Id
    private Long id;

    @Column(nullable = false)
    private Long incidentId;

    @Column(nullable = false)
    private LocalDate reviewDate;

    @Column(length = 5000)
    private String summary;

    @Column(length = 5000)
    private String timeline;

    @Column(length = 5000)
    private String rootCause;

    @OneToMany(cascade = CascadeType.ALL)
    private List<ContributingFactor> contributingFactors;

    @OneToMany(cascade = CascadeType.ALL)
    private List<Action> actionItems;

    @Column
    private String conductedBy;

    @Column
    private LocalDate targetCompletionDate;

    // Getters and setters
}

@Component
public class PostMortemService {

    @Autowired
    private PostMortemRepository postMortemRepository;

    public PostMortemReport conductPostMortem(Incident incident) {
        PostMortemReport report = new PostMortemReport();
        report.setIncidentId(incident.getId());
        report.setReviewDate(LocalDate.now());

        // 1. Timeline reconstruction
        report.setTimeline(reconstructTimeline(incident));

        // 2. Root cause analysis
        report.setRootCause(analyzeRootCause(incident));

        // 3. Contributing factors
        report.setContributingFactors(identifyContributingFactors(incident));

        // 4. Action items
        report.setActionItems(generateActionItems(incident));

        // 5. Summary
        report.setSummary(generateSummary(report));

        return postMortemRepository.save(report);
    }

    private String reconstructTimeline(Incident incident) {
        // Reconstruct detailed timeline from logs
        return "";
    }

    private String analyzeRootCause(Incident incident) {
        // Determine root cause without blame
        return "";
    }

    private List<ContributingFactor> identifyContributingFactors(Incident incident) {
        // Identify all factors that contributed to incident
        return List.of();
    }

    private List<Action> generateActionItems(Incident incident) {
        // Generate action items to prevent recurrence
        List<Action> actions = new ArrayList<>();

        // Examples:
        // - Implement additional monitoring
        // - Patch vulnerable system
        // - Update security policies
        // - Conduct security training

        return actions;
    }

    private String generateSummary(PostMortemReport report) {
        return String.format("""
            POSTMORTEM SUMMARY
            
            Timeline:
            %s
            
            Root Cause:
            %s
            
            Contributing Factors:
            %s
            
            Action Items:
            %s
            """,
            report.getTimeline(),
            report.getRootCause(),
            formatContributingFactors(report),
            formatActionItems(report)
        );
    }

    private String formatContributingFactors(PostMortemReport report) {
        return report.getContributingFactors().stream()
            .map(f -> "- " + f.getDescription())
            .collect(Collectors.joining("\n"));
    }

    private String formatActionItems(PostMortemReport report) {
        return report.getActionItems().stream()
            .map(a -> String.format("- %s (Owner: %s, Due: %s)",
                a.getDescription(), a.getOwner(), a.getDueDate()))
            .collect(Collectors.joining("\n"));
    }
}
```

---

## Incident Response Plan

### 24/7 Incident Response

The Gym Platform maintains 24/7 incident response capability:

```yaml
incident-response-plan:
  coverage: 24/7
  response-time:
    critical: 5 minutes
    high: 15 minutes
    medium: 1 hour
    low: 4 hours

  escalation-path:
    level-1:
      - primary-incident-commander
      - security-lead
      - infrastructure-lead
    level-2:
      - cto
      - cso
    level-3:
      - ceo
      - legal-team
      - dpo

  communication-channels:
    internal:
      - slack-channel: '#incident-response'
      - pagerduty
      - oncall-phone
    external:
      - customer-support
      - data-protection-authorities
      - law-enforcement

  testing:
    frequency: quarterly
    tabletop-exercises: 2-per-year
    full-drills: 1-per-year

  documentation:
    incident-records: encrypted-storage
    retention: 7-years
    access-controls: admin-only
```

---

## Related Documentation

- [Security Overview](01-security-overview.md) - Security architecture
- [Compliance](06-compliance.md) - GDPR breach notification requirements
- [Vulnerability Management](08-vulnerability-management.md) - Patch management
- [Incident Management](../operations/06-incident-management.md) - General incident procedures

## References

- [NIST Cybersecurity Framework](https://www.nist.gov/cyberframework)
- [NIST Incident Handling Guide](https://nvlpubs.nist.gov/nistpubs/SpecialPublications/NIST.SP.800-61r3.pdf)
- [SANS Incident Response](https://www.sans.org/reading-room/whitepapers/incident/)
- [OWASP Incident Response](https://owasp.org/www-project-incident-response/)

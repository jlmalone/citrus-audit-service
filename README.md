# 🍊 Citrus Audit & Compliance Service

Enterprise-grade audit logging and compliance service for the Citrus Platform. Provides comprehensive event sourcing, user activity tracking, compliance reporting (SOC 2, GDPR), data retention policies, and GDPR erasure capabilities.

## Features

### 🔍 Audit Logging
- **Event Sourcing**: Complete audit trail of all system events
- **User Activity Tracking**: Track all user actions with detailed context
- **Access Logs**: Monitor resource access patterns
- **Receipt Generation**: Cryptographic receipts for audit events

### 📊 Compliance Reporting
- **SOC 2 Reports**: Automated SOC 2 compliance reporting
- **GDPR Reports**: GDPR compliance monitoring and reporting
- **Compliance Statistics**: Real-time compliance metrics and insights
- **Audit Findings**: Automated detection of compliance issues

### 🗄️ Data Management
- **Retention Policies**: Configurable data retention by event type
- **Automated Cleanup**: Automatic deletion of expired audit data
- **GDPR Erasure**: Right to erasure (right to be forgotten) implementation
- **Data Portability**: Export user data for GDPR compliance

## Tech Stack

- **Language**: Kotlin 1.9.21
- **Framework**: Ktor 2.3.7
- **Database**: PostgreSQL 15
- **ORM**: Exposed
- **Build**: Gradle with Kotlin DSL

## Quick Start

### Prerequisites

- Docker & Docker Compose
- JDK 17+ (for local development)
- PostgreSQL 15+ (if running without Docker)

### Running with Docker Compose

```bash
# Start all services
docker-compose up -d

# View logs
docker-compose logs -f audit-service

# Stop services
docker-compose down
```

The service will be available at `http://localhost:8080`

### Running Locally

```bash
# Start PostgreSQL
docker-compose up -d postgres

# Build the project
./gradlew build

# Run the service
./gradlew run
```

## API Documentation

### Audit Events

#### Log an Event
```http
POST /audit/events
Content-Type: application/x-www-form-urlencoded

eventType=RESOURCE_ACCESSED
userId=user123
username=john.doe
action=READ_FILE
description=User accessed sensitive document
resourceType=document
resourceId=doc-456
severity=INFO
success=true
```

#### Query Events
```http
GET /audit/events?userId=user123&limit=50&offset=0
GET /audit/events?eventType=USER_LOGIN&startTime=2025-01-01T00:00:00Z
GET /audit/events?severity=CRITICAL&successOnly=false
```

#### Get Event by ID
```http
GET /audit/events/{eventId}
```

### User Activity

#### Get User Activity Summary
```http
GET /audit/users/{userId}/activity?limit=10
```

Response:
```json
{
  "userId": "user123",
  "username": "john.doe",
  "loginCount": 42,
  "lastLogin": "2025-01-15T10:30:00Z",
  "resourceAccessCount": 156,
  "failedActionCount": 3,
  "mostRecentActions": [...]
}
```

### Access Logs

#### Get Access Logs
```http
GET /audit/access-logs?resourceType=document&limit=100
GET /audit/access-logs?resourceId=doc-456
```

### Receipts

#### Generate Receipt for Event
```http
GET /audit/receipts/{eventId}
```

#### Get User Receipts
```http
GET /audit/users/{userId}/receipts?limit=50
```

### Compliance Reports

#### Generate SOC 2 Report
```http
GET /compliance/reports/soc2?days=30
```

#### Generate GDPR Report
```http
GET /compliance/reports/gdpr?days=90
```

#### Get Compliance Statistics
```http
GET /compliance/stats?days=30
```

### Data Retention

#### Get All Retention Policies
```http
GET /retention/policies
```

#### Create/Update Retention Policy
```http
POST /retention/policies
Content-Type: application/x-www-form-urlencoded

eventType=USER_LOGIN
retentionDays=90
autoDelete=true
description=Login events retained for 90 days
```

#### Apply Retention Policies (Dry Run)
```http
POST /retention/apply?dryRun=true
```

#### Apply Retention Policies (Execute)
```http
POST /retention/apply?dryRun=false
```

#### Initialize Default Policies
```http
POST /retention/initialize
```

### GDPR Operations

#### Request Data Erasure
```http
POST /gdpr/erasure
Content-Type: application/x-www-form-urlencoded

userId=user123
requestedBy=admin@company.com
reason=User requested account deletion
```

#### Execute Erasure
```http
POST /gdpr/erasure/{requestId}/execute
```

#### Get Erasure Status
```http
GET /gdpr/erasure/{requestId}
```

#### Get All Erasure Requests
```http
GET /gdpr/erasure?status=PENDING
```

#### Export User Data
```http
GET /gdpr/users/{userId}/export
```

## Event Types

The service supports the following audit event types:

- `USER_LOGIN` - User login events
- `USER_LOGOUT` - User logout events
- `USER_CREATED` - New user creation
- `USER_UPDATED` - User profile updates
- `USER_DELETED` - User deletion
- `RESOURCE_ACCESSED` - Resource access events
- `RESOURCE_CREATED` - Resource creation
- `RESOURCE_UPDATED` - Resource modifications
- `RESOURCE_DELETED` - Resource deletion
- `PERMISSION_GRANTED` - Permission grants
- `PERMISSION_REVOKED` - Permission revocations
- `PAYMENT_PROCESSED` - Payment transactions
- `PAYMENT_FAILED` - Failed payments
- `DATA_EXPORTED` - Data export operations
- `DATA_IMPORTED` - Data import operations
- `COMPLIANCE_REPORT_GENERATED` - Compliance report generation
- `GDPR_ERASURE_REQUESTED` - GDPR erasure request
- `GDPR_ERASURE_COMPLETED` - GDPR erasure completion
- `RETENTION_POLICY_APPLIED` - Retention policy application

## Event Severity Levels

- `INFO` - Informational events
- `WARNING` - Warning events requiring attention
- `ERROR` - Error events
- `CRITICAL` - Critical security or system events

## Configuration

Environment variables:

```bash
# Server
PORT=8080

# Database
DB_URL=jdbc:postgresql://localhost:5432/citrus_audit
DB_USER=citrus
DB_PASSWORD=citrus123

# Retention (days)
RETENTION_DEFAULT_DAYS=365
RETENTION_GDPR_DAYS=2555  # 7 years
```

## Database Schema

### Tables

1. **audit_events** - Main event sourcing table
   - Stores all audit events with full context
   - Indexed on userId, eventType, timestamp, resourceType/Id

2. **retention_policies** - Data retention policies
   - Configurable retention by event type
   - Auto-delete capability

3. **gdpr_erasure_requests** - GDPR erasure tracking
   - Tracks erasure requests and completions
   - Maintains compliance audit trail

## Security Considerations

- All audit events are immutable (event sourcing pattern)
- GDPR erasure anonymizes data rather than deleting (maintains audit trail)
- Cryptographic checksums for receipt verification
- Configurable data retention to minimize data storage
- Comprehensive access logging

## Development

### Build

```bash
./gradlew build
```

### Run Tests

```bash
./gradlew test
```

### Generate Fat JAR

```bash
./gradlew shadowJar
```

## Production Deployment

1. Configure environment variables
2. Set up PostgreSQL with appropriate security
3. Use Docker Compose or Kubernetes for orchestration
4. Enable SSL/TLS for database connections
5. Set up regular backups of audit data
6. Configure retention policies appropriately
7. Monitor compliance report findings

## Compliance Features

### SOC 2 Controls

- **Access Control**: Tracks all access attempts and failures
- **Change Management**: Logs all resource modifications
- **System Monitoring**: Comprehensive event logging
- **Security Incident Detection**: Identifies suspicious patterns

### GDPR Compliance

- **Right to Access**: User data export functionality
- **Right to Erasure**: Data anonymization on request
- **Data Minimization**: Configurable retention policies
- **Audit Trail**: Complete event history for compliance
- **Breach Detection**: Monitors for potential data breaches

## License

Part of the Citrus Enterprise Platform.

## Support

For issues and questions, please contact the Citrus Platform team.

---

**Built with 🍊 by the Citrus Team**

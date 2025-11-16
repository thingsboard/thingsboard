# ThingsBoard Java Backend - Quick Reference Guide

## Project Statistics

| Metric | Count |
|--------|-------|
| Total Java Files | 1,600+ |
| Application Module Java Files | 905 |
| DAO Module Java Files | 742 |
| REST Controllers | 59 |
| Entity Models | 97 |
| SQL Entity Classes | 66 |
| JPA Repositories | 84 |
| DAO Interfaces | 60+ |
| Service Classes | 55+ |
| Rule Engine Nodes | 91 |
| REST API Endpoints | 200+ |
| Database Tables | 34+ |
| Supported Protocols | 6 |
| Supported Databases | 4+ |
| Total Lines of Code | 200,000+ |

---

## Technology Stack Summary

| Category | Technology | Version |
|----------|-----------|---------|
| **Framework** | Spring Boot | 3.4.10 |
| **Java** | Java | 17+ |
| **Build** | Maven | 3+ |
| **Primary DB** | PostgreSQL | Latest |
| **Secondary DB** | MySQL, H2, Cassandra | - |
| **Message Queue** | Kafka | 3.9.1 |
| **Cache** | Redis | 5.1.5 |
| **WebSocket** | Java WebSocket | 1.5.6 |
| **MQTT** | Paho Client | 1.2.5 |
| **CoAP** | Californium | 3.12.1 |
| **LWM2M** | Leshan | 2.0.0 |
| **gRPC** | gRPC | 1.68.1 |
| **JWT** | JJWT | 0.12.5 |
| **ORM** | Hibernate/JPA | - |
| **Logging** | SLF4J/Logback | - |

---

## Core API Endpoint Categories

### 1. Device Management (10+ endpoints)
- CRUD operations on devices
- Device profile management
- Device credentials/authentication
- Device claiming & provisioning
- Device connectivity tracking

### 2. Asset Management (8+ endpoints)
- CRUD operations on assets
- Asset profile management
- Asset-to-customer assignments
- Asset searching & filtering

### 3. Alarm Management (12+ endpoints)
- Alarm creation & management
- Acknowledge/Clear operations
- Alarm assignment & escalation
- Alarm comments
- Alarm searching & filtering

### 4. Dashboard Management (6+ endpoints)
- Dashboard CRUD operations
- Dashboard sharing with customers
- Home dashboard configuration
- Dashboard widgets management

### 5. User & Access Control (12+ endpoints)
- User account management
- User authentication & authorization
- Customer management
- Tenant management
- Two-factor authentication
- User settings & preferences

### 6. Telemetry & Data (10+ endpoints)
- Attributes (save, get, delete)
- Timeseries data (save, get, delete)
- Telemetry subscriptions
- Data export & import

### 7. Rule Engine (4+ endpoints)
- Rule chain CRUD
- Rule chain metadata
- Rule node definitions
- Component descriptors

### 8. RPC Communication (4+ endpoints)
- RPC v1 (one-way & two-way)
- RPC v2 (request/response)
- Device-to-server communication

### 9. Entity Relations (8+ endpoints)
- Create/delete relations
- Query entity relationships
- Bulk relation operations

### 10. Edge Computing (4+ endpoints)
- Edge device management
- Edge event tracking
- Edge synchronization

### 11. Notifications (8+ endpoints)
- Notification delivery
- Notification rules
- Notification targets
- Notification templates

### 12. System & Configuration (8+ endpoints)
- System settings
- UI settings
- Admin configuration
- System information
- Usage tracking

---

## Database Entity Summary

### Core Entities (Most Important)
1. **Device** - IoT devices, sensors, actuators
2. **DeviceProfile** - Device type templates & configurations
3. **Asset** - Physical/logical assets & infrastructure
4. **AssetProfile** - Asset type templates
5. **Alarm** - Event alerts & notifications
6. **Dashboard** - UI dashboards & visualizations
7. **Tenant** - Multi-tenancy isolation
8. **Customer** - Customer accounts & hierarchies
9. **User** - User accounts & permissions
10. **EntityView** - Filtered entity views

### Supporting Entities
11. **RuleChain** - Visual rule processing chains
12. **RuleNode** - Rule nodes in a chain
13. **Attributes** - Static metadata (key-value pairs)
14. **Timeseries** - Time-stamped telemetry data
15. **Relation** - Entity relationships & hierarchy
16. **Event** - System events & activities
17. **AuditLog** - Activity audit trail
18. **RPC** - Remote procedure call requests/responses
19. **Edge** - Edge computing devices
20. **Notification** - System notifications

---

## Authentication & Security Features

### Supported Authentication Methods
1. **JWT (JSON Web Tokens)**
   - Access tokens
   - Refresh tokens
   - Token-based stateless auth

2. **OAuth2**
   - Multiple OAuth2 providers
   - Custom OAuth2 configuration
   - OAuth2 client management

3. **Device Credentials**
   - Access tokens for IoT devices
   - X.509 certificate authentication
   - Pre-shared keys

4. **Two-Factor Authentication (2FA)**
   - TOTP (Time-based One-Time Password)
   - 2FA provisioning & verification

### Authorization Features
- Role-based access control (RBAC)
- Resource-based permissions
- Operation-level permissions
- Tenant isolation
- Customer hierarchy support
- Domain-based access

---

## Rule Engine Node Categories

### Filter Nodes (11)
- Message type filtering
- Entity type filtering & switching
- JavaScript-based filters
- Relation validation
- Alarm status checking
- Checkpoint creation

### Transform Nodes (7)
- Message payload transformation
- JSONPath extraction
- Array splitting
- Key manipulation (copy, rename, delete)
- Originator changing

### Action Nodes (11)
- Alarm creation & clearing
- Device state tracking
- Entity relation management
- Customer assignment
- Attribute copying
- Custom Cassandra operations

### External Integration Nodes (8)
- REST API calls
- Kafka producer
- RabbitMQ producer
- SMS sending (Twilio, AWS, SMPP)
- Slack notifications
- Custom notifications

### Flow Control Nodes (6)
- Rule chain input/output
- Message acknowledgment
- Checkpoints
- Synchronization barriers

---

## WebSocket Capabilities

### Real-Time Features
- Live telemetry streaming
- Attribute updates
- Entity state changes
- Event notifications
- Alarm updates
- Dashboard data push

### Session Management
- Session creation/termination
- Session lifecycle events
- Stale session cleanup
- Multi-session support
- Tenant-level isolation

### Message Types
- Text messages
- Ping/Pong (keep-alive)
- Commands
- Subscriptions
- Updates
- Error messages

---

## Transport Protocol Support

### HTTP/HTTPS
- Standard REST API
- Device data submission
- File uploads
- Request/response synchronous

### MQTT (v3.1.1, v5.0)
- Device telemetry publishing
- Attribute subscriptions
- RPC handling
- Multiple QoS levels
- Will messages

### CoAP (Constrained Application Protocol)
- Lightweight protocol for IoT
- Resource discovery
- Observe (subscription) support
- DTLS security

### LWM2M (Lightweight M2M)
- OMA standard support
- Bootstrap server
- Firmware updates
- Security modes (NoSec, PSK, RPK, X.509)
- Multiple transport (UDP, DTLS, SMS)

### SNMP (Simple Network Management Protocol)
- Network device integration
- Trap handling
- SNMP v1, v2c, v3 support

### gRPC
- Service-to-service communication
- Protocol Buffers serialization
- Bi-directional streaming

---

## Multi-Tenancy Architecture

### Tenant Isolation
- Complete data isolation
- Separate billing & usage tracking
- Independent configurations
- Resource quotas per tenant

### Hierarchy Levels
1. **Tenant** - Top-level account
2. **Customer** - Customer within tenant
3. **Device/Asset** - Individual entity ownership
4. **User** - Account access levels

### Role Support
- System Administrator
- Tenant Administrator
- Customer User
- Custom roles
- Permission-based access

---

## Data Query System (EDQS)

### Entity Data Query System
- Advanced entity searching
- Relation-based queries
- Telemetry filtering
- Aggregation support
- Time-based queries
- Pagination support

### Supported Entity Types
- Device
- Asset
- Tenant
- Customer
- User
- Edge
- Dashboard
- EntityView
- OtaPackage
- And more...

---

## Notification System

### Notification Channels
- Email (SMTP)
- SMS (Twilio, AWS SNS, SMPP)
- Slack
- Custom webhooks
- Internal notifications

### Trigger Types
- Alarm creation
- Alarm clearing
- Alarm acknowledgment
- Alarm assignment
- Device activity (connect/disconnect)
- Rule engine events

### Notification Features
- Template-based messages
- Dynamic variable substitution
- Recipient filtering
- Delivery tracking
- Retry logic

---

## Calculated Fields

### Features
- Script-based field calculation
- TBEL (ThingsBoard Expression Language) support
- JavaScript execution
- Aggregation functions (SUM, AVG, COUNT, etc.)
- Time-series rolling calculations
- Geofencing support
- Alarm-based calculations
- State management

---

## Job Scheduling

### Job Types
- Device telemetry export
- Asset management tasks
- Data cleanup
- Report generation
- Custom scheduled tasks

### Scheduling Options
- Cron expressions
- Fixed delay/rate
- One-time execution
- Recurring schedules

---

## OTA (Over-The-Air) Updates

### Features
- Firmware package management
- Firmware distribution to devices
- Update progress tracking
- Rollback capabilities
- Version control
- Device groups support

---

## API Usage Tracking

### Tracked Metrics
- API call count
- Data point ingestion
- Storage usage
- Concurrent connections
- Request rate
- Error rates

### Usage Limits
- Per-tenant quotas
- Per-customer limits
- Rate limiting
- Quota alerts
- Usage-based billing integration

---

## Version Control & Export/Import

### Features
- Entity versioning
- Change tracking
- Rollback capability
- Bulk export
- Bulk import
- Conflict resolution
- Merge support

---

## Performance Features

### Caching
- Redis-based distributed caching
- Local guava caching
- Cache invalidation
- Entity caching
- Telemetry caching

### Optimization
- Database query optimization
- Index usage
- Partitioning support
- Connection pooling
- Message batching

### Scalability
- Kafka for event streaming
- Horizontal scaling support
- Load balancing ready
- Microservices architecture
- gRPC for inter-service communication

---

## Security Best Practices

1. **JWT Token Management**
   - Secure token signing
   - Token expiration
   - Refresh token rotation
   - Algorithm configuration

2. **Database Security**
   - SQL injection prevention (parameterized queries)
   - Column-level encryption ready
   - Audit logging
   - Access control

3. **Transport Security**
   - HTTPS/TLS support
   - DTLS for CoAP/LWM2M
   - Certificate pinning
   - Mutual TLS support

4. **API Security**
   - Request validation
   - Rate limiting
   - CORS configuration
   - Input sanitization

---

## Common Usage Patterns

### 1. Device Telemetry Collection
```
Device → HTTP/MQTT → TelemetryController → Service → TimiseriesDAO → Database
```

### 2. Rule Chain Processing
```
Message → RuleEngine → RuleChain → RuleNodes → Actions → External Systems
```

### 3. Dashboard Updates
```
Service → WebSocket → Client → UI Update
```

### 4. Alarm Generation
```
Telemetry Data → RuleEngine → AlarmNode → AlarmService → AlarmController → Dashboard
```

### 5. Notification Delivery
```
Event → NotificationRule → NotificationService → Channel (Email/SMS/Slack) → User
```

---

## Key Configuration Files

- **pom.xml** - Maven build configuration
- **application.yml** - Spring Boot properties
- **security-config.java** - Spring Security setup
- **websocket-config.java** - WebSocket configuration
- **schema-*.sql** - Database initialization

---

## Entry Points for Development

1. **Adding a new Entity**
   - Create entity class in common/data
   - Create SQL entity in dao/model
   - Create DAO interface
   - Create JPA repository
   - Create service classes
   - Create controller with endpoints

2. **Adding a new REST Endpoint**
   - Create/extend controller
   - Add @RequestMapping methods
   - Add service layer logic
   - Add DAO operations
   - Document with Swagger annotations

3. **Creating a new Rule Node**
   - Extend RuleNode interface
   - Implement execute() method
   - Register with @Component
   - Document configuration

4. **Adding a new Transport Protocol**
   - Implement transport interface
   - Add device authentication
   - Implement message parsing
   - Register transport factory
   - Add to configuration

---

## Useful Commands

```bash
# Build the project
mvn clean install

# Run the application
mvn spring-boot:run

# Generate Swagger/OpenAPI docs
mvn springdoc-openapi:generate

# Run tests
mvn test

# Package for deployment
mvn package -DskipTests
```

---

## Documentation & Resources

- **OpenAPI/Swagger**: Available at `/swagger-ui.html`
- **API Docs**: Generated from controller annotations
- **Database Schema**: SQL files in dao/src/main/resources/sql/
- **Configuration**: application.yml and environment variables
- **Logs**: Check application.log for debugging

---

## Quick Stats

- **Controllers**: 59
- **API Endpoints**: 200+
- **Database Tables**: 34+
- **Services**: 55+
- **Rule Nodes**: 91
- **Repositories**: 84
- **Entity Models**: 97
- **Transport Protocols**: 6
- **Supported Databases**: 4+
- **Authentication Methods**: 4+


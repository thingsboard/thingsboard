# ThingsBoard Detailed Module Inventory

## Backend Module Summary

### 1. Core Application Module (`/application`)
**Size:** ~200 Java classes  
**Purpose:** Main Spring Boot application entry point

**Key Packages:**
- `controller/` - 40+ REST API controllers
- `service/` - 40+ business logic services
- `config/` - Spring Bean configuration
- `actors/` - Actor system implementation
- `exception/` - Custom exception handlers
- `utils/` - Utility functions
- `install/` - Database initialization

**Main Controllers:**
1. AuthController - Authentication & JWT
2. EntityQueryController - Entity queries
3. DeviceController - Device CRUD
4. AssetController - Asset CRUD
5. DashboardController - Dashboard operations
6. TenantController - Multi-tenancy
7. UserController - User management
8. RuleEngineController - Rule chains
9. TelemetryController - Timeseries data
10. AlarmController - Alarm management
11. AuditLogController - Audit trails
12. RpcV1Controller - RPC commands
13. NotificationController - Notifications
14. EdgeController - Edge devices
15. CalculatedFieldController - Computed fields
16. DeviceProfileController - Device profiles
17. AssetProfileController - Asset profiles
18. WidgetsBundleController - UI widgets
19. MobileAppBundleController - Mobile support
20. UiSettingsController - UI configuration
21. QrCodeSettingsController - QR codes
22. UsageInfoController - Usage tracking
23. EntityViewController - Entity views
24. AttributeController - Entity attributes
25. ComponentDescriptorController - Component registry
26. And 15+ more specialized controllers

**Key Services:**
- DeviceService, DeviceCacheService
- TelemetryService, TelemetrySubscriptionService
- EntityService
- RuleChainService, RuleEngineService
- NotificationService, NotificationTemplateService
- MailService, SmsService
- TenantService, UserService
- DashboardService
- AlarmService, AlarmCommentService
- TransportService
- SessionService
- SubscriptionService
- RpcService
- StateService
- EdgeService
- MobileService
- AiModelService
- CalculatedFieldService

---

### 2. Common Modules (`/common`)

#### 2.1 Actor Module
**Classes:** ~50  
**Purpose:** Actor system framework for distributed processing

**Key Classes:**
- ActorSystemContext
- TbActorMailbox
- TbActorRef
- ActorCreationStrategy
- ActorSystemLifecycle

**Used For:**
- Device actors (one per device)
- Tenant actors (per-tenant isolation)
- Rule chain actors
- Session actors
- Subscription actors

#### 2.2 Cache Module
**Classes:** ~30  
**Purpose:** Distributed caching layer

**Key Classes:**
- CacheConfiguration
- CacheManager
- EntityCache
- RuleChainCache
- DeviceCache

**Backends:**
- Redis (primary)
- In-memory (fallback)

#### 2.3 Cluster API Module
**Classes:** ~40  
**Purpose:** Inter-node cluster communication

**Key Classes:**
- ClusterServiceImpl
- ClusterRpcService
- PartitionService
- ShardKey
- ClusterMessage

#### 2.4 Data Module
**Classes:** ~150 (Entity models)  
**Purpose:** Core domain models

**Entity Models:**
- Device, DeviceProfile, DeviceInfo
- Asset, AssetProfile
- Dashboard, DashboardInfo
- User, UserSettings
- Tenant, TenantProfile
- Customer
- EntityView
- Alarm, AlarmTemplate
- RuleChain, RuleNode
- Attribute, AttributeKvEntry
- Telemetry, TsKvEntry
- Relation, EntityRelation
- Mobile entities

**Each entity includes:**
- Entity class
- EntityId wrapper
- DTO (Data Transfer Object)
- Search/Filter parameters

#### 2.5 DAO API Module
**Interfaces:** ~50  
**Purpose:** Data access abstraction

**Key DAOs:**
- EntityRepository
- DeviceRepository
- AssetRepository
- DashboardRepository
- UserRepository
- TenantRepository
- RuleChainRepository
- AlarmRepository
- EventRepository
- AuditLogRepository
- AttributeRepository
- TelemetryRepository

#### 2.6 Discovery API Module
**Classes:** ~20  
**Purpose:** Service discovery

**Key Components:**
- ServiceDiscovery
- ServiceId
- ServiceNode
- ServiceRegistry

#### 2.7 Edge API Module
**Classes:** ~30  
**Purpose:** ThingsBoard Edge device APIs

**Key Classes:**
- EdgeEvent
- EdgeEventActionType
- EdgeSyncRequest
- EdgeConfiguration

#### 2.8 EDQS Module
**Classes:** ~40  
**Purpose:** Event-driven query service

**Key Components:**
- EdqsSession
- EdqsQueryTemplate
- DynamicQuery
- QueryResultSet

#### 2.9 Message Module
**Classes:** ~80  
**Purpose:** Internal message types

**Message Types:**
- TbMsg (core message wrapper)
- TbMsgMetaData
- DataCollectionMsg
- TelemetryUploadMsg
- AttributeUpdateMsg
- ClusterEventMsg
- SessionEventMsg

**Message Routing:**
- Message envelope
- Message routing rules
- Message transformers

#### 2.10 Proto Module
**Files:** ~20 .proto files  
**Purpose:** Protocol Buffer definitions

**Key Proto Definitions:**
- device.proto
- telemetry.proto
- rule_chain.proto
- transport.proto
- cluster_api.proto
- dashboard.proto
- alarm.proto

#### 2.11 Queue Module
**Classes:** ~60  
**Purpose:** Message queuing abstraction

**Queue Implementations:**
- KafkaQueue
- RabbitMQQueue
- InMemoryQueue
- MockQueue

**Queue Types:**
- Main queue
- Rule engine queue
- Transport queue
- Telemetry queue
- Notification queue

#### 2.12 Script Module
**Classes:** ~50  
**Purpose:** Script execution

**Script Types:**
- JavaScript (Nashorn/Node.js)
- TBEL (ThingsBoard Expression Language)
- Transformation scripts
- Filter scripts
- Action scripts

**Key Classes:**
- ScriptExecutor
- RemoteJsExecutor
- TbelExecutor
- ScriptEngine
- CompilationUnit

#### 2.13 Stats Module
**Classes:** ~40  
**Purpose:** Statistics collection

**Metrics Collected:**
- Message counts
- API call counts
- Telemetry ingestion rate
- Device connection statistics
- Rule engine execution metrics

#### 2.14 Transport Module
**Classes:** ~80  
**Purpose:** Transport protocol abstraction

**Supported Protocols:**
- MQTT
- HTTP
- CoAP
- LWM2M
- SNMP

**Key Classes:**
- TransportService
- DeviceTransportState
- SessionMsgProcessor
- TransportDeviceSessionContext

#### 2.15 Util Module
**Classes:** ~100  
**Purpose:** Common utilities

**Utilities:**
- JSON parsing
- UUID generation
- Timestamp handling
- Math utilities
- Collection utilities
- String utilities
- File utilities

#### 2.16 Version Control Module
**Classes:** ~80  
**Purpose:** Git-based version control

**Key Components:**
- VersionControlService
- RepositoryService
- CommitService
- BranchService
- EntityVersioning

---

### 3. DAO Module (`/dao`)
**Size:** ~100+ Java classes  
**Purpose:** Data access layer implementation

**Key Packages:**
- `device/` - Device DAOs
- `asset/` - Asset DAOs
- `dashboard/` - Dashboard DAOs
- `user/` - User DAOs
- `tenant/` - Tenant DAOs
- `telemetry/` - Timeseries DAOs
- `alarm/` - Alarm DAOs
- `event/` - Event DAOs
- `audit/` - Audit log DAOs
- `attributes/` - Attribute DAOs
- `relation/` - Entity relation DAOs
- `rulechain/` - Rule chain DAOs
- `widget/` - Widget DAOs
- `customer/` - Customer DAOs

**Implementation Types:**
- SQL DAOs (PostgreSQL, MySQL)
- Cassandra DAOs (timeseries)
- Timescale DAOs (timeseries)
- Hybrid DAOs (SQL + Cassandra)

---

### 4. Rule Engine Module (`/rule-engine`)

#### 4.1 Rule Engine API
**Classes:** ~50  
**Purpose:** Rule engine interfaces and abstract classes

**Key Classes:**
- TbNode
- TbNodeComponent
- RuleNodeConfiguration
- RuleNodeFactory
- RuleEngineContext
- TbContext
- TbMsg

#### 4.2 Rule Engine Components
**Node Types:** 50+ different node implementations

**Node Categories:**

1. **Filter Nodes (8)**
   - TbJsFilterNode
   - TbCheckExistenceNode
   - TbIsNewNode
   - TbHasPropertyNode
   - TbCheckEntityNode
   - TbSwitchNode
   - TbTenantCheckNode
   - TbOriginatorCheckNode

2. **Action Nodes (12)**
   - TbSendEmailNode
   - TbSendSmsNode
   - TbCreateAlarmNode
   - TbClearAlarmNode
   - TbAddAttributesNode
   - TbDeleteAttributesNode
   - TbSaveAttributesNode
   - TbCreateRpcCallRequestNode
   - TbCreateAttributeUpdateNode
   - TbUpdateMobileAttribute
   - TbEmailNotificationNode
   - TbSendToCustomIntegrationNode

3. **Telemetry Nodes (3)**
   - TbSaveTimeseriesNode
   - TbGetTimeseriesNode
   - TbTimeseriesLatestNode

4. **Transform Nodes (5)**
   - TbTransformNode
   - TbJsTransformNode
   - TbCalculateClientAttributesNode
   - TbCalculateServerAttributesNode
   - TbChangeOriginatorNode

5. **Flow Control (5)**
   - TbSwitchNode
   - TbSplitNode
   - TbMergeNode
   - TbLoopNode
   - TbDelayNode

6. **External Service Nodes (5)**
   - TbRestApiCallNode
   - TbMqttNode
   - TbKafkaNode
   - TbRabbitMqNode
   - TbSendRestApiCallReplyNode

7. **Cloud Integration (4)**
   - TbGcpPubSubNode
   - TbAwsSqsNode
   - TbAwsSnsNode
   - TbAwsDynamodbNode

8. **Geolocation Nodes (3)**
   - TbGpsGeofencingFilterNode
   - TbGpsGeofencingActionNode
   - TbBoundingBoxGeofencingNode

9. **AI Nodes (2)**
   - TbTweetsAnalysisNode
   - TbGenerateAiCompletionNode

10. **Debug Nodes (2)**
    - TbLogNode
    - TbDebugNode

11. **Data Routing (2)**
    - TbOriginatorAttrsGetterNode
    - TbAttributesNodeMsgRoute

---

### 5. Transport Modules (`/transport`)

#### 5.1 MQTT Transport
**Classes:** ~60  
**Purpose:** MQTT protocol support

**Key Components:**
- MqttTransportHandler
- MqttDeviceSession
- ProtocolVersion (3.1.1, 5.0)
- MqttTopicMatcher
- PublishProcessor
- SubscribeProcessor

**Features:**
- QoS 0, 1, 2 support
- Persistent sessions
- Topic subscriptions
- Retained messages
- MQTT 5.0 compatibility

#### 5.2 HTTP Transport
**Classes:** ~40  
**Purpose:** HTTP/HTTPS support

**Key Components:**
- HttpTransportHandler
- HttpSessionHandler
- JsonPayloadProcessor
- DataFormat (JSON, Protobuf)
- IotDashboardAdapter

#### 5.3 CoAP Transport
**Classes:** ~50  
**Purpose:** Constrained Application Protocol

**Key Components:**
- CoapTransportResource
- CoapTransportHandler
- CoapDevice
- DefaultBlockHandler
- ObserveHandler

#### 5.4 LWM2M Transport
**Classes:** ~70  
**Purpose:** Lightweight M2M protocol (IoT)

**Key Components:**
- Lwm2mTransportHandler
- Lwm2mDevice
- ObjectModel
- ResourceModel
- Lwm2mCredentialsSecurityInfoValidator

#### 5.5 SNMP Transport
**Classes:** ~40  
**Purpose:** Simple Network Management Protocol

**Key Components:**
- SnmpTransportHandler
- SnmpDevice
- TrapHandler
- MibParser
- SnmpValueConverter

---

## Frontend Module Summary

### 1. Core Module (`/ui-ngx/src/app/core`)
**Components:** ~50  
**Purpose:** Application core services and configuration

**Key Directories:**
- `api/` - HTTP client services (device, dashboard, telemetry, etc.)
- `auth/` - Authentication & JWT token management
- `guards/` - Route guards (auth, permission)
- `http/` - HTTP client configuration
- `interceptors/` - Request/response interceptors
- `local-storage/` - Browser storage abstraction
- `services/` - Core services (entity, tenant, user, etc.)
- `settings/` - Application settings service
- `translate/` - i18n translation setup
- `ws/` - WebSocket client
- `notification/` - Toast notification service
- `operator/` - Custom RxJS operators
- `meta-reducers/` - NgRx meta-reducers

**Key API Services:**
- EntityService
- DeviceService
- AssetService
- DashboardService
- TelemetryService
- AlarmService
- UserService
- TenantService
- RuleChainService
- AttributeService
- NotificationService

### 2. Modules (`/ui-ngx/src/app/modules`)
**Features:** Feature-based lazy-loaded modules

**Available Modules:**
1. **common/** - Common UI components
   - Shared components
   - Directives
   - Pipes

2. **dashboard/** - Dashboard creation and editing
   - Dashboard editor
   - Widget container
   - Layout engine
   - Widget library

3. **home/** - Home and administration
   - Home page
   - Customer management
   - Tenant management
   - Device management
   - Asset management
   - Configuration pages

4. **login/** - Authentication
   - Login form
   - Registration
   - Password reset
   - OAuth2 integration

### 3. Shared Module (`/ui-ngx/src/app/shared`)
**Components:** 100+ reusable components

**Key Subdirectories:**
- `components/` - UI components
  - Dialogs
  - Forms
  - Tables
  - Cards
  - Headers
  - Sidebars
  - Modals
  - Buttons

- `directives/` - Custom directives
  - Click-outside
  - Scroll
  - Focus
  - Highlight
  - Throttle

- `pipes/` - Custom pipes
  - Date formatting
  - Time formatting
  - Icon lookup
  - Ellipsis
  - Abbreviation
  - Safe URL

- `models/` - TypeScript interfaces
  - Entity models
  - API request/response models
  - UI state models
  - Configuration models

- `services/` - Shared services
  - Toast notifications
  - Dialog service
  - Date service
  - UUID service
  - File service

- `utils/` - Utility functions
  - Date utilities
  - String utilities
  - Array utilities
  - Object utilities
  - Validation utilities

- `widgets/` - Dashboard widgets
  - Chart widgets
  - Gauge widgets
  - Indicator widgets
  - Map widgets
  - Table widgets
  - Status widgets

- `ace/` - Ace editor integration
  - Editor configuration
  - Language definitions
  - TBEL language definition
  - Snippet support

---

## File Organization Summary

### Backend Statistics
- **Total Java Files:** ~1000+
- **Lines of Code:** ~500K+
- **Maven Modules:** 25+
- **Configuration Files:** 100+
- **Test Files:** ~500

### Frontend Statistics
- **Total TypeScript/JavaScript Files:** ~300+
- **Lines of Code:** ~100K+
- **Angular Components:** ~100+
- **Services:** ~50+
- **Module Files:** 4
- **CSS/SCSS Files:** ~200+

### Configuration Files
- **application/src/main/resources/thingsboard.yml** - Main configuration (2058 lines)
- **ui-ngx/angular.json** - Angular build configuration
- **ui-ngx/tailwind.config.js** - Tailwind CSS configuration
- **ui-ngx/tsconfig.json** - TypeScript configuration
- **pom.xml** - Root Maven POM
- **docker-compose.yml** - Multi-service composition
- **build.sh** - Build script
- **package.json** - Dependencies (UI)

---

## Key Metrics

| Metric | Count | Notes |
|--------|-------|-------|
| Java Classes | 1000+ | Core application code |
| REST Endpoints | 200+ | API surface |
| Services | 40+ | Business logic |
| Controllers | 40+ | API handlers |
| DAOs/Repositories | 50+ | Data access |
| Entity Models | 50+ | Domain models |
| Rule Engine Nodes | 50+ | Processing nodes |
| Components (Frontend) | 100+ | React-equivalent |
| Services (Frontend) | 50+ | Business logic |
| Configuration Keys | 300+ | Tunable parameters |
| Database Tables | 60+ | Schema tables |
| Microservices | 10 | Separate artifacts |
| Docker Images | 10+ | Container images |
| Supported Protocols | 5 | MQTT, HTTP, CoAP, LWM2M, SNMP |
| Message Types | 30+ | Internal messaging |
| Auth Mechanisms | 3 | JWT, OAuth2, Basic |

---

## Dependency Graph

```
application (main)
├── common (all modules)
├── dao
├── rule-engine (both api & components)
├── transport (mqtt, http, coap, lwm2m, snmp)
├── rest-client
└── tools

common (shared libraries)
├── actor
├── cache
├── cluster-api
├── coap-server
├── dao-api
├── data
├── discovery-api
├── edge-api
├── edqs
├── message
├── proto
├── queue
├── script
├── stats
├── transport
├── util
└── version-control

rule-engine
├── rule-engine-api
└── rule-engine-components

transport (per-protocol)
├── mqtt
├── http
├── coap
├── lwm2m
└── snmp

msa (microservices)
├── tb (core)
├── tb-node (compute)
├── js-executor (Node.js)
├── web-ui (Express.js)
├── transport (protocol handlers)
├── rule-engine (rule execution)
├── vc-executor (version control)
├── edqs (query engine)
├── monitoring (health checks)
└── black-box-tests (integration tests)

ui-ngx (Angular frontend)
├── core (services)
├── modules (features)
└── shared (components)
```

---

## Document Version: 1.0
**Last Updated:** 2025-11-16

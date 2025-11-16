# ThingsBoard Architecture Analysis Report
**Version:** 4.3.0-SNAPSHOT  
**Analysis Date:** 2025-11-16  
**Project Location:** /home/user/thingsboard

---

## 1. CURRENT BACKEND TECHNOLOGY STACK

### Primary Technology: Java 17
- **Framework:** Spring Boot 3.4.10
- **Build System:** Apache Maven (multi-module project)
- **Package Management:** Maven Central Repository
- **Compilation:** Java 17 (maven.compiler.source and target)
- **Packaging:** JAR (Spring Boot standalone executable JARs)

### Key Java Dependencies:
- **Spring Boot Ecosystem:**
  - spring-boot: 3.4.10
  - spring-data-jpa
  - spring-security
  - spring-web
  - spring-scheduling (async & scheduled tasks)

- **Database Connectors:**
  - PostgreSQL (primary SQL database)
  - Cassandra 5.0.4 (NoSQL timeseries)
  - TimescaleDB (PostgreSQL extension for timeseries)
  - HSQLDB (embedded testing)
  - Redis 5.1.5 (via Jedis)

- **Messaging & Streaming:**
  - Apache Kafka 3.9.1
  - RabbitMQ support
  - Zookeeper 3.9.3 (cluster coordination)

- **IoT Protocols:**
  - MQTT (Netty-MQTT custom implementation)
  - HTTP/HTTPS
  - CoAP (California Institute of Technology)
  - LWM2M (Leshan 2.0.0-M15)
  - SNMP 3.8.0

- **Actor System:**
  - Custom actor-based architecture for distributed processing
  - Thread pool executors for concurrent operations
  - Akka-like patterns without Akka dependency

- **Security:**
  - JWT (JSON Web Tokens) - JJWT 0.12.5
  - Auth0 JWT library
  - OAuth2 support
  - Spring Security
  - BouncyCastle 1.78.1 (cryptography)

- **Data Processing:**
  - TBEL (ThingsBoard Expression Language) 1.2.8
  - JavaScript (Nashorn - deprecated local, Node.js remote)
  - Protocol Buffers 3.25.5
  - gRPC 1.68.1

- **Utilities:**
  - Lombok 1.18.38 (code generation)
  - Guava 33.1.0-jre (collections & utilities)
  - Commons libraries (lang3, io, csv, beanutils)
  - JSON Schema validation
  - JGit 6.10.1.202505221210-r (git operations)

- **Monitoring & Logging:**
  - SLF4J + Logback (logging)
  - Micrometer metrics
  - DropWizard Metrics 4.2.25
  - Elasticsearch support (audit logs)

### Version Information:
- **Current Version:** 4.3.0-SNAPSHOT
- **Java Version:** 17+
- **License:** Apache 2.0

---

## 2. CURRENT FRONTEND TECHNOLOGY STACK

### Primary Technology: Angular 18
- **Framework:** Angular 18.2.13
- **Build Tool:** Angular CLI 18.2.12
- **Module Bundler:** esbuild (via @angular-builders/custom-esbuild)
- **Package Manager:** Yarn (yarn.lock found)
- **Language:** TypeScript 5.5.4
- **Styling:** SCSS + Tailwind CSS 3.4.15
- **Target:** Browser-based SPA (Single Page Application)

### Key Frontend Dependencies:
- **UI Framework:**
  - Angular Material 18.2.14 (Material Design)
  - Angular CDK 18.2.14 (Component Dev Kit)
  - Angular Animations (for transitions)
  - RxJS 7.8.1 (reactive programming)

- **State Management:**
  - NgRx 18.1.1 (@ngrx/store, @ngrx/effects)
  - NgRx Store DevTools

- **Charting & Visualization:**
  - ECharts 5.5.1 (custom ThingsBoard fork)
  - Flot (plotting library, custom fork)
  - Canvas Gauges 2.1.7
  - Leaflet 1.9.4 (mapping library)
  - MapLibre GL 5.2.0 (vector maps)
  - Leaflet extensions (geoman, marker cluster, etc.)

- **Data & UI Components:**
  - Angular Forms (reactive & template-driven)
  - Angular Router
  - Angular Common HTTP
  - @ngx-translate (i18n support) 15.0.0
  - ngx-clipboard
  - ngx-drag-drop
  - ngx-markdown
  - ngx-sharebuttons

- **Code Editors:**
  - Ace Editor 1.36.5 (code editing)
  - TinyMCE 6.8.5 (rich text editor)
  - JS Beautify 1.15.1

- **Date & Time:**
  - Day.js 1.11.13 (date manipulation)
  - Moment.js 2.30.1 (legacy, still used)
  - Moment Timezone 0.5.45
  - @mat-datetimepicker 14.0.0

- **Other Libraries:**
  - jQuery 3.7.1 (legacy)
  - jquery.terminal 2.44.1
  - jstree 3.3.17 (tree widget)
  - jszip 3.10.1 (ZIP handling)
  - QRCode 1.5.4
  - html2canvas 1.4.1
  - SVG libraries (svg.js, svg.filter.js, svg.panzoom.js)
  - Leaflet extensions (polyline decorator, provider, googlemutant, etc.)

### Build Configuration:
- **Angular.json** configuration for esbuild custom plugin
- **Tailwind CSS** for utility-first styling
- **ESLint** for code linting
- **TypeScript** strict mode
- **Asset management:** includes Material Design Icons (MDI), Ace workers, TinyMCE assets

### Output:
- **Build Target:** target/generated-resources/public
- **Deployment:** Static assets served by backend or separate web server

---

## 3. MAIN BACKEND MODULES & COMPONENTS

### Core Application Module
**Location:** `/application`
**Purpose:** Main Spring Boot application entry point

#### Key Components:

**1. Controllers (REST API)**
- **EntityQueryController** - Query entities and data
- **DeviceController** - Device management
- **AssetController** - Asset management
- **DashboardController** - Dashboard CRUD operations
- **TenantController** - Multi-tenancy management
- **UserController** - User management
- **RuleEngineController** - Rule chain management
- **TelemetryController** - Timeseries data
- **AlarmController** - Alarm management
- **AuditLogController** - Audit logging
- **RpcV1Controller** - RPC command handling
- **NotificationController** - Notification management
- **EdgeController** - ThingsBoard Edge device management
- **CalculatedFieldController** - Computed fields
- **AuthController** - Authentication & JWT
- And 30+ more specialized controllers

**2. Services**
- **Device Services:** device lifecycle, connectivity, provisioning
- **Telemetry Services:** timeseries data ingestion & querying
- **Entity Services:** CRUD for all entity types
- **Rule Engine Services:** rule chain execution
- **Notification Services:** email, SMS, push notifications
- **Mail Services:** SMTP integration
- **SMS Services:** SMS provider integration
- **Transport Services:** protocol handling (MQTT, HTTP, CoAP, LWM2M)
- **Session Services:** WebSocket session management
- **Subscription Services:** real-time data subscriptions
- **RPC Services:** Remote Procedure Calls
- **State Services:** Device state tracking
- **Edge Services:** Edge gateway support
- **Mobile Services:** Mobile app support
- **AI Services:** AI/ML model integration (LangChain4j 1.1.0)
- **Calculated Field Services:** Dynamic field computation

**3. DAO (Data Access Objects)**
**Location:** `/dao`
- SQL DAOs (using JPA/Hibernate)
- Cassandra DAOs (NoSQL)
- Timescale DAOs (timeseries specific)
- Entity repositories for all data types

**4. Actor System**
**Location:** `/application/src/main/java/org/thingsboard/server/actors`
- Device actors (one per device for state management)
- Tenant actors (per-tenant isolation)
- Rule chain actors (distributed rule engine)
- Session actors (WebSocket connections)
- Subscription actors (data subscriptions)

### Common Modules
**Location:** `/common`

1. **actor** - Actor system base classes
2. **cache** - Caching layer (Redis integration)
3. **cluster-api** - Inter-node communication
4. **data** - Entity models (Device, Asset, Dashboard, etc.)
5. **dao-api** - DAO interfaces
6. **discovery-api** - Service discovery
7. **edge-api** - Edge gateway APIs
8. **edqs** - Query execution engine
9. **message** - Message models & serialization
10. **proto** - Protocol Buffer definitions
11. **queue** - Message queuing abstractions
12. **script** - Script execution (JS/TBEL)
13. **stats** - Statistics collection
14. **transport** - Transport protocol abstractions
15. **util** - Common utilities
16. **version-control** - Git-based version control

### Rule Engine
**Location:** `/rule-engine`

**Components by Category:**
- **action** - Send email, SMS, HTTP calls
- **ai** - AI model integration nodes
- **aws** - AWS services (SQS, SNS, DynamoDB)
- **credentials** - Credential management
- **debug** - Debug node for troubleshooting
- **deduplication** - Message deduplication
- **delay** - Message delay
- **edge** - Edge gateway integration
- **external** - External service calls
- **filter** - Message filtering
- **flow** - Flow control (switch, split, merge)
- **gcp** - Google Cloud Platform (Pub/Sub, Firestore)
- **geo** - Geofencing & geolocation
- **kafka** - Kafka producer node
- **mail** - Email sending
- **math** - Mathematical operations
- **metadata** - Metadata enrichment
- **mqtt** - MQTT publisher node
- **notification** - Notification rules
- **profile** - Device profile rules
- **rabbitmq** - RabbitMQ publisher
- **rest** - REST API calls
- **rpc** - Device RPC calls
- **sms** - SMS sending
- **telemetry** - Telemetry storage
- **transaction** - Transaction handling
- **transform** - Data transformation

### Transport Layer
**Location:** `/transport` and `/msa/transport`

Separate microservice per protocol:
- **MQTT** - Lightweight IoT protocol
- **HTTP** - RESTful API & HTTPS
- **CoAP** - Constrained Application Protocol
- **LWM2M** - Lightweight M2M (IoT)
- **SNMP** - Simple Network Management Protocol

Each transport has:
- Protocol-specific handler
- Conversion to internal message format
- Rate limiting & security

### Microservices Architecture (MSA)
**Location:** `/msa`

**Services:**
1. **tb** - Core ThingsBoard service
2. **tb-node** - Dedicated TB node
3. **rule-engine** - Dedicated rule engine nodes
4. **transport (mqtt/http/coap/lwm2m/snmp)** - Dedicated transport services
5. **js-executor** - Remote JavaScript executor (Node.js)
6. **web-ui** - Frontend serving (Express.js)
7. **vc-executor** - Version control executor (Git operations)
8. **edqs** - Event-driven query service
9. **monitoring** - Health & metrics monitoring
10. **black-box-tests** - Integration test suite

---

## 4. MAIN FRONTEND MODULES & COMPONENTS

### Application Structure
**Location:** `/ui-ngx/src/app`

**Main Directories:**
1. **core** - Application core services & configuration
2. **modules** - Feature modules (lazy-loaded)
3. **shared** - Shared components & utilities

### Core Module (`/core`)
- **api** - HTTP client services
- **auth** - Authentication & JWT token management
- **guards** - Route guards (auth, permission)
- **http** - Custom HTTP interceptors
- **interceptors** - HTTP request/response interceptors
- **local-storage** - Browser storage service
- **services** - Core business logic services
- **settings** - Application settings
- **translate** - i18n translation service
- **ws** - WebSocket client
- **notification** - Toast notifications
- **operator** - RxJS operators
- **css** - Global styles
- **meta-reducers** - NgRx store meta reducers

### Feature Modules (`/modules`)
1. **common** - Common components
2. **dashboard** - Dashboard creation & editing
3. **home** - Home/landing page
4. **login** - Authentication UI

### Shared Module (`/shared`)
- **components** - Reusable UI components
- **directives** - Custom directives
- **pipes** - Custom pipes
- **models** - TypeScript interfaces/models
- **services** - Shared services
- **utils** - Utility functions
- **widgets** - Custom dashboard widgets
- **ace** - Ace editor configuration
- **models/ace/tbel** - TBEL language definition

### Assets
- **map** - Leaflet map resources
- **widget** - Widget libraries & definitions
- **dashboard** - Dashboard templates
- **jstree** - Tree widget resources
- **metadata** - Icon & type metadata
- **markers** - Map markers
- **fonts** - Custom fonts
- **locale** - i18n language files
- **home** - Home page assets
- **help** - Help documentation assets

---

## 5. BUILD SYSTEMS

### Backend Build System
**Tool:** Apache Maven 3.x+
**Type:** Multi-module Maven project

**Root POM:** `/pom.xml`
- Defines parent configuration, dependencies, plugins
- Modules: application, common, dao, rule-engine, transport, tools, rest-client, etc.

**Build Process:**
1. Maven compilation (Java 17)
2. Dependency resolution
3. Code generation (Protocol Buffers)
4. JAR packaging
5. Docker image creation
6. Platform-specific installers (Windows, Unix)

**Key Plugins:**
- maven-compiler-plugin (Java 17)
- maven-shade-plugin (JAR bundling)
- maven-assembly-plugin (distribution packaging)
- protobuf-maven-plugin (Protocol Buffer code generation)
- maven-surefire-plugin (testing)

**Build Script:** `/build.sh` - Shell script for building entire project
**Proto Build:** `/build_proto.sh` - Protocol Buffer compilation

### Frontend Build System
**Tool:** Angular CLI 18.2.12 with custom esbuild configuration
**Type:** Single Angular application with esbuild bundler

**Build Configuration:** `/ui-ngx/angular.json`
- Custom esbuild plugins
- Asset handling (icons, workers, libraries)
- Production & development configurations

**Package Manager:** Yarn
**Lock File:** `/ui-ngx/yarn.lock`

**Build Commands:**
```bash
npm run build:prod        # Production build
npm run build:types      # Type generation
npm run build:icon-metadata  # Icon metadata
npm run lint             # ESLint
npm run start           # Development server
```

**Build Output:** 
- **Location:** `/ui-ngx/target/generated-resources/public`
- **Format:** Minified JavaScript, CSS, HTML, assets
- **Integration:** Copied to backend JAR for serving

### Additional Build Configuration

**Gradle Builds:**
- `/packaging/java/build.gradle` - Java packaging
- `/packaging/js/build.gradle` - JavaScript packaging

**Docker Builds:**
- Multiple Dockerfiles for different services
- Docker Compose configuration: `/docker/docker-compose.yml`
- Platform-specific Docker images (Linux x64, Windows, etc.)

---

## 6. PROJECT STRUCTURE OVERVIEW

```
thingsboard/
├── application/                    # Main Spring Boot app
│   ├── src/main/java/
│   │   └── org/thingsboard/server/
│   │       ├── controller/        # REST API endpoints
│   │       ├── service/           # Business logic
│   │       ├── config/            # Spring configuration
│   │       ├── actors/            # Actor system
│   │       └── utils/             # Utilities
│   └── src/main/resources/
│       └── thingsboard.yml        # Main configuration (2000+ lines)
│
├── common/                         # Shared modules
│   ├── actor/                     # Actor framework
│   ├── cache/                     # Redis caching
│   ├── data/                      # Entity models
│   ├── dao-api/                   # DAO interfaces
│   ├── message/                   # Message models
│   ├── queue/                     # Message queuing
│   ├── transport/                 # Protocol abstractions
│   └── ... (14 more modules)
│
├── dao/                            # Data Access Layer
│   └── src/main/java/
│       └── org/thingsboard/server/dao/
│           ├── device/            # Device repositories
│           ├── dashboard/         # Dashboard repositories
│           ├── timeseries/        # Timeseries DAOs
│           └── ... (30+ more DAOs)
│
├── rule-engine/                    # Rule Engine
│   ├── rule-engine-api/           # Interfaces
│   └── rule-engine-components/
│       └── src/main/java/org/thingsboard/rule/engine/
│           ├── action/            # 50+ rule node types
│           ├── ai/
│           ├── aws/
│           ├── geo/
│           ├── kafka/
│           └── ... (25 more categories)
│
├── transport/                      # Transport protocols
│   ├── mqtt/                      # MQTT
│   ├── http/                      # HTTP
│   ├── coap/                      # CoAP
│   ├── lwm2m/                     # LWM2M
│   └── snmp/                      # SNMP
│
├── msa/                            # Microservices Architecture
│   ├── tb/                        # Core service
│   ├── tb-node/                   # Dedicated nodes
│   ├── js-executor/               # JavaScript executor (Node.js)
│   ├── web-ui/                    # Frontend server (Express.js)
│   ├── transport/                 # Transport services
│   ├── vc-executor/               # Version control
│   ├── edqs/                      # Query service
│   └── monitoring/                # Health monitoring
│
├── ui-ngx/                         # Angular Frontend
│   ├── src/
│   │   ├── app/
│   │   │   ├── core/              # Core services
│   │   │   ├── modules/           # Feature modules
│   │   │   └── shared/            # Shared components
│   │   ├── assets/                # Static assets
│   │   ├── theme/                 # Theme configuration
│   │   └── environments/          # Build environments
│   ├── angular.json               # Build config
│   ├── package.json               # Dependencies
│   └── tsconfig.json              # TypeScript config
│
├── docker/                         # Docker configuration
│   ├── docker-compose.yml         # Multi-service composition
│   └── README.md
│
├── packaging/                      # Platform-specific builds
│   ├── java/                      # Java packaging
│   └── js/                        # JavaScript packaging
│
├── pom.xml                         # Root Maven config
├── build.sh                        # Build script
├── build_proto.sh                  # Protocol Buffer build
└── README.md                       # Project documentation
```

---

## 7. TECHNOLOGY STACK SUMMARY TABLE

| Component | Current Technology | Version |
|-----------|-------------------|---------|
| **Backend Framework** | Spring Boot | 3.4.10 |
| **Backend Language** | Java | 17+ |
| **Build Tool (Backend)** | Maven | 3.x+ |
| **Frontend Framework** | Angular | 18.2.13 |
| **Frontend Bundler** | esbuild | (custom) |
| **Frontend Language** | TypeScript | 5.5.4 |
| **Package Manager (Frontend)** | Yarn | (locked) |
| **Primary Database** | PostgreSQL | 11+ |
| **NoSQL Database** | Cassandra | 5.0.4 |
| **Timeseries Database** | TimescaleDB/Cassandra | Variable |
| **Cache** | Redis | 5.1.5 |
| **Message Queue** | Kafka | 3.9.1 |
| **Cluster Coordination** | Zookeeper | 3.9.3 |
| **MQTT Protocol** | Netty-MQTT | Custom |
| **HTTP/REST** | Spring Web | 3.4.10 |
| **WebSocket** | Spring WebSocket | 3.4.10 |
| **Reactive** | RxJS | 7.8.1 |
| **State Management** | NgRx | 18.1.1 |
| **UI Components** | Angular Material | 18.2.14 |
| **Styling** | Tailwind CSS | 3.4.15 |
| **Logging** | SLF4J + Logback | Latest |
| **Testing (Backend)** | JUnit/TestNG | 7.10.1 |
| **Testing (Frontend)** | Jasmine/Karma | (configured) |
| **Container** | Docker | Latest |
| **Orchestration** | Docker Compose | Latest |

---

## 8. KEY ARCHITECTURAL PATTERNS

### 1. **Multi-Tenancy**
- Complete tenant isolation at application level
- Tenant-aware services and repositories
- Separate actor system hierarchies per tenant

### 2. **Microservices Architecture**
- Loosely coupled services
- Zookeeper for service discovery
- Inter-service communication via Kafka/gRPC
- Can scale individual services independently

### 3. **Actor Model**
- Lightweight concurrency model
- Device-per-actor pattern for state management
- Non-blocking message processing
- Thread pool per actor type

### 4. **Event-Driven Architecture**
- Kafka for event streaming
- Event sourcing patterns
- Rule engine as event processor
- Audit logging of all operations

### 5. **Domain-Driven Design**
- Clear service boundaries
- Entity models in common/data
- Service interfaces in *-api modules
- Implementation-specific in service modules

### 6. **REST API Design**
- Spring REST Controllers
- JSON request/response
- Pagination support
- Query DSL for complex queries

### 7. **Real-time Subscriptions**
- WebSocket-based subscriptions
- Server-sent updates
- Per-user subscription management
- Efficient message batching

### 8. **Caching Strategy**
- Redis for distributed cache
- Entity cache
- Rule engine cache
- Metrics cache

---

## 9. DATABASE SUPPORT

### SQL Databases
- **PostgreSQL** (primary, fully supported)
- **MySQL** (supported)
- **SQL Server** (supported)
- **H2/HSQLDB** (development/testing)

### NoSQL Databases
- **Cassandra** (timeseries & hybrid mode)
- **MongoDB** (considered)

### Timeseries Specific
- **Cassandra** (distributed timeseries)
- **TimescaleDB** (PostgreSQL extension)
- **InfluxDB** (integration)

### Caching
- **Redis** (primary cache store)
- **Memcached** (supported)

---

## 10. DEPLOYMENT ARCHITECTURE

### Single Node
- Embedded HSQLDB or PostgreSQL
- All services in one JVM
- Suitable for development/small scale

### High Availability Cluster
- Multiple TB-Core nodes
- Multiple Rule Engine nodes
- Dedicated Transport nodes
- Dedicated JS Executor nodes
- Shared PostgreSQL + Cassandra
- Zookeeper cluster
- Load balancer (Nginx/HAProxy)

### Containerized (Docker)
- Separate container per service
- Docker Compose for local development
- Kubernetes-ready (individual Dockerfiles)
- Network isolation via Docker networks

### Supported Deployment Platforms
- Linux (Debian, Ubuntu, CentOS, RHEL)
- Windows (via installer or Docker)
- macOS (development)
- Kubernetes (via Helm or manual manifests)
- AWS (CloudFormation/Terraform templates available)
- Azure (available)
- GCP (available)

---

## CONVERSION PLANNING NOTES

### Implications for Python + React Migration:

**Backend Complexity:**
- 30+ REST controllers to convert
- 40+ complex services with multi-tenancy logic
- Advanced caching & distributed processing
- Actor system behavior requires careful refactoring

**Frontend Considerations:**
- 50+ custom Angular components to convert
- Complex state management (NgRx → Redux/Context)
- 100+ npm dependencies to find React equivalents
- Material Design → Material-UI or similar
- Tailwind CSS (can keep)

**Architecture Preservation:**
- Core domain models remain similar
- REST API surface can stay compatible
- Database layer unchanged (PostgreSQL/Cassandra)
- Message queue integration (Kafka) unchanged
- Docker deployment unchanged

**High-Risk Areas:**
1. Actor-based concurrency → Python async/background jobs
2. Real-time subscriptions (WebSocket) → Socket.io or similar
3. Rule engine execution → Function wrapping or scripting engine
4. Multi-tenancy isolation → Need careful ORM configuration
5. Clustering/Zookeeper coordination → Requires careful refactoring

---

## CONCLUSION

ThingsBoard is a well-architected, enterprise-scale IoT platform with:
- **Mature Backend:** 9 years of Java/Spring development
- **Modern Frontend:** Latest Angular with Material Design
- **Scalable Architecture:** Microservices with proven clustering
- **Comprehensive Feature Set:** 40+ transport/rule engine components
- **Production Ready:** Used in enterprise deployments globally

The conversion to Python backend and React frontend is feasible but requires significant effort due to the platform's complexity and the need to maintain feature parity with the existing system.

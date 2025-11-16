# ThingsBoard Backend Codebase - Comprehensive Analysis

## 1. Backend Framework & Technology Stack

### Framework: FastAPI (Python)
- **Framework**: FastAPI 0.109.0 (modern, async, high-performance)
- **Server**: Uvicorn 0.27.0 (ASGI server)
- **Language**: Python 3.x
- **Architecture**: REST API with async/await patterns
- **Base URL**: `http://localhost:8080`
- **API Docs**: Available at `/swagger-ui` (development) and `/redoc`

### Supporting Infrastructure
- **Database**: PostgreSQL (primary relational database)
  - SQLAlchemy 2.0 with async support
  - Alembic for migrations
  - Connection pooling: 20 pool size, 40 max overflow
  
- **Time-Series Data**: Cassandra 3.29.0 (optional, for historical telemetry)
  
- **Cache**: Redis 5.0.1 (distributed caching and locks)
  
- **Message Queue**: Apache Kafka (event streaming)
  - Bootstrap servers configurable
  - Multiple topics for different data types
  
- **Authentication**: JWT tokens (python-jose)
  - Access tokens: 60 minutes (configurable)
  - Refresh tokens: 30 days (configurable)
  - Password hashing: bcrypt via passlib
  
- **WebSocket Support**: python-socketio 5.11.0 & websockets 12.0
  - Currently referenced in requirements but not yet fully integrated
  - TelemetrySubscription schema exists for WebSocket subscriptions

### IoT Protocols Support
- MQTT (paho-mqtt 1.6.1) - primary protocol
- CoAP (aiocoap 0.4.7)
- HTTP/HTTPS (aiohttp 3.9.1)
- LWM2M (optional)
- SNMP (optional)

---

## 2. Backend Directory Structure

```
backend-python/
├── app/
│   ├── __init__.py
│   ├── main.py                    # FastAPI application entry point
│   ├── api/
│   │   ├── v1/
│   │   │   ├── api.py            # Router aggregation
│   │   │   └── endpoints/        # API route handlers
│   │   │       ├── auth.py       # Authentication (IMPLEMENTED)
│   │   │       ├── devices.py    # Device management (IMPLEMENTED)
│   │   │       ├── customers.py  # Customer management (IMPLEMENTED)
│   │   │       ├── tenants.py    # Tenant management (IMPLEMENTED)
│   │   │       ├── users.py      # User management (TODO)
│   │   │       ├── assets.py     # Asset management (TODO)
│   │   │       ├── alarms.py     # Alarm management (TODO)
│   │   │       ├── dashboards.py # Dashboard management (TODO)
│   │   │       ├── rule_chains.py # Rule chain management (TODO)
│   │   │       └── telemetry.py  # Telemetry APIs (IMPLEMENTED)
│   ├── core/
│   │   ├── config.py             # Configuration management
│   │   ├── security.py           # JWT & auth utilities
│   │   ├── logging.py            # Logging setup
│   │   └── events.py             # App lifecycle events
│   ├── models/                   # SQLAlchemy database models
│   │   ├── base.py              # Base entity classes
│   │   ├── device.py            # Device entity
│   │   ├── device_profile.py    # Device profile entity
│   │   ├── asset.py             # Asset entity
│   │   ├── customer.py          # Customer entity
│   │   ├── tenant.py            # Tenant entity
│   │   ├── user.py              # User entity with roles
│   │   ├── alarm.py             # Alarm entity
│   │   ├── dashboard.py         # Dashboard entity
│   │   ├── rule_chain.py        # Rule chain entity
│   │   ├── entity_view.py       # Entity view entity
│   │   └── telemetry.py         # Telemetry data models
│   ├── schemas/                 # Pydantic request/response schemas
│   │   ├── auth.py
│   │   ├── device.py
│   │   ├── customer.py
│   │   ├── tenant.py
│   │   ├── asset.py
│   │   ├── telemetry.py
│   │   └── gateway.py
│   ├── db/
│   │   └── session.py           # Database session management
│   ├── services/
│   │   └── mqtt_connector.py    # MQTT connector service
│   └── rule_engine/             # Rule engine implementation
│       ├── core/
│       │   ├── rule_node_base.py
│       │   └── rule_engine_executor.py
│       ├── models/
│       └── nodes/               # Rule node implementations
│           ├── filter/          # Filter nodes (5 nodes)
│           ├── transformation/  # Transformation nodes (2 nodes)
│           ├── enrichment/      # Enrichment nodes (3 nodes)
│           └── action/          # Action nodes (6 nodes)
├── requirements.txt             # Python dependencies
├── Dockerfile                   # Docker configuration
├── docker-compose.yml           # Docker Compose setup
├── .env.example                 # Environment template
└── README.md                    # Documentation
```

---

## 3. API Routes & Controllers Overview

### API Version
All routes use `/api/` prefix with base structure: `/api/v1/{resource}`

### Implemented APIs (COMPLETE)

#### 1. Authentication (`/api/auth`)
**File**: `/home/user/thingsboard/backend-python/app/api/v1/endpoints/auth.py`
- `POST /auth/login` - User login (returns JWT tokens)
- `POST /auth/token/refresh` - Refresh access token
- `GET /auth/user` - Get current authenticated user
- `POST /auth/changePassword` - Change password
- `POST /auth/logout` - Logout endpoint

**Features**:
- JWT-based authentication
- Access token + Refresh token pattern
- Password verification with bcrypt
- User enable/disable control

#### 2. Device Management (`/api/devices`)
**File**: `/home/user/thingsboard/backend-python/app/api/v1/endpoints/devices.py`
- `POST /devices` - Create device
- `GET /devices/{device_id}` - Get device by ID
- `GET /devices` - List devices (paginated)
- `PUT /devices/{device_id}` - Update device
- `DELETE /devices/{device_id}` - Delete device
- `GET /devices/{device_id}/credentials` - Get device credentials
- `POST /devices/{device_id}/credentials` - Update device credentials

**Features**:
- Multi-tenant support with tenant isolation
- Device credentials management (ACCESS_TOKEN, X509, MQTT_BASIC, LWM2M)
- Customer-scoped device assignment
- Device profile linking
- Search text indexing
- Full CRUD operations
- Role-based access control (SYS_ADMIN, TENANT_ADMIN, CUSTOMER_USER)

#### 3. Customer Management (`/api/customers`)
**File**: `/home/user/thingsboard/backend-python/app/api/v1/endpoints/customers.py`
- `POST /customers` - Create customer
- `GET /customers/{customer_id}` - Get customer
- `GET /customers` - List customers (paginated)
- `PUT /customers/{customer_id}` - Update customer
- `DELETE /customers/{customer_id}` - Delete customer

**Features**:
- Full contact information (email, phone, address)
- Multi-tenant scoping
- Public customer flag
- Device and asset assignment
- Full CRUD operations

#### 4. Tenant Management (`/api/tenants`)
**File**: `/home/user/thingsboard/backend-python/app/api/v1/endpoints/tenants.py`
- `POST /tenants` - Create tenant (SYS_ADMIN only)
- `GET /tenants/{tenant_id}` - Get tenant
- `GET /tenants` - List all tenants (paginated)
- `PUT /tenants/{tenant_id}` - Update tenant
- `DELETE /tenants/{tenant_id}` - Delete tenant

**Features**:
- Top-level data isolation boundary
- Full contact and address information
- Region support for multi-region deployments
- Isolated TB Core and Rule Engine flags
- System admin only access

#### 5. Telemetry APIs (`/api/telemetry`) - HIGH PRIORITY
**File**: `/home/user/thingsboard/backend-python/app/api/v1/endpoints/telemetry.py`

**Time-Series Data**:
- `POST /telemetry/{entity_type}/{entity_id}/timeseries/{scope}` - Save telemetry
- `GET /telemetry/{entity_type}/{entity_id}/values/timeseries` - Get latest telemetry values
- `GET /telemetry/{entity_type}/{entity_id}/keys/timeseries` - Get available telemetry keys

**Attributes**:
- `POST /telemetry/{entity_type}/{entity_id}/attributes/{scope}` - Save attributes
- `GET /telemetry/{entity_type}/{entity_id}/attributes` - Get attributes (all scopes)
- `DELETE /telemetry/{entity_type}/{entity_id}/attributes/{scope}` - Delete attributes

**Features**:
- Support for multiple data types (bool, string, long, double, JSON)
- Attribute scopes: CLIENT_SCOPE, SERVER_SCOPE, SHARED_SCOPE
- Flexible telemetry format (simple key-value or time-series with timestamps)
- Latest value caching in PostgreSQL
- Historical data support (Cassandra/TimescaleDB ready)
- Polymorphic value storage
- Entity-based access control

---

### TODO/Pending APIs (NOT YET IMPLEMENTED)

#### 1. User Management (`/api/users`)
**Status**: STUB ONLY (TODO)
- Needs: Create, Read, Update, Delete, List users
- Features needed: Activation email, Password reset, User roles

#### 2. Asset Management (`/api/assets`)
**Status**: STUB ONLY (TODO)
- Needs: Full CRUD for assets
- Features needed: Asset type support, Asset profiles, Hierarchies

#### 3. Alarm Management (`/api/alarms`)
**Status**: STUB ONLY (TODO)
- Needed endpoints for alarm lifecycle management

#### 4. Dashboard Management (`/api/dashboards`)
**Status**: STUB ONLY (TODO)
- Widget management, Layout configuration

#### 5. Rule Chain Management (`/api/ruleChains`)
**Status**: STUB ONLY (TODO)
- Rule node graph management, Metadata persistence

---

## 4. Database Models & Entities

### Entity Hierarchy

**Base Classes**:
- `BaseEntity` - All entities (UUIDMixin, TimestampMixin)
- `TenantEntity(BaseEntity)` - Tenant-scoped (Device, Asset, Customer, etc.)
- `CustomerEntity(TenantEntity)` - Customer-scoped (Device, Asset, etc.)
- `NamedEntity(BaseEntity)` - Named entities with search text

### Core Entities

#### Device
- **Table**: `device`
- **Inheritance**: NamedEntity, CustomerEntity
- **Fields**: id, name, type, label, device_profile_id, device_data, firmware_id, software_id, external_id, tenant_id, customer_id, search_text, created_time
- **Relationships**: DeviceProfile, DeviceCredentials
- **Indexes**: tenant_id, customer_id, type, device_profile_id, search_text, external_id

#### DeviceProfile
- **Table**: `device_profile`
- **Inheritance**: NamedEntity, TenantEntity
- **Fields**: type (DEFAULT), transport_type (DEFAULT/MQTT/COAP/LWM2M/SNMP), description, is_default, default_rule_chain_id, default_dashboard_id, profile_data (JSON), transport_configuration (JSON)

#### Asset
- **Table**: `asset`
- **Inheritance**: NamedEntity, CustomerEntity
- **Fields**: type, label, asset_profile_id, additional_info (JSON), external_id

#### Customer
- **Table**: `customer`
- **Inheritance**: NamedEntity, TenantEntity
- **Fields**: title, email, phone, address, address2, city, state, zip, country, additional_info (JSON), is_public

#### Tenant
- **Table**: `tenant`
- **Inheritance**: NamedEntity
- **Fields**: region, tenant_profile_id, email, phone, address info, additional_info (JSON), isolated_tb_core, isolated_tb_rule_engine

#### User (TbUser)
- **Table**: `tb_user`
- **Inheritance**: BaseEntity, TenantEntity
- **Authority Levels**: SYS_ADMIN, TENANT_ADMIN, CUSTOMER_USER
- **Fields**: email, authority, customer_id, first_name, last_name, additional_info (JSON)
- **Relationships**: UserCredentials (1-to-1)

#### UserCredentials
- **Table**: `user_credentials`
- **Inheritance**: BaseEntity
- **Fields**: user_id, password (hashed), activate_token, reset_token, enabled, additional_info (JSON)

#### Alarm
- **Table**: `alarm`
- **Inheritance**: BaseEntity, TenantEntity
- **Severity**: CRITICAL, MAJOR, MINOR, WARNING, INDETERMINATE
- **Status**: ACTIVE_UNACK, ACTIVE_ACK, CLEARED_UNACK, CLEARED_ACK
- **Fields**: originator_type, originator_id, customer_id, type, severity, status, start_ts, end_ts, ack_ts, clear_ts, details (JSON), propagate flags

#### Telemetry Models
- **TsKvLatest** - Latest telemetry values (PostgreSQL)
- **AttributeKv** - Latest attributes (PostgreSQL)
- **DataTypes**: STRING, LONG, DOUBLE, BOOLEAN, JSON
- **Polymorphic Storage**: bool_v, str_v, long_v, dbl_v, json_v

#### Dashboard
- **Table**: `dashboard`
- **Fields**: title, image, mobile_hide, mobile_order, configuration (JSON), assigned_customers (JSON), additional_info (JSON), external_id

#### RuleChain
- **Table**: `rule_chain`
- **Type**: CORE, EDGE
- **Fields**: type, first_rule_node_id, root, debug_mode, configuration (JSON), additional_info (JSON), external_id

#### EntityView
- **Table**: `entity_view`
- **Fields**: entity_type, entity_id, type, keys (JSON), start_ts, end_ts, additional_info (JSON)

#### DeviceCredentials
- **Table**: `device_credentials`
- **Credentials Types**: ACCESS_TOKEN, X509_CERTIFICATE, MQTT_BASIC, LWM2M_CREDENTIALS
- **Fields**: device_id, credentials_type, credentials_id, credentials_value (encrypted)

---

## 5. WebSocket Support Status

### Current Status: DEPENDENCIES INSTALLED, CORE NOT YET IMPLEMENTED

**Available Dependencies**:
- `python-socketio==5.11.0`
- `websockets==12.0`

**WebSocket References Found**:
- `TelemetrySubscription` schema defined in telemetry.py (ready for WebSocket subscriptions)
- TODO comments in MQTT connector: "Send to WebSocket for real-time updates"
- MQTT service has hooks for real-time data processing

**What Needs Implementation**:
1. WebSocket endpoint handler
2. Connection manager for client subscriptions
3. Real-time telemetry broadcast on data updates
4. Subscription management (subscribe/unsubscribe)
5. Message routing from MQTT/rule engine to connected clients

**Architecture Ready For**:
- Async message streaming
- Real-time device telemetry updates
- Alarm notifications
- Rule engine event streaming
- Device online/offline status updates

---

## 6. Overall Backend Architecture

### Architecture Patterns

#### 1. **Layered Architecture**
```
API Layer (FastAPI Endpoints)
    ↓
Schema/Validation Layer (Pydantic)
    ↓
Service Layer (Business Logic)
    ↓
Database Layer (SQLAlchemy ORM)
    ↓
PostgreSQL + Cassandra
```

#### 2. **Multi-Tenancy**
- Tenant-level isolation via `tenant_id` fields
- Customer sub-tenancy support
- Role-based access control (RBAC)
- Authority levels: SYS_ADMIN, TENANT_ADMIN, CUSTOMER_USER

#### 3. **Authentication & Security**
- JWT token-based authentication
- Access token (60 min) + Refresh token (30 days)
- Bcrypt password hashing
- Bearer token scheme
- Per-endpoint authorization checks

#### 4. **Data Storage Strategy**
- **Relational Data**: PostgreSQL
  - Entities (Device, Asset, Customer, User, etc.)
  - Relationships
  - Latest telemetry/attributes (TsKvLatest, AttributeKv)
  
- **Time-Series Data**: Cassandra (optional)
  - Historical telemetry data
  - High-volume writes
  - Time-partition optimization
  
- **Caching**: Redis
  - Session caching
  - Distributed locks
  - Rate limiting data

- **Message Queue**: Kafka
  - Device data events
  - Telemetry distribution
  - Rule engine messages
  - Cross-service communication

#### 5. **IoT Protocol Integration**
- **MQTT Service**: `mqtt_connector.py`
  - Data converter support
  - JSONPath expression evaluation
  - Configurable topic filters
  - QoS support
  - Automatic device creation
  
- **Other Protocols**: Configurable via settings
  - CoAP, HTTP, LWM2M support flags

#### 6. **Rule Engine**
- **Type**: Node-based processing pipeline
- **Node Categories** (17 total nodes):
  - **Filter Nodes** (5):
    - Switch node (condition routing)
    - Originator type filter
    - Message type switch
    - Check relation node
    - Originator fields filter
  
  - **Transformation Nodes** (2):
    - JavaScript transformation
    - Rename keys node
  
  - **Enrichment Nodes** (3):
    - Originator attributes enrichment
    - Related attributes enrichment
    - Customer details enrichment
  
  - **Action Nodes** (6):
    - Save telemetry
    - Create alarm
    - Send email
    - RPC call
    - REST API call
    - Log node

- **Execution Model**:
  - Async message processing
  - Connection types: SUCCESS, FAILURE, TRUE, FALSE, OTHER
  - Debug mode support
  - Context-based execution

#### 7. **Dependency Injection**
- FastAPI `Depends()` for:
  - Database sessions
  - Current user (auth)
  - Authority checks
  - Role-based access

#### 8. **Error Handling**
- HTTP exceptions with proper status codes
- Validation via Pydantic schemas
- Async exception handling
- Logging at each layer

---

## 7. Configuration Management

### Environment Variables
Located in `.env.example`:

**Environment**:
- ENVIRONMENT (development/production)
- DEBUG (true/false)

**Security**:
- SECRET_KEY (JWT signing key)
- JWT_ALGORITHM (HS256)
- ACCESS_TOKEN_EXPIRE_MINUTES
- REFRESH_TOKEN_EXPIRE_DAYS

**Database - PostgreSQL**:
- POSTGRES_SERVER
- POSTGRES_PORT
- POSTGRES_USER
- POSTGRES_PASSWORD
- POSTGRES_DB

**Cache - Redis**:
- REDIS_HOST
- REDIS_PORT
- REDIS_DB
- REDIS_PASSWORD

**Message Queue - Kafka**:
- KAFKA_ENABLED
- KAFKA_BOOTSTRAP_SERVERS
- KAFKA_TOPIC_PREFIX

**Time-Series - Cassandra**:
- CASSANDRA_ENABLED
- CASSANDRA_HOSTS
- CASSANDRA_PORT
- CASSANDRA_KEYSPACE

**IoT Protocols**:
- MQTT_ENABLED, MQTT_BIND_PORT, MQTT_SSL_ENABLED
- COAP_ENABLED, COAP_BIND_PORT
- HTTP_TRANSPORT_ENABLED, HTTP_BIND_PORT
- LWM2M_ENABLED, LWM2M_BIND_PORT

**Telemetry**:
- TELEMETRY_MAX_STRING_VALUE_LENGTH
- TELEMETRY_TTL_DAYS

**Rate Limiting**:
- RATE_LIMIT_ENABLED
- DEFAULT_TENANT_DEVICE_LIMIT
- DEFAULT_TENANT_ASSET_LIMIT

---

## 8. API Response Patterns

### Standard Response Format
```json
{
  "id": "uuid",
  "created_time": 1234567890000,
  "name": "entity_name",
  "tenant_id": "uuid",
  "customer_id": "uuid" (optional),
  "additional_info": {}
}
```

### Error Responses
```json
{
  "detail": "Error message",
  "status_code": 400
}
```

### Paginated Responses
- Query parameters: `limit` (default 100), `offset` (default 0)
- Returns list of entities

### Telemetry Response Format
```json
{
  "data": {
    "key1": [{"ts": 1234567890000, "value": 25.5}],
    "key2": [{"ts": 1234567890000, "value": "online"}]
  }
}
```

---

## 9. Authentication Flow

### Login Flow
1. `POST /api/auth/login` with email + password
2. Server validates credentials (bcrypt comparison)
3. Returns:
   ```json
   {
     "token": "access_token_jwt",
     "refresh_token": "refresh_token_jwt"
   }
   ```
4. Client stores tokens
5. Client sends `Authorization: Bearer {token}` with subsequent requests

### Token Refresh Flow
1. `POST /api/auth/token/refresh` with refresh_token
2. Server validates refresh token
3. Returns new access token + refresh token

### Authorization Checks
- `get_current_user`: Validates Bearer token, checks user enabled status
- `require_sys_admin`: Requires SYS_ADMIN authority
- `require_tenant_admin`: Requires SYS_ADMIN or TENANT_ADMIN authority
- Per-endpoint tenant/customer scoping checks

---

## 10. Key Statistics

- **Total Python Files**: 75
- **API Endpoint Lines**: 1,102 lines (endpoints only)
- **Rule Engine Nodes**: 17 implemented node types
- **Supported Models**: 13+ database entities
- **API Endpoints Implemented**: 5 major APIs (Device, Customer, Tenant, Telemetry, Auth)
- **APIs with TODO**: 5 APIs (User, Asset, Alarm, Dashboard, RuleChain)

---

## 11. Migration Status Summary

### ✅ COMPLETED
- Backend infrastructure (FastAPI setup)
- Core models (Tenant, Customer, User, Device, DeviceProfile, etc.)
- Database layer (PostgreSQL, async SQLAlchemy)
- Authentication & JWT system
- Multi-tenancy support
- Device management (CRUD)
- Customer management (CRUD)
- Tenant management (CRUD)
- Telemetry APIs (basic save/retrieve)
- MQTT connector service
- Rule engine core infrastructure (17 nodes)

### 🔄 IN PROGRESS
- WebSocket integration for real-time updates
- Rule engine execution pipeline
- Cassandra integration for historical telemetry
- MQTT transport layer integration

### ⏳ PENDING
- User management APIs
- Asset management (CRUD)
- Alarm management APIs
- Dashboard management
- Rule chain CRUD APIs
- Advanced rule engine features
- Gateway support
- Entity relations
- Audit logs
- Edge support

---

## 12. Technology Decision Notes

### Why FastAPI?
- Native async/await support
- Automatic OpenAPI documentation
- Built-in validation with Pydantic
- High performance (similar to Node.js/Go)
- Easy to develop and maintain
- Great for IoT use cases

### Why PostgreSQL as Primary?
- Relational data integrity
- Strong ACID guarantees
- JSON/JSONB support for flexible data
- Full-text search capability
- Excellent async support with asyncpg

### Why Cassandra Optional?
- Time-series optimized
- Horizontal scalability
- Handles high write throughput
- Perfect for IoT telemetry data

### Why Kafka?
- Event streaming architecture
- Decoupling of services
- Scalable message distribution
- Perfect for rule engine input/output

### Why Redis?
- Fast caching layer
- Distributed locks
- Session storage
- Rate limiting support


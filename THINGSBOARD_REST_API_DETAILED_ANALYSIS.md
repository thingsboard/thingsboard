# ThingsBoard Java Backend REST API - Comprehensive Analysis

## Executive Summary

ThingsBoard uses a robust REST API architecture built on Spring Boot with Spring Security (JWT-based authentication). The API follows a hierarchical pattern with tenant isolation, customer management, and role-based access control (RBAC). All major endpoints are documented using OpenAPI 3.0 (Swagger).

---

## 1. KEY CONTROLLERS - ENDPOINT SIGNATURES

### 1.1 DeviceController (`/api`)

#### Core Device Operations

```
GET /api/device/{deviceId}
├── Path Params: deviceId (UUID string)
├── Security: @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
├── Response: Device (JSON)
└── Notes: Checks device ownership by tenant/customer

GET /api/device/info/{deviceId}
├── Response: DeviceInfo (includes extra metadata)
└── Security: Same as above

POST /api/device
├── Request Body: Device JSON
├── Query Params:
│   ├── accessToken (optional): Pre-defined token
│   ├── nameConflictPolicy: FAIL (default), UPDATE, OR CREATE_NEW_UNIQUE
│   ├── uniquifySeparator: "_" (default)
│   └── uniquifyStrategy: RANDOM (default), SEQUENTIAL
├── Security: @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
├── Response: Device (with generated ID)
└── Notes: Device name must be unique per tenant

DELETE /api/device/{deviceId}
├── Security: @PreAuthorize("hasAuthority('TENANT_ADMIN')")
└── Response: 200 OK

POST /api/customer/{customerId}/device/{deviceId}
├── Purpose: Assign device to customer
├── Security: @PreAuthorize("hasAuthority('TENANT_ADMIN')")
└── Response: Device

DELETE /api/customer/device/{deviceId}
├── Purpose: Unassign from customer
├── Security: @PreAuthorize("hasAuthority('TENANT_ADMIN')")
└── Response: Device

POST /api/customer/public/device/{deviceId}
├── Purpose: Make device publicly available
└── Security: @PreAuthorize("hasAuthority('TENANT_ADMIN')")

GET /api/device/{deviceId}/credentials
├── Security: @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
└── Response: DeviceCredentials

POST /api/device/credentials
├── Request Body: DeviceCredentials JSON
├── Security: @PreAuthorize("hasAuthority('TENANT_ADMIN')")
└── Response: DeviceCredentials

GET /api/tenant/devices?pageSize=10&page=0&type=&textSearch=&sortProperty=&sortOrder=
├── Query Params:
│   ├── pageSize (required): Items per page
│   ├── page (required): Page number (0-indexed)
│   ├── type (optional): Device type filter
│   ├── textSearch (optional): Substring search
│   ├── sortProperty (optional): createdTime|name|deviceProfileName|label|customerTitle
│   └── sortOrder (optional): ASC|DESC
├── Security: @PreAuthorize("hasAuthority('TENANT_ADMIN')")
└── Response: PageData<Device>

GET /api/customer/{customerId}/devices?pageSize=10&page=0
├── Similar params as above
├── Security: @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
└── Response: PageData<Device>

GET /api/devices?deviceIds=id1,id2,id3
├── Purpose: Get multiple devices by IDs
├── Security: @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
└── Response: List<Device>

POST /api/devices
├── Purpose: Find devices by complex query
├── Request Body: DeviceSearchQuery
├── Security: @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
└── Response: List<Device>

POST /api/customer/device/{deviceName}/claim
├── Purpose: Claim device for customer
├── Path Param: deviceName (device identifier)
├── Request Body: ClaimRequest (optional, contains secretKey)
├── Security: @PreAuthorize("hasAuthority('CUSTOMER_USER')")
└── Response: DeferredResult<ResponseEntity> (async)

DELETE /api/customer/device/{deviceName}/claim
├── Purpose: Reclaim device (unassign from customer)
├── Security: @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
└── Response: DeferredResult<ResponseEntity> (async)

POST /api/device/bulk_import
├── Purpose: Bulk import devices from CSV
├── Request Body: BulkImportRequest
├── Security: @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
└── Response: BulkImportResult<Device>
```

#### Device DTO Structure

```java
class Device {
    DeviceId id;
    TenantId tenantId;
    CustomerId customerId;        // nullable
    String name;                  // unique per tenant
    String type;                  // e.g., "default", custom types
    String label;                 // user-friendly label
    DeviceProfileId deviceProfileId;
    JsonNode additionalInfo;      // flexible metadata
    OtaPackageId firmwareId;
    OtaPackageId softwareId;
    DeviceId externalId;
    Long version;
    Long createdTime;
    Long updatedTime;
}

class DeviceInfo extends Device {
    String deviceProfileName;
    String customerTitle;
    Boolean active;
}

class DeviceCredentials {
    DeviceCredentialsId id;
    DeviceId deviceId;
    DeviceCredentialsType credentialsType;  // ACCESS_TOKEN, X509, MQTT_BASIC, LWM2M
    String credentialsId;
    String credentialsValue;
}

enum DeviceCredentialsType {
    ACCESS_TOKEN,
    X509,
    MQTT_BASIC,
    LWM2M_CREDENTIALS
}
```

---

### 1.2 AssetController (`/api`)

#### Core Asset Operations

```
GET /api/asset/{assetId}
├── Path Params: assetId (UUID string)
├── Security: @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
└── Response: Asset

POST /api/asset
├── Request Body: Asset JSON
├── Query Params:
│   ├── nameConflictPolicy: FAIL (default)
│   ├── uniquifySeparator: "_"
│   └── uniquifyStrategy: RANDOM
├── Security: @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
└── Response: Asset

DELETE /api/asset/{assetId}
├── Security: @PreAuthorize("hasAuthority('TENANT_ADMIN')")
└── Response: 200 OK

POST /api/customer/{customerId}/asset/{assetId}
├── Purpose: Assign asset to customer
├── Security: @PreAuthorize("hasAuthority('TENANT_ADMIN')")
└── Response: Asset

GET /api/tenant/assets?pageSize=10&page=0&type=&textSearch=&sortProperty=&sortOrder=
├── Query Params: pageSize|page|type|textSearch|sortProperty|sortOrder
├── Security: @PreAuthorize("hasAuthority('TENANT_ADMIN')")
└── Response: PageData<Asset>

GET /api/customer/{customerId}/assets?pageSize=10&page=0
├── Security: @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
└── Response: PageData<Asset>

GET /api/assets?assetIds=id1,id2
├── Purpose: Get multiple assets by IDs
├── Security: @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
└── Response: List<Asset>

POST /api/assets
├── Purpose: Find assets by complex query
├── Request Body: AssetSearchQuery
└── Response: List<Asset>

POST /api/asset/bulk_import
├── Purpose: Bulk import from CSV
├── Request Body: BulkImportRequest
└── Response: BulkImportResult<Asset>
```

#### Asset DTO Structure

```java
class Asset {
    AssetId id;
    TenantId tenantId;
    CustomerId customerId;
    String name;                  // unique per tenant
    String type;                  // asset type
    String label;
    AssetProfileId assetProfileId;
    JsonNode additionalInfo;
    AssetId externalId;
    Long version;
    Long createdTime;
    Long updatedTime;
}

class AssetInfo extends Asset {
    String assetProfileName;
    String customerTitle;
}
```

---

### 1.3 CustomerController (`/api`)

```
GET /api/customer/{customerId}
├── Path Params: customerId (UUID string)
├── Security: @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
└── Response: Customer

POST /api/customer
├── Request Body: Customer JSON
├── Security: @PreAuthorize("hasAuthority('TENANT_ADMIN')")
└── Response: Customer

DELETE /api/customer/{customerId}
├── Security: @PreAuthorize("hasAuthority('TENANT_ADMIN')")
└── Response: 200 OK (deletes all customer users, unassigns entities)

GET /api/customers?pageSize=10&page=0&textSearch=&sortProperty=&sortOrder=
├── Query Params: pageSize|page|textSearch|sortProperty|sortOrder
├── Security: @PreAuthorize("hasAuthority('TENANT_ADMIN')")
└── Response: PageData<Customer>

GET /api/customer/{customerId}/shortInfo
├── Purpose: Get minimal customer info (title + isPublic flag)
└── Response: JsonNode { "title": "...", "isPublic": boolean }

GET /api/customer/{customerId}/title
├── Purpose: Get customer title only
├── Produces: application/text
└── Response: String (plain text)

GET /api/tenant/customers?customerTitle=TitleValue
├── Purpose: Get customer by title
├── Query Param: customerTitle
├── Security: @PreAuthorize("hasAuthority('TENANT_ADMIN')")
└── Response: Customer
```

#### Customer DTO Structure

```java
class Customer {
    CustomerId id;
    TenantId tenantId;
    String title;                 // unique per tenant
    String email;
    String phone;
    String country;
    String city;
    String address;
    String address2;
    String zip;
    String state;
    boolean isPublic;
    JsonNode additionalInfo;      // can contain homeDashboard settings
    Long createdTime;
    Long updatedTime;
}
```

---

### 1.4 UserController (`/api`)

```
GET /api/user/{userId}
├── Path Params: userId (UUID string)
├── Security: @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
└── Response: User

POST /api/user
├── Request Body: User JSON
├── Query Param: sendActivationMail (boolean, default: true)
├── Security: @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
└── Response: User

DELETE /api/user/{userId}
├── Security: @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
└── Response: 200 OK (Cannot delete self, must maintain >= 1 TENANT_ADMIN)

GET /api/users?pageSize=10&page=0&textSearch=&sortProperty=&sortOrder=
├── Query Params: pageSize|page|textSearch|sortProperty|sortOrder
├── Security: @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
├── Notes: TENANT_ADMIN sees tenant users; CUSTOMER_USER sees customer users
└── Response: PageData<User>

GET /api/tenant/{tenantId}/users?pageSize=10&page=0
├── Purpose: Get tenant admins
├── Security: @PreAuthorize("hasAuthority('SYS_ADMIN')")
└── Response: PageData<User>

GET /api/customer/{customerId}/users?pageSize=10&page=0
├── Purpose: Get customer users
├── Security: @PreAuthorize("hasAuthority('TENANT_ADMIN')")
└── Response: PageData<User>

POST /api/user/{userId}/userCredentialsEnabled
├── Purpose: Enable/disable user login
├── Query Param: userCredentialsEnabled (boolean, default: true)
├── Security: @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
└── Response: 200 OK

GET /api/user/{userId}/token
├── Purpose: Get JWT token for impersonation
├── Security: @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
└── Response: JwtPair { accessToken, refreshToken }

POST /api/user/sendActivationMail
├── Purpose: Resend activation email
├── Query Param: email
├── Security: @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
└── Response: 200 OK

GET /api/user/{userId}/activationLink
├── Purpose: Get activation link for user
├── Produces: text/plain
├── Security: @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
└── Response: String (URL)

POST /api/user/settings
├── Purpose: Save user settings (general)
├── Request Body: JsonNode (arbitrary JSON)
├── Security: @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
└── Response: JsonNode (saved settings)

GET /api/user/settings
├── Purpose: Retrieve user settings
└── Response: JsonNode

PUT /api/user/settings/{type}
├── Purpose: Update specific settings type
├── Path Param: type (general|quick_links|doc_links|dashboards)
├── Request Body: JsonNode (only specified fields updated)
└── Response: 200 OK

DELETE /api/user/settings/{paths}
├── Purpose: Delete specific settings by JSON paths
├── Path Param: paths (comma-separated, e.g., "A.B,C")
└── Response: 200 OK

GET /api/user/dashboards
├── Purpose: Get last visited and starred dashboards
├── Security: @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
└── Response: UserDashboardsInfo

GET /api/user/dashboards/{dashboardId}/{action}
├── Purpose: Report dashboard action
├── Path Params: dashboardId, action (visit|star|unstar)
└── Response: UserDashboardsInfo
```

#### User DTO Structure

```java
class User {
    UserId id;
    TenantId tenantId;
    CustomerId customerId;        // nullable
    String email;                 // unique across platform
    String firstName;
    String lastName;
    Authority authority;          // SYS_ADMIN|TENANT_ADMIN|CUSTOMER_USER
    boolean enabled;
    Long createdTime;
    Long updatedTime;
}

enum Authority {
    SYS_ADMIN,        // System administrator
    TENANT_ADMIN,     // Tenant administrator
    CUSTOMER_USER     // Customer user
}
```

---

### 1.5 AlarmController (`/api`)

```
GET /api/alarm/{alarmId}
├── Path Params: alarmId (UUID string)
├── Security: @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
└── Response: Alarm

POST /api/alarm
├── Request Body: Alarm JSON
├── Security: @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
├── Notes: Platform deduplicates alarms by originator + type
└── Response: Alarm

DELETE /api/alarm/{alarmId}
├── Purpose: Delete alarm
├── Security: @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
└── Response: boolean (success)

POST /api/alarm/{alarmId}/ack
├── Purpose: Acknowledge alarm (sets ack_ts)
├── Security: @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
└── Response: AlarmInfo

POST /api/alarm/{alarmId}/clear
├── Purpose: Clear alarm (sets clear_ts)
├── Security: @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
└── Response: AlarmInfo

POST /api/alarm/{alarmId}/assign/{assigneeId}
├── Purpose: Assign alarm to user
├── Path Params: alarmId, assigneeId (userId)
├── Security: @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
└── Response: Alarm

DELETE /api/alarm/{alarmId}/assign
├── Purpose: Unassign alarm
├── Security: @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
└── Response: Alarm

GET /api/alarm/{entityType}/{entityId}?searchStatus=&status=&pageSize=10&page=0
├── Query Params:
│   ├── searchStatus (optional): ANY|ACTIVE|CLEARED|ACK|UNACK
│   ├── status (optional): ACTIVE_UNACK|ACTIVE_ACK|CLEARED_UNACK|CLEARED_ACK
│   ├── assigneeId (optional): filter by assignee
│   ├── pageSize, page, textSearch, sortProperty, sortOrder
│   ├── startTime, endTime (optional): timestamp range
│   └── fetchOriginator (optional): include originator name
├── Security: @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
└── Response: PageData<AlarmInfo>

GET /api/alarms?searchStatus=&pageSize=10&page=0
├── Purpose: Get all tenant/customer alarms
├── Query Params: same as above
├── Security: @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
└── Response: PageData<AlarmInfo>

GET /api/v2/alarm/{entityType}/{entityId}?statusList=&severityList=&typeList=
├── Purpose: Alternative V2 API with arrays
├── Query Params:
│   ├── statusList (array): ANY|ACTIVE|CLEARED|ACK|UNACK
│   ├── severityList (array): CRITICAL|MAJOR|MINOR|WARNING|INDETERMINATE
│   ├── typeList (array): alarm type strings
│   └── (other params same as V1)
├── Security: @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
└── Response: PageData<AlarmInfo>

GET /api/alarm/highestSeverity/{entityType}/{entityId}
├── Purpose: Get highest alarm severity for entity
├── Query Params: searchStatus|status|assigneeId
└── Response: AlarmSeverity (CRITICAL|MAJOR|MINOR|WARNING|INDETERMINATE)

GET /api/alarm/types?pageSize=10&page=0&textSearch=&sortOrder=
├── Purpose: Get all alarm types used by tenant/customer
└── Response: PageData<EntitySubtype>
```

#### Alarm DTO Structure

```java
class Alarm {
    AlarmId id;
    TenantId tenantId;
    CustomerId customerId;        // nullable
    EntityId originator;          // which entity caused alarm (Device, Asset, etc.)
    String type;                  // unique per originator
    AlarmSeverity severity;       // CRITICAL|MAJOR|MINOR|WARNING|INDETERMINATE
    AlarmStatus status;           // ACTIVE_UNACK|ACTIVE_ACK|CLEARED_UNACK|CLEARED_ACK
    Long startTs;                 // when alarm started
    Long endTs;                   // nullable, set when alarm cleared
    Long ackTs;                   // nullable, set when acknowledged
    Long assignTs;                // nullable, set when assigned
    UserId assigneeId;            // nullable, user assigned to alarm
    JsonNode details;             // alarm-specific details
    Long createdTime;
    Long updatedTime;
}

class AlarmInfo extends Alarm {
    String originatorName;        // resolved from originator EntityId
}

enum AlarmSeverity {
    CRITICAL, MAJOR, MINOR, WARNING, INDETERMINATE
}

enum AlarmStatus {
    ACTIVE_UNACK,
    ACTIVE_ACK,
    CLEARED_UNACK,
    CLEARED_ACK
}

enum AlarmSearchStatus {
    ANY,      // any status
    ACTIVE,   // ACTIVE_UNACK or ACTIVE_ACK
    CLEARED,  // CLEARED_UNACK or CLEARED_ACK
    ACK,      // ACTIVE_ACK or CLEARED_ACK
    UNACK     // ACTIVE_UNACK or CLEARED_UNACK
}
```

---

### 1.6 TelemetryController (`/api/telemetry`)

#### Attributes Operations

```
GET /api/telemetry/{entityType}/{entityId}/keys/attributes
├── Purpose: Get all attribute keys
├── Path Params: entityType (DEVICE|ASSET|etc.), entityId
├── Security: @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
└── Response: DeferredResult<ResponseEntity> (async, Set<String>)

GET /api/telemetry/{entityType}/{entityId}/keys/attributes/{scope}
├── Purpose: Get attribute keys by scope
├── Path Params: entityType, entityId, scope (SERVER_SCOPE|SHARED_SCOPE|CLIENT_SCOPE)
└── Response: DeferredResult<ResponseEntity>

GET /api/telemetry/{entityType}/{entityId}/values/attributes
├── Purpose: Get all attributes (all scopes)
├── Query Params: keys (optional, comma-separated)
├── Response: DeferredResult<ResponseEntity> (Map<String, List<AttributeKvEntry>>)
│           { "CLIENT_SCOPE": [...], "SERVER_SCOPE": [...], "SHARED_SCOPE": [...] }

GET /api/telemetry/{entityType}/{entityId}/values/attributes/{scope}
├── Purpose: Get attributes of specific scope
├── Query Params: keys (optional)
├── Response: DeferredResult<ResponseEntity> (List<AttributeKvEntry>)

POST /api/telemetry/{deviceId}/{scope}
├── Purpose: Save device attributes
├── Path Params: deviceId, scope (SERVER_SCOPE|SHARED_SCOPE)
├── Request Body: JSON key-value pairs, e.g., {"temperature": 25.5, "status": "online"}
├── Security: @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
└── Response: DeferredResult<ResponseEntity> (200 OK)

POST /api/telemetry/{entityType}/{entityId}/{scope}
├── Purpose: Save entity attributes (generic)
├── Path Params: entityType, entityId, scope
├── Request Body: JSON object
└── Response: DeferredResult<ResponseEntity>

POST /api/telemetry/{entityType}/{entityId}/attributes/{scope}
├── Purpose: Alternative V2 API path
├── Request Body: JSON object
└── Response: DeferredResult<ResponseEntity>
```

#### Timeseries Operations

```
GET /api/telemetry/{entityType}/{entityId}/keys/timeseries
├── Purpose: Get all timeseries keys
├── Response: DeferredResult<ResponseEntity> (Set<String>)

GET /api/telemetry/{entityType}/{entityId}/values/timeseries
├── Purpose: Get latest timeseries values
├── Query Params:
│   ├── keys (optional): comma-separated key names
│   └── useStrictDataTypes (default: false)
├── Response: DeferredResult<ResponseEntity>
│   { "temperature": [{"ts": 1234567890, "value": "25.5"}],
│     "humidity": [{"ts": 1234567890, "value": "65"}] }

GET /api/telemetry/{entityType}/{entityId}/values/timeseries?keys=&startTs=&endTs=
├── Purpose: Get timeseries data for range
├── Query Params:
│   ├── keys (required): comma-separated
│   ├── startTs (required): milliseconds
│   ├── endTs (required): milliseconds
│   ├── intervalType (optional): MILLISECONDS|WEEK|WEEK_ISO|MONTH|QUARTER
│   ├── interval (default: 0): aggregation interval in ms
│   ├── timeZone (optional): timezone for WEEK/MONTH/QUARTER
│   ├── limit (default: 100): max data points
│   ├── agg (default: NONE): MIN|MAX|AVG|SUM|COUNT|NONE
│   ├── orderBy (default: DESC): ASC|DESC
│   └── useStrictDataTypes (default: false)
├── Response: DeferredResult<ResponseEntity>
│   { "temperature": [
│       {"ts": 1234567890, "value": 25.5},
│       {"ts": 1234567900, "value": 26.0}
│     ]
│   }
```

#### Telemetry Request/Response DTOs

```java
class AttributeKvEntry {
    String key;
    long ts;              // timestamp (for server attributes)
    DataType dataType;    // STRING, LONG, DOUBLE, BOOLEAN
    Object value;
}

class TsKvEntry {
    String key;
    long ts;              // timestamp (milliseconds)
    DataType dataType;
    Object value;
}

enum DataType {
    STRING,
    LONG,
    DOUBLE,
    BOOLEAN
}

enum Aggregation {
    MIN, MAX, AVG, SUM, COUNT, NONE
}

// Response example for attributes:
{
  "SERVER_SCOPE": [
    {"key": "serial", "value": "ABC123"},
    {"key": "hwVersion", "value": "2.1"}
  ],
  "SHARED_SCOPE": [
    {"key": "model", "value": "TB-001"}
  ],
  "CLIENT_SCOPE": [
    {"key": "lastActivityTime", "value": "1234567890"}
  ]
}

// Response example for timeseries:
{
  "temperature": [
    {"ts": 1234567890, "value": "25.5"},
    {"ts": 1234567900, "value": "26.0"}
  ],
  "humidity": [
    {"ts": 1234567890, "value": "65.2"}
  ]
}
```

---

## 2. API PATTERNS

### 2.1 URL Structure

```
Base Path: /api
Version: Implicit (no /v1/ prefix for REST, WebSocket uses /api/ws)

Entity CRUD Pattern:
  GET    /api/{entity}/{id}                   - Read single
  POST   /api/{entity}                        - Create/Update
  DELETE /api/{entity}/{id}                   - Delete
  GET    /api/{parentEntity}/{parentId}/{entity} - Get by parent

Pagination Pattern:
  GET /api/{entity}?pageSize=10&page=0
      &textSearch=query
      &sortProperty=name&sortOrder=ASC
      &startTime=&endTime=
```

### 2.2 ID Handling

All IDs are **UUID strings** (RFC 4122), not integers.

```
Path Parameters:
  - Passed as string representation of UUID in path
  - Converted to UUID objects internally
  - Examples:
    GET /api/device/784f394c-42b6-435a-983c-b7beff2784f9
    GET /api/customer/550e8400-e29b-41d4-a716-446655440000

Query Parameters:
  - Comma-separated arrays of UUIDs for batch operations
  - Example:
    GET /api/devices?deviceIds=id1,id2,id3

Internal Type Wrappers:
  - DeviceId, AssetId, CustomerId, UserId, AlarmId
  - Extend UUIDBased interface
  - Constructor: new DeviceId(UUID.fromString(strDeviceId))
```

### 2.3 Pagination Pattern (PageLink)

```
Request:
  GET /api/tenant/devices?pageSize=50&page=0
      &textSearch=temperature
      &sortProperty=name
      &sortOrder=ASC

Query Parameters:
  - pageSize (int, required): items per page (default: varies by endpoint)
  - page (int, required): 0-indexed page number
  - textSearch (String, optional): substring search on specific fields
  - sortProperty (String, optional): field to sort by
  - sortOrder (String, optional): ASC or DESC
  - startTime (Long, optional): filter by createdTime >= startTime (for TimePageLink)
  - endTime (Long, optional): filter by createdTime <= endTime (for TimePageLink)

Response: PageData<T>
{
  "data": [
    { /* entity objects */ },
    ...
  ],
  "totalPages": 5,
  "totalElements": 234,
  "hasNext": true
}

Sorting Examples:
  GET /api/tenant/devices?pageSize=10&page=0&sortProperty=name&sortOrder=ASC
  GET /api/customers?pageSize=20&page=1&sortProperty=createdTime&sortOrder=DESC

Default Sort Property:
  - If no sortProperty specified: uses "id"
  - Some endpoints: createdTime is common default
```

### 2.4 Request/Response DTO Patterns

#### Base Entity Pattern

```java
// Base class for all entities
class BaseData<I extends UUIDBased> implements Cloneable {
    I id;
    Long createdTime;
    Long updatedTime;
}

// With additional info (flexible JSON)
class BaseDataWithAdditionalInfo<I extends UUIDBased> extends BaseData<I> {
    JsonNode additionalInfo;
}

// With timestamps
class HasCreatedTime {
    Long createdTime;  // milliseconds since epoch
}

class HasUpdatedTime {
    Long updatedTime;  // milliseconds since epoch
}
```

#### Common DTO Suffixes

- `Entity` (e.g., `Device`): Full data object
- `Info` (e.g., `DeviceInfo`): Entity + additional resolved fields (e.g., profileName)
- `Query` (e.g., `DeviceSearchQuery`): Complex search parameters
- `Request` (e.g., `ClaimRequest`): API request payload
- `Result` (e.g., `ClaimResult`): API response payload
- `Credentials` (e.g., `DeviceCredentials`): Authentication data

---

## 3. FILTERS AND SEARCHES

### 3.1 Text Search

```
Implementation: Case-insensitive substring matching
Applied fields vary by entity:

Devices:
  - name, label, type, deviceProfileName, customerTitle

Assets:
  - name, label, type, assetProfileName, customerTitle

Customers:
  - title, email, city, country

Users:
  - firstName, lastName, email

Alarms:
  - type, severity, status

Query Pattern:
  GET /api/tenant/devices?pageSize=10&page=0&textSearch=temperature
```

### 3.2 Type Filtering

```
Devices:
  GET /api/tenant/devices?pageSize=10&page=0&type=default
  GET /api/tenant/devices?pageSize=10&page=0&deviceProfileId={profileId}

Assets:
  GET /api/tenant/assets?pageSize=10&page=0&type=building
  GET /api/tenant/assets?pageSize=10&page=0&assetProfileId={profileId}

Alarms V1:
  Query Params: searchStatus, status
  GET /api/alarm/DEVICE/{deviceId}?searchStatus=ACTIVE&pageSize=10&page=0

Alarms V2:
  Array-based filtering
  GET /api/v2/alarm/DEVICE/{deviceId}?statusList=ACTIVE,CLEARED&severityList=CRITICAL,MAJOR
```

### 3.3 Complex Queries

```
DeviceSearchQuery (POST /api/devices):
{
  "parameters": {
    "entityId": "...",
    "direction": "FROM|TO",
    "maxLevel": 2
  },
  "deviceTypes": ["default", "custom"],
  "pageLink": {
    "pageSize": 10,
    "page": 0,
    "textSearch": null,
    "sortOrder": null
  }
}

AssetSearchQuery (POST /api/assets):
{
  "parameters": {
    "entityId": "...",
    "relation": "CONTAINS|MANAGES",
    "maxLevel": 1
  },
  "assetTypes": ["building", "room"],
  "pageLink": { ... }
}
```

---

## 4. AUTHENTICATION & SECURITY

### 4.1 JWT Implementation

#### Token Generation (JwtTokenFactory)

```
Entry Point: JwtAuthenticationProvider.authenticate(String token)
  1. Validates token signature
  2. Parses claims (user ID, tenant, authority, etc.)
  3. Checks token expiration
  4. Returns SecurityUser object

SecurityUser Structure:
{
  UserId id,
  TenantId tenantId,
  CustomerId customerId,
  Authority authority,
  UserPrincipal principal,      // UserName or PublicId based
  boolean isEnabled,
  Long expiresTime               // token expiration
}

Token Claims:
{
  "sub": "userId",
  "tenantId": "tenantId",
  "customerId": "customerId",  // nullable
  "authority": "TENANT_ADMIN|CUSTOMER_USER|SYS_ADMIN",
  "type": "ACCESS|REFRESH",
  "iat": 1234567890,
  "exp": 1234571490,
  "principalType": "USER_NAME|PUBLIC_ID",
  "principalValue": "email@domain.com|publicId"
}

JwtPair Response:
{
  "accessToken": "eyJhbGc...",    // expires in ~15 mins
  "refreshToken": "eyJhbGc..."    // expires in ~24 hours
}
```

#### Authentication Flow

```
1. Login Request:
   POST /api/auth/login
   Body: {"username": "user@example.com", "password": "password"}
   Response: JwtPair (accessToken, refreshToken)

2. API Request with Token:
   GET /api/device/{deviceId}
   Headers: {"Authorization": "Bearer <accessToken>"}

3. Token Validation:
   - Signature verified using server's secret key
   - Claims parsed: user ID, tenant, authority, etc.
   - Token expiration checked
   - Token outdating checked (TokenOutdatingService)

4. Refresh Flow:
   POST /api/auth/refresh
   Body: {"refreshToken": "..."}
   Response: JwtPair (new tokens)

5. Logout:
   POST /api/auth/logout
   - Invalidates current tokens
   - Broadcasts UserCredentialsInvalidationEvent
```

### 4.2 Authorization (Role-Based Access Control)

#### Security Annotations

```java
@PreAuthorize("hasAuthority('TENANT_ADMIN')")
  - Only tenant administrators

@PreAuthorize("hasAuthority('CUSTOMER_USER')")
  - Only customer users

@PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
  - Tenant OR customer users

@PreAuthorize("hasAuthority('SYS_ADMIN')")
  - System administrators only

@PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
  - System or tenant administrators
```

#### Permission Checks (Operation + Resource)

```java
// Pattern used internally
accessControlService.checkPermission(user, Resource.DEVICE, Operation.READ, deviceId, device)

enum Resource {
  DEVICE, ASSET, CUSTOMER, USER, DASHBOARD, TENANT, ATTRIBUTE, TELEMETRY,
  ALARM, EVENT, RELATION, EDGE, RULE_CHAIN, RULE_NODE, etc.
}

enum Operation {
  READ, WRITE, DELETE, CREATE, READ_ATTRIBUTES, WRITE_ATTRIBUTES,
  READ_TELEMETRY, WRITE_TELEMETRY, CLAIM_DEVICES, etc.
}

// Permission Hierarchy:
// System Admin (SYS_ADMIN)
//   ├─ Can access all tenants
//   ├─ Can access all customers
//   └─ Can manage users with any authority
//
// Tenant Admin (TENANT_ADMIN)
//   ├─ Can access own tenant only
//   ├─ Can access all customers in own tenant
//   ├─ Can manage TENANT_ADMIN and CUSTOMER_USER
//   └─ Can manage all devices/assets in own tenant
//
// Customer User (CUSTOMER_USER)
//   ├─ Can access own customer only
//   ├─ Can access entities assigned to own customer
//   └─ Cannot create/delete entities (read-only mostly)
```

#### Tenant Isolation

```
Pattern: All queries automatically filtered by tenant
  
Example:
  DeviceController.getTenantDevices() {
    TenantId tenantId = getCurrentUser().getTenantId();
    return deviceService.findDevicesByTenantId(tenantId, pageLink);
    // Internally filters: WHERE tenant_id = $tenantId
  }

Customer Isolation:
  For CUSTOMER_USER, additionally filtered by customer:
  DeviceController.getCustomerDevices(CustomerId customerId) {
    // Checks: customerId belongs to current user's customer
    // Then: WHERE tenant_id = $tenantId AND customer_id = $customerId
  }

Cascade Permissions:
  Access to entity → Can access related entities
  Example: Can read device → Can read device's attributes/telemetry
```

### 4.3 Security Exception Handling

```java
@ExceptionHandler
public ResponseEntity handleAccessDeniedException(AccessDeniedException ex) {
  return ResponseEntity.status(HttpStatus.FORBIDDEN)
    .body({
      "timestamp": System.currentTimeMillis(),
      "status": 403,
      "error": "Forbidden",
      "message": "Access denied"
    });
}

@ExceptionHandler
public ResponseEntity handleThingsboardException(ThingsboardException ex) {
  return ResponseEntity.status(ex.getErrorCode().getHttpStatusCode())
    .body({
      "message": ex.getMessage(),
      "errorCode": ex.getErrorCode()
    });
}

Common HTTP Status Codes:
  200 OK - Success
  201 Created - Entity created
  204 No Content - Success with no body
  400 Bad Request - Validation error
  401 Unauthorized - Missing/invalid token
  403 Forbidden - Insufficient permissions
  404 Not Found - Entity doesn't exist
  409 Conflict - Entity already exists (name conflict)
  500 Internal Server Error - Server error
```

---

## 5. WEBSOCKET IMPLEMENTATION

### 5.1 WebSocket Configuration

```
Endpoint: /api/ws
Protocol: WebSocket text frames (JSON messages)
Max Buffer Size: 32KB text, 32KB binary
CORS: Allows all origins

Registry:
  registry.addHandler(wsHandler, "/api/ws/**")
    .setAllowedOriginPatterns("*")
```

### 5.2 Message Types

#### Session Lifecycle

```
1. Connection Establishment:
   WebSocket /api/ws establishes TCP connection
   
2. Authentication (First Message):
   Client → Server: AuthCmd (within WsCommandsWrapper)
   {
     "authCmd": {
       "token": "<JWT access token>"
     }
   }
   
3. Session Established:
   Server → Client: SessionEvent.onEstablished()
   
4. Commands (after auth):
   Client → Server: TelemetryCmdsWrapper or NotificationCmdsWrapper
   {
     "attrSubCmd": [...],          // attribute subscription
     "tsSubCmd": [...],            // timeseries subscription
     "historicalDataCmd": [...],   // historical data fetch
     "rpcCmd": [...]               // RPC commands
   }
   
5. Data Messages:
   Server → Client: Subscription data updates (JSON)
   
6. Keep-Alive:
   Client → Server: Ping frame (every 30s)
   Server → Client: Pong frame
   
7. Connection Close:
   Client sends close frame or timeout after 10s of no ping response
   Server sends close frame if auth timeout (10s) or message queue limit exceeded
```

#### WebSocket Message Structure

```java
class WsCommandsWrapper {
  AuthCmd authCmd;                              // Auth command (first message only)
  List<AttributeSubscriptionCmd> attrSubCmd;    // Attribute subscriptions
  List<TimeseriesSubscriptionCmd> tsSubCmd;     // Timeseries subscriptions
  List<HistoricalDataCmd> historicalDataCmd;    // Historical data fetch
  List<RpcCmd> rpcCmd;                          // RPC invocations
  List<Integer> cmdIds;                         // command IDs to acknowledge
}

class AuthCmd {
  String token;                                 // JWT access token
}

class AttributeSubscriptionCmd {
  int cmdId;
  String key;                                   // subscription ID
  String entityType;                            // DEVICE, ASSET, etc.
  String entityId;
  List<String> keys;                            // attribute keys to monitor
  List<AttributeScope> scopes;                  // SERVER_SCOPE, SHARED_SCOPE, CLIENT_SCOPE
}

class TimeseriesSubscriptionCmd {
  int cmdId;
  String key;                                   // subscription ID
  String entityType;
  String entityId;
  List<String> keys;                            // timeseries keys to monitor
  long startTs;                                 // optional start timestamp
  int limit;                                    // data points limit
}

class HistoricalDataCmd {
  int cmdId;
  String entityType;
  String entityId;
  List<String> keys;
  long startTs;
  long endTs;
  Aggregation agg;                              // MIN, MAX, AVG, SUM, COUNT, NONE
  long interval;                                // aggregation interval
  int limit;
  boolean useStrictDataTypes;
}

// Server Response (subscription updates):
{
  "subscriptionId": "...",
  "errorCode": null,
  "data": {
    "temperature": [
      {"ts": 1234567890, "value": "25.5"}
    ],
    "humidity": [
      {"ts": 1234567890, "value": "65"}
    ]
  }
}

// Server Response (error):
{
  "subscriptionId": "...",
  "errorCode": "UNAUTHORIZED|BAD_REQUEST|INTERNAL_ERROR",
  "errorMsg": "Error description"
}
```

### 5.3 Session Management

```java
class WebSocketSessionRef {
  String sessionId;                             // UUID
  WebSocketSessionType sessionType;             // GENERAL|TELEMETRY|NOTIFICATIONS
  SecurityUser securityCtx;                     // null until authenticated
  TenantId tenantId;                            // from securityCtx
  CustomerId customerId;                        // from securityCtx
  UserId userId;                                // from securityCtx
  // ... methods to send messages
}

enum WebSocketSessionType {
  GENERAL,        // Dashboard/UI updates
  TELEMETRY,      // Real-time device telemetry
  NOTIFICATIONS   // Notification updates
}

// Session Lifecycle (TbWebSocketHandler):
class SessionMetaData {
  WebSocketSession session;                     // Spring WebSocket session
  WebSocketSessionRef sessionRef;               // Custom session wrapper
  Queue<TextMessage> msgQueue;                  // Message queue (max 1000)
  AtomicBoolean inProgress;                     // Send in progress flag
  
  // Methods:
  void onMsg(String payload);                   // Process incoming message
  void processPongMessage(long ts);             // Handle pong response
  void send(TextMessage msg);                   // Queue message for send
}

// Session Limits:
  - Max 1000 messages queued per session
  - Auth timeout: 10 seconds
  - Ping timeout: 30 seconds (3 attempts)
  - Send timeout: 5 seconds
  - Rate limiting per tenant (configurable)
```

### 5.4 Message Flow Example

```
Client → Server: Connect to /api/ws

Client → Server (1st message):
{
  "authCmd": {
    "token": "eyJhbGc..."
  }
}

Server → Client:
Session established, ready for commands

Client → Server:
{
  "attrSubCmd": [
    {
      "cmdId": 1,
      "key": "attr-sub-1",
      "entityType": "DEVICE",
      "entityId": "784f394c-42b6-435a-983c-b7beff2784f9",
      "keys": ["temperature", "humidity"],
      "scopes": ["SERVER_SCOPE"]
    }
  ],
  "tsSubCmd": [
    {
      "cmdId": 2,
      "key": "ts-sub-1",
      "entityType": "DEVICE",
      "entityId": "784f394c-42b6-435a-983c-b7beff2784f9",
      "keys": ["temperature"],
      "limit": 100
    }
  ]
}

Server → Client (when attributes change):
{
  "subscriptionId": "attr-sub-1",
  "data": {
    "temperature": [
      {"ts": 1700000000000, "value": "25.5"}
    ]
  }
}

Server → Client (continuous telemetry updates):
{
  "subscriptionId": "ts-sub-1",
  "data": {
    "temperature": [
      {"ts": 1700000000000, "value": "25.5"},
      {"ts": 1700000010000, "value": "25.7"},
      {"ts": 1700000020000, "value": "25.3"}
    ]
  }
}

Client → Server: Keep-alive (ping frame)
Server → Client: Pong frame

Client → Server: Close connection
Server → Client: Close frame
```

---

## 6. REQUEST/RESPONSE EXAMPLES

### 6.1 Device Create/Update

```
POST /api/device
Content-Type: application/json

Request Body:
{
  "name": "Temperature Sensor 01",
  "type": "sensor",
  "label": "Front Door Sensor",
  "deviceProfileId": {
    "entityType": "DEVICE_PROFILE",
    "id": "a1b2c3d4-e5f6-7890-1234-567890abcdef"
  },
  "additionalInfo": {
    "location": "Front Door",
    "building": "Main"
  }
}

Response (201 Created):
{
  "id": {
    "entityType": "DEVICE",
    "id": "784f394c-42b6-435a-983c-b7beff2784f9"
  },
  "tenantId": {
    "entityType": "TENANT",
    "id": "13814000-1dd2-11b2-8080-808080808080"
  },
  "customerId": null,
  "name": "Temperature Sensor 01",
  "type": "sensor",
  "label": "Front Door Sensor",
  "deviceProfileId": {
    "entityType": "DEVICE_PROFILE",
    "id": "a1b2c3d4-e5f6-7890-1234-567890abcdef"
  },
  "createdTime": 1700000000000,
  "updatedTime": 1700000000000,
  "additionalInfo": {
    "location": "Front Door",
    "building": "Main"
  }
}
```

### 6.2 Device Pagination Response

```
GET /api/tenant/devices?pageSize=2&page=0&sortProperty=name&sortOrder=ASC

Response (200 OK):
{
  "data": [
    {
      "id": {"entityType": "DEVICE", "id": "device-id-1"},
      "tenantId": {"entityType": "TENANT", "id": "tenant-id"},
      "customerId": null,
      "name": "Device A",
      "type": "sensor",
      "label": "Sensor A",
      "deviceProfileId": {...},
      "createdTime": 1699999990000,
      "updatedTime": 1699999990000
    },
    {
      "id": {"entityType": "DEVICE", "id": "device-id-2"},
      "tenantId": {"entityType": "TENANT", "id": "tenant-id"},
      "customerId": null,
      "name": "Device B",
      "type": "sensor",
      "label": "Sensor B",
      "deviceProfileId": {...},
      "createdTime": 1699999980000,
      "updatedTime": 1699999980000
    }
  ],
  "totalPages": 5,
  "totalElements": 10,
  "hasNext": true
}
```

### 6.3 Save Attributes

```
POST /api/telemetry/DEVICE/784f394c-42b6-435a-983c-b7beff2784f9/SERVER_SCOPE
Content-Type: application/json

Request Body:
{
  "serial": "SN-20231115-001",
  "hwVersion": "2.1",
  "fwVersion": "1.0.5"
}

Response (200 OK):
(empty body)
```

### 6.4 Get Attributes

```
GET /api/telemetry/DEVICE/784f394c-42b6-435a-983c-b7beff2784f9/values/attributes/SERVER_SCOPE

Response (200 OK):
[
  {
    "key": "serial",
    "value": "SN-20231115-001"
  },
  {
    "key": "hwVersion",
    "value": "2.1"
  },
  {
    "key": "fwVersion",
    "value": "1.0.5"
  }
]
```

### 6.5 Alarm Lifecycle

```
1. Create Alarm:
POST /api/alarm
{
  "originator": {
    "entityType": "DEVICE",
    "id": "784f394c-42b6-435a-983c-b7beff2784f9"
  },
  "type": "HighTemperature",
  "severity": "MAJOR",
  "details": {
    "currentTemp": 45.5,
    "threshold": 40.0
  }
}

Response:
{
  "id": {"entityType": "ALARM", "id": "alarm-id-1"},
  "tenantId": {...},
  "customerId": null,
  "originator": {...},
  "type": "HighTemperature",
  "severity": "MAJOR",
  "status": "ACTIVE_UNACK",
  "startTs": 1700000000000,
  "endTs": null,
  "ackTs": null,
  "assignTs": null,
  "assigneeId": null,
  "details": {...},
  "createdTime": 1700000000000
}

2. Acknowledge:
POST /api/alarm/alarm-id-1/ack
Response: AlarmInfo { status: "ACTIVE_ACK", ackTs: 1700000005000, ... }

3. Assign:
POST /api/alarm/alarm-id-1/assign/user-id-1
Response: Alarm { assigneeId: user-id-1, assignTs: 1700000010000, ... }

4. Clear:
POST /api/alarm/alarm-id-1/clear
Response: AlarmInfo { status: "CLEARED_ACK", endTs: 1700000015000, ... }
```

---

## 7. FILTERING AND QUERYING SUMMARY

### Query Parameter Types

```
Pagination:
  pageSize: int - items per page (required)
  page: int - 0-indexed page number (required)

Search:
  textSearch: String - substring search
  
Sorting:
  sortProperty: String - field to sort by
  sortOrder: String - ASC or DESC

Time Range:
  startTime: Long - milliseconds since epoch
  endTime: Long - milliseconds since epoch

Custom:
  deviceProfileId: UUID - filter by device profile
  assetProfileId: UUID - filter by asset profile
  type: String - device/asset/alarm type
  searchStatus: String - alarm search status
  status: String - alarm status
  severityList: String[] - alarm severities
  assigneeId: UUID - assigned user
  fetchOriginator: boolean - include originator name
```

### Exact Sort Properties by Entity

```
Device:
  createdTime, name, deviceProfileName, label, customerTitle

Asset:
  createdTime, name, type, label, customerTitle

Customer:
  createdTime, title, email, country, city

User:
  createdTime, firstName, lastName, email

Alarm:
  createdTime, startTs, endTs, type, ackTs, clearTs, severity, status
```

---

## CONCLUSION

ThingsBoard's REST API is:
- **Hierarchical**: Tenant → Customer → Device/Asset/User
- **Stateless**: JWT token-based, no session management
- **Async-capable**: Uses DeferredResult for long-running operations
- **Paginated**: Consistent PageLink pattern with sorting/filtering
- **Real-time**: WebSocket support for live telemetry/attribute updates
- **Secure**: Multi-level access control (System → Tenant → Customer)
- **Standards-compliant**: OpenAPI 3.0, JSON request/response

All endpoints are protected by Spring Security with @PreAuthorize annotations enforcing role-based access control.

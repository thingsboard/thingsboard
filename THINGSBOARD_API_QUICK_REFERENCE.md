# ThingsBoard REST API - Quick Reference Guide

## Base Configuration
- **Base URL**: `/api`
- **Authentication**: JWT Bearer Token
- **ID Format**: UUID strings (RFC 4122)
- **Response Format**: JSON
- **WebSocket**: `GET /api/ws` (text frames, JSON messages)

---

## Device Operations Quick Reference

```bash
# Get device
GET /api/device/{deviceId}
Auth: TENANT_ADMIN, CUSTOMER_USER

# Create device
POST /api/device
Body: { "name": "...", "type": "...", "label": "...", "deviceProfileId": {...} }
Auth: TENANT_ADMIN, CUSTOMER_USER

# Update device
POST /api/device
Body: { "id": {...}, "name": "...", ... }

# Delete device
DELETE /api/device/{deviceId}
Auth: TENANT_ADMIN

# Get device credentials
GET /api/device/{deviceId}/credentials
Auth: TENANT_ADMIN, CUSTOMER_USER

# Update device credentials
POST /api/device/credentials
Body: { "deviceId": {...}, "credentialsType": "ACCESS_TOKEN|X509|MQTT_BASIC", ... }

# List tenant devices (paginated)
GET /api/tenant/devices?pageSize=10&page=0&type=&textSearch=&sortProperty=name&sortOrder=ASC

# List customer devices
GET /api/customer/{customerId}/devices?pageSize=10&page=0

# Get multiple devices
GET /api/devices?deviceIds=id1,id2,id3

# Assign to customer
POST /api/customer/{customerId}/device/{deviceId}

# Unassign from customer
DELETE /api/customer/device/{deviceId}

# Make public
POST /api/customer/public/device/{deviceId}
```

---

## Asset Operations Quick Reference

```bash
# Get asset
GET /api/asset/{assetId}

# Create/Update asset
POST /api/asset
Body: { "name": "...", "type": "...", "label": "...", "assetProfileId": {...} }

# Delete asset
DELETE /api/asset/{assetId}

# List tenant assets
GET /api/tenant/assets?pageSize=10&page=0

# List customer assets
GET /api/customer/{customerId}/assets?pageSize=10&page=0

# Get multiple assets
GET /api/assets?assetIds=id1,id2

# Assign to customer
POST /api/customer/{customerId}/asset/{assetId}

# Unassign from customer
DELETE /api/customer/asset/{assetId}
```

---

## Customer Operations Quick Reference

```bash
# Get customer
GET /api/customer/{customerId}

# Create/Update customer
POST /api/customer
Body: { "title": "...", "email": "...", "phone": "...", ... }

# Delete customer
DELETE /api/customer/{customerId}

# List customers
GET /api/customers?pageSize=10&page=0&textSearch=&sortProperty=title&sortOrder=ASC

# Get customer by title
GET /api/tenant/customers?customerTitle=MyCustomer
```

---

## User Operations Quick Reference

```bash
# Get user
GET /api/user/{userId}

# Create user
POST /api/user
Body: { "email": "user@example.com", "firstName": "...", "lastName": "...", "authority": "TENANT_ADMIN" }
Query: sendActivationMail=true

# Update user
POST /api/user
Body: { "id": {...}, "email": "...", ... }

# Delete user
DELETE /api/user/{userId}

# List users
GET /api/users?pageSize=10&page=0

# List tenant admins (System Admin only)
GET /api/tenant/{tenantId}/users?pageSize=10&page=0

# List customer users
GET /api/customer/{customerId}/users?pageSize=10&page=0

# Get user token (impersonation)
GET /api/user/{userId}/token
Response: { "accessToken": "...", "refreshToken": "..." }

# Disable user login
POST /api/user/{userId}/userCredentialsEnabled?userCredentialsEnabled=false

# Save user settings
POST /api/user/settings
Body: { "key1": "value1", "key2": "value2" }

# Get user settings
GET /api/user/settings

# Get dashboard info
GET /api/user/dashboards
```

---

## Alarm Operations Quick Reference

```bash
# Get alarm
GET /api/alarm/{alarmId}

# Create alarm
POST /api/alarm
Body: {
  "originator": { "entityType": "DEVICE", "id": "..." },
  "type": "HighTemperature",
  "severity": "MAJOR",
  "details": { ... }
}

# Delete alarm
DELETE /api/alarm/{alarmId}

# Acknowledge alarm
POST /api/alarm/{alarmId}/ack
Response: AlarmInfo { status: "ACTIVE_ACK", ackTs: <timestamp> }

# Clear alarm
POST /api/alarm/{alarmId}/clear
Response: AlarmInfo { status: "CLEARED_ACK", endTs: <timestamp> }

# Assign alarm to user
POST /api/alarm/{alarmId}/assign/{userId}

# Unassign alarm
DELETE /api/alarm/{alarmId}/assign

# Get alarms for entity (V1)
GET /api/alarm/DEVICE/{deviceId}?searchStatus=ACTIVE&pageSize=10&page=0
searchStatus: ANY|ACTIVE|CLEARED|ACK|UNACK
status: ACTIVE_UNACK|ACTIVE_ACK|CLEARED_UNACK|CLEARED_ACK

# Get all alarms for tenant/customer
GET /api/alarms?pageSize=10&page=0&searchStatus=ACTIVE

# Get alarms for entity (V2 - arrays)
GET /api/v2/alarm/DEVICE/{deviceId}?statusList=ACTIVE,CLEARED&severityList=CRITICAL,MAJOR

# Get all alarms (V2)
GET /api/v2/alarms?pageSize=10&page=0

# Get highest alarm severity
GET /api/alarm/highestSeverity/DEVICE/{deviceId}?searchStatus=ACTIVE
Response: CRITICAL|MAJOR|MINOR|WARNING|INDETERMINATE
```

---

## Telemetry/Attributes Operations Quick Reference

```bash
# Get attribute keys (all scopes)
GET /api/telemetry/DEVICE/{deviceId}/keys/attributes

# Get attribute keys (specific scope)
GET /api/telemetry/DEVICE/{deviceId}/keys/attributes/SERVER_SCOPE
Scopes: SERVER_SCOPE|SHARED_SCOPE|CLIENT_SCOPE

# Get attributes (all scopes)
GET /api/telemetry/DEVICE/{deviceId}/values/attributes
Query: keys=attr1,attr2

# Get attributes (specific scope)
GET /api/telemetry/DEVICE/{deviceId}/values/attributes/SERVER_SCOPE
Query: keys=attr1,attr2

# Save attributes (Device API)
POST /api/telemetry/{deviceId}/SERVER_SCOPE
Body: { "key1": "value1", "key2": "value2" }

# Save attributes (Generic Entity API)
POST /api/telemetry/DEVICE/{deviceId}/attributes/SERVER_SCOPE
Body: { "key1": "value1" }

# Get timeseries keys
GET /api/telemetry/DEVICE/{deviceId}/keys/timeseries

# Get latest timeseries values
GET /api/telemetry/DEVICE/{deviceId}/values/timeseries
Query: keys=temp,humidity&useStrictDataTypes=false

# Get timeseries data range
GET /api/telemetry/DEVICE/{deviceId}/values/timeseries?keys=temp&startTs=1000&endTs=2000
Query params:
  - keys (required)
  - startTs (required): milliseconds
  - endTs (required): milliseconds
  - interval: aggregation interval in ms
  - agg: MIN|MAX|AVG|SUM|COUNT|NONE
  - limit: max data points (default: 100)
  - useStrictDataTypes: boolean (default: false)
```

---

## Pagination Standard Pattern

```
Query Parameters:
  pageSize (int): items per page (required)
  page (int): 0-indexed page number (required)
  textSearch (String): substring search (optional)
  sortProperty (String): field to sort by (optional)
  sortOrder (String): ASC|DESC (optional)
  startTime (Long): milliseconds (optional, for time range)
  endTime (Long): milliseconds (optional, for time range)

Example:
  GET /api/tenant/devices?pageSize=25&page=0&textSearch=sensor&sortProperty=name&sortOrder=ASC

Response:
  {
    "data": [ ... entities ... ],
    "totalPages": 10,
    "totalElements": 234,
    "hasNext": true
  }
```

---

## Authentication Pattern

```bash
# Login
POST /api/auth/login
Body: { "username": "user@example.com", "password": "password" }
Response: { "token": "...", "refreshToken": "..." }

# API Request with Token
GET /api/device/{deviceId}
Headers: { "Authorization": "Bearer <token>" }

# Refresh Token
POST /api/auth/refresh
Body: { "refreshToken": "..." }
Response: { "token": "...", "refreshToken": "..." }

# Logout
POST /api/auth/logout
```

---

## Security Annotations Quick Reference

```java
@PreAuthorize("hasAuthority('TENANT_ADMIN')")
  // Only Tenant Administrators

@PreAuthorize("hasAuthority('CUSTOMER_USER')")
  // Only Customer Users

@PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
  // Tenant Admins OR Customer Users

@PreAuthorize("hasAuthority('SYS_ADMIN')")
  // Only System Administrators

@PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
  // System OR Tenant Admins
```

---

## Authority Hierarchy

```
System Admin (SYS_ADMIN)
  ├─ Access all tenants
  ├─ Access all customers
  └─ Manage all users

Tenant Admin (TENANT_ADMIN)
  ├─ Access own tenant only
  ├─ Access all customers in tenant
  ├─ Manage TENANT_ADMIN and CUSTOMER_USER roles
  └─ Create/Delete devices, assets in own tenant

Customer User (CUSTOMER_USER)
  ├─ Access own customer only
  ├─ Access entities assigned to customer (read mostly)
  └─ Cannot create/delete entities
```

---

## Common Query Parameters by Entity

### Device Sort Properties
`createdTime|name|deviceProfileName|label|customerTitle`

### Asset Sort Properties
`createdTime|name|type|label|customerTitle`

### Customer Sort Properties
`createdTime|title|email|country|city`

### User Sort Properties
`createdTime|firstName|lastName|email`

### Alarm Sort Properties
`createdTime|startTs|endTs|type|ackTs|clearTs|severity|status`

---

## Device Credentials Types

```
ACCESS_TOKEN
  credentialsId: token string
  credentialsValue: empty
  
X509
  credentialsId: SHA3 hash
  credentialsValue: PEM certificate
  
MQTT_BASIC
  credentialsId: username
  credentialsValue: password
  
LWM2M_CREDENTIALS
  credentialsId: LwM2M endpoint
  credentialsValue: RPK/certificate data
```

---

## WebSocket Message Structure

```json
{
  "authCmd": {
    "token": "<JWT token>"
  },
  "attrSubCmd": [
    {
      "cmdId": 1,
      "key": "subscription-1",
      "entityType": "DEVICE",
      "entityId": "<uuid>",
      "keys": ["attr1", "attr2"],
      "scopes": ["SERVER_SCOPE"]
    }
  ],
  "tsSubCmd": [
    {
      "cmdId": 2,
      "key": "timeseries-sub-1",
      "entityType": "DEVICE",
      "entityId": "<uuid>",
      "keys": ["temperature"],
      "limit": 100
    }
  ]
}
```

---

## HTTP Status Codes

```
200 OK - Success
201 Created - Entity created
204 No Content - Success with no body
400 Bad Request - Invalid parameters/validation error
401 Unauthorized - Missing/invalid token
403 Forbidden - Insufficient permissions
404 Not Found - Entity doesn't exist
409 Conflict - Entity already exists (name conflict, etc.)
429 Too Many Requests - Rate limit exceeded
500 Internal Server Error - Server error
503 Service Unavailable - Server maintenance
```

---

## Error Response Format

```json
{
  "timestamp": 1700000000000,
  "status": 400,
  "error": "Bad Request",
  "message": "Description of error",
  "path": "/api/device",
  "errorCode": "INVALID_PARAMETERS"
}
```

---

## ID Type Conversions

```java
// String to ID
DeviceId deviceId = new DeviceId(UUID.fromString(strDeviceId));
AssetId assetId = new AssetId(UUID.fromString(strAssetId));
CustomerId customerId = new CustomerId(UUID.fromString(strCustomerId));
UserId userId = new UserId(UUID.fromString(strUserId));
AlarmId alarmId = new AlarmId(UUID.fromString(strAlarmId));

// ID to String
String strDeviceId = deviceId.getId().toString();
```

---

## Key DTOs

```java
// Core Entity DTOs
Device, DeviceInfo, DeviceCredentials
Asset, AssetInfo
Customer
User
Alarm, AlarmInfo
PageData<T>
JwtPair

// Attribute/Telemetry DTOs
AttributeKvEntry, TsKvEntry
SaveAttributesRequest, SaveTelemetryRequest

// Search/Query DTOs
DeviceSearchQuery, AssetSearchQuery
AlarmQuery, AlarmQueryV2
PageLink, TimePageLink

// Claim/Assignment DTOs
ClaimRequest, ClaimResult
SaveDeviceWithCredentialsRequest
```

---

## EntityType Enum Values

Used in URLs and queries:
`DEVICE, ASSET, CUSTOMER, USER, TENANT, DASHBOARD, ALARM, RULE_CHAIN, RULE_NODE, ENTITY_VIEW, EDGE, WIDGET_TYPE, TENANT_PROFILE, DEVICE_PROFILE, ASSET_PROFILE`

---

## Default Limits & Timeouts

- Message queue per WebSocket: 1000 messages
- WebSocket auth timeout: 10 seconds
- Ping timeout: 30 seconds
- Send timeout: 5 seconds
- Max text buffer: 32KB
- Max binary buffer: 32KB


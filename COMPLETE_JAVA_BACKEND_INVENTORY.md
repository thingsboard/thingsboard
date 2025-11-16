# ThingsBoard Java Backend - Complete Inventory & Architecture

**Project Version**: 4.3.0-SNAPSHOT
**Java Version**: 17+
**Build Tool**: Maven
**Database Support**: PostgreSQL, MySQL, H2, Cassandra

---

## 1. COMPLETE PROJECT STRUCTURE

### 1.1 Main Modules

```
thingsboard/
├── application/              # Main Spring Boot application (905 Java files)
├── dao/                      # Data Access Objects layer (742 Java files)
├── common/                   # Shared libraries and utilities
│   ├── cluster-api/         # Clustering support
│   ├── data/                # Common data models
│   ├── message/             # Message queuing
│   ├── proto/               # Protocol Buffers
│   ├── queue/               # Queue abstraction
│   ├── stats/               # Statistics
│   ├── transport-api/       # Transport abstractions
│   ├── util/                # Utility functions
│   ├── edqs/                # Entity Data Query System
│   └── discovery-api/       # Service discovery
├── rule-engine/             # Rule engine components (91 nodes total)
│   ├── rule-engine-api/     # API definitions
│   └── rule-engine-components/  # Node implementations
├── transport/               # Multiple protocol support
│   ├── http/                # HTTP transport
│   ├── mqtt/                # MQTT protocol
│   ├── coap/                # CoAP protocol
│   ├── lwm2m/               # LWM2M protocol
│   └── snmp/                # SNMP protocol
├── msa/                     # Microservices architecture support
├── packaging/               # Deployment packages
├── monitoring/              # Health & monitoring
├── edqs/                    # Entity Data Query System
├── rest-client/             # REST client utilities
├── tools/                   # CLI and utility tools
└── ui-ngx/                  # Frontend (Angular)

```

---

## 2. DATABASE STRUCTURE & ENTITY MODELS

### 2.1 Core Entity Models (97 entity classes total)

#### Device Management (14+ classes)
- `/common/data/src/main/java/org/thingsboard/server/common/data/Device.java`
- `/common/data/src/main/java/org/thingsboard/server/common/data/DeviceInfo.java`
- `/common/data/src/main/java/org/thingsboard/server/common/data/DeviceProfile.java`
- `/common/data/src/main/java/org/thingsboard/server/common/data/DeviceProfileInfo.java`
- `/common/data/src/main/java/org/thingsboard/server/common/data/DeviceCredentials.java`
- `/common/data/src/main/java/org/thingsboard/server/common/data/security/DeviceTokenCredentials.java`
- `/common/data/src/main/java/org/thingsboard/server/common/data/security/DeviceX509Credentials.java`

#### Asset Management (6+ classes)
- `/common/data/src/main/java/org/thingsboard/server/common/data/asset/Asset.java`
- `/common/data/src/main/java/org/thingsboard/server/common/data/asset/AssetInfo.java`
- `/common/data/src/main/java/org/thingsboard/server/common/data/asset/AssetProfile.java`
- `/common/data/src/main/java/org/thingsboard/server/common/data/asset/AssetProfileInfo.java`

#### Alarms & Events (18+ classes)
- `/common/data/src/main/java/org/thingsboard/server/common/data/alarm/Alarm.java`
- `/common/data/src/main/java/org/thingsboard/server/common/data/alarm/AlarmInfo.java`
- `/common/data/src/main/java/org/thingsboard/server/common/data/alarm/AlarmComment.java`
- `/common/data/src/main/java/org/thingsboard/server/common/data/alarm/AlarmCommentInfo.java`
- `/common/data/src/main/java/org/thingsboard/server/common/data/alarm/AlarmAssignee.java`
- `/common/data/src/main/java/org/thingsboard/server/common/data/alarm/AlarmSeverity.java` (CRITICAL, MAJOR, MINOR, WARNING, INDETERMINATE)
- `/common/data/src/main/java/org/thingsboard/server/common/data/alarm/AlarmStatus.java`
- `/common/data/src/main/java/org/thingsboard/server/common/data/alarm/AlarmQuery.java`
- `/common/data/src/main/java/org/thingsboard/server/common/data/alarm/AlarmQueryV2.java`

#### User & Access Control (8+ classes)
- `/common/data/src/main/java/org/thingsboard/server/common/data/User.java`
- `/common/data/src/main/java/org/thingsboard/server/common/data/Customer.java`
- `/common/data/src/main/java/org/thingsboard/server/common/data/Tenant.java`
- `/common/data/src/main/java/org/thingsboard/server/common/data/TenantInfo.java`
- `/common/data/src/main/java/org/thingsboard/server/common/data/TenantProfile.java`

#### Dashboard & Visualization (5+ classes)
- `/common/data/src/main/java/org/thingsboard/server/common/data/Dashboard.java`
- `/common/data/src/main/java/org/thingsboard/server/common/data/DashboardInfo.java`
- `/common/data/src/main/java/org/thingsboard/server/common/data/HomeDashboard.java`
- `/common/data/src/main/java/org/thingsboard/server/common/data/HomeDashboardInfo.java`

#### Other Core Entities
- `/common/data/src/main/java/org/thingsboard/server/common/data/EntityView.java`
- `/common/data/src/main/java/org/thingsboard/server/common/data/edge/Edge.java`
- `/common/data/src/main/java/org/thingsboard/server/common/data/edge/EdgeInfo.java`
- `/common/data/src/main/java/org/thingsboard/server/common/data/ai/AiModel.java`
- `/common/data/src/main/java/org/thingsboard/server/common/data/cf/CalculatedField.java`

### 2.2 SQL Entity Classes (66 entity classes)

**Location**: `/dao/src/main/java/org/thingsboard/server/dao/model/sql/`

#### Key Entity Tables
- `DeviceEntity` - Device storage
- `DeviceProfileEntity` - Device profile configuration
- `DeviceCredentialsEntity` - Device authentication
- `AssetEntity` - Asset storage
- `AssetProfileEntity` - Asset profiles
- `AlarmEntity` - Alarm events
- `AlarmCommentEntity` - Alarm comments
- `EntityAlarmEntity` - Alarm relationships
- `UserEntity` - User accounts
- `TenantEntity` - Multi-tenancy
- `CustomerEntity` - Customer data
- `DashboardEntity` - Dashboard definitions
- `EntityViewEntity` - Entity views
- `WidgetTypeEntity` - Widget definitions
- `RuleChainEntity` - Rule chain definitions
- `RuleNodeEntity` - Rule node instances
- `RuleNodeStateEntity` - Rule node state
- `AttributeKvEntity` - Attributes storage
- `RpcEntity` - RPC requests/responses
- `EventEntity` - System events
- `AuditLogEntity` - Audit trail
- `EdgeEntity` - Edge computing devices
- `EdgeEventEntity` - Edge events
- `NotificationRuleEntity` - Notification rules
- `NotificationTargetEntity` - Notification targets
- `NotificationTemplateEntity` - Notification templates
- `NotificationRequestInfoEntity` - Notification requests
- `OAuth2ClientEntity` - OAuth2 clients
- `DomainEntity` - Domain configuration
- `OtaPackageInfoEntity` - Over-the-air updates
- `QueueEntity` - Message queues
- `JobEntity` - Background jobs
- `AdminSettingsEntity` - System settings
- `ApiUsageStateEntity` - API usage tracking

### 2.3 Database Schema Files

- `/dao/src/main/resources/sql/schema-entities.sql` - Main entity tables
- `/dao/src/main/resources/sql/schema-ts-latest-psql.sql` - Timeseries latest values (PostgreSQL)
- `/dao/src/main/resources/sql/schema-ts-psql.sql` - Timeseries data (PostgreSQL)
- `/dao/src/main/resources/sql/schema-timescale.sql` - TimescaleDB support
- `/dao/src/main/resources/sql/schema-entities-idx.sql` - Database indexes
- `/dao/src/main/resources/sql/schema-entities-idx-psql-addon.sql` - PostgreSQL indexes
- `/dao/src/main/resources/sql/schema-views-and-functions.sql` - Database views & functions

---

## 3. REST API CONTROLLERS & ENDPOINTS (59 controllers)

### 3.1 Complete Controller List

#### Core Entity Controllers (26 controllers)
1. **DeviceController** (`/api/device/*`)
   - POST `/device` - Create device
   - GET `/device/{deviceId}` - Get device
   - DELETE `/device/{deviceId}` - Delete device
   - GET `/tenant/devices` - List tenant devices (paginated)
   - GET `/tenant/deviceInfos` - List device info (paginated)
   - GET `/customer/{customerId}/devices` - List customer devices
   - GET `/edge/{edgeId}/devices` - List edge devices
   - POST `/device/claim` - Claim device
   - POST `/device/reclaim` - Reclaim device
   - POST `/device/credentials/update` - Update device credentials

2. **AssetController** (`/api/asset/*`)
   - POST `/asset` - Create asset
   - GET `/asset/{assetId}` - Get asset
   - DELETE `/asset/{assetId}` - Delete asset
   - GET `/tenant/assets` - List tenant assets
   - GET `/customer/{customerId}/assets` - List customer assets
   - POST `/asset/{assetId}/customers/{customerId}` - Assign to customer
   - DELETE `/asset/{assetId}/customers/{customerId}` - Unassign from customer

3. **DeviceProfileController** (`/api/deviceProfile/*`)
   - POST `/deviceProfile` - Create device profile
   - GET `/deviceProfile/{deviceProfileId}` - Get device profile
   - DELETE `/deviceProfile/{deviceProfileId}` - Delete device profile
   - GET `/deviceProfiles` - List device profiles
   - POST `/deviceProfile/{deviceProfileId}/default` - Set as default

4. **AssetProfileController** (`/api/assetProfile/*`)
   - POST `/assetProfile` - Create asset profile
   - GET `/assetProfile/{assetProfileId}` - Get asset profile
   - DELETE `/assetProfile/{assetProfileId}` - Delete asset profile
   - GET `/assetProfiles` - List asset profiles
   - POST `/assetProfile/{assetProfileId}/default` - Set as default

5. **AlarmController** (`/api/alarm/*`)
   - POST `/alarm` - Create alarm
   - GET `/alarm/{alarmId}` - Get alarm
   - DELETE `/alarm/{alarmId}` - Delete alarm
   - GET `/alarms` - Search alarms
   - POST `/alarm/{alarmId}/ack` - Acknowledge alarm
   - POST `/alarm/{alarmId}/clear` - Clear alarm
   - POST `/alarm/{alarmId}/assign` - Assign alarm
   - POST `/alarm/{alarmId}/unassign` - Unassign alarm

6. **AlarmCommentController** (`/api/alarmComment/*`)
   - POST `/alarmComment` - Add alarm comment
   - GET `/alarmComments/{alarmId}` - Get alarm comments
   - DELETE `/alarmComment/{commentId}` - Delete comment

7. **DashboardController** (`/api/dashboard/*`)
   - POST `/dashboard` - Create dashboard
   - GET `/dashboard/{dashboardId}` - Get dashboard
   - DELETE `/dashboard/{dashboardId}` - Delete dashboard
   - GET `/customer/{customerId}/dashboards` - List customer dashboards
   - POST `/dashboard/{dashboardId}/customers/{customerId}` - Share dashboard

8. **EntityViewController** (`/api/entityView/*`)
   - POST `/entityView` - Create entity view
   - GET `/entityView/{entityViewId}` - Get entity view
   - DELETE `/entityView/{entityViewId}` - Delete entity view
   - GET `/customer/{customerId}/entityViews` - List customer entity views

9. **UserController** (`/api/user/*`)
   - POST `/user` - Create user
   - GET `/user/{userId}` - Get user
   - DELETE `/user/{userId}` - Delete user
   - GET `/tenant/users` - List tenant users
   - GET `/customer/{customerId}/users` - List customer users
   - GET `/user/tokenRefresh` - Refresh token

10. **CustomerController** (`/api/customer/*`)
    - POST `/customer` - Create customer
    - GET `/customer/{customerId}` - Get customer
    - DELETE `/customer/{customerId}` - Delete customer
    - GET `/customers` - List customers

11. **TenantController** (`/api/tenant/*`)
    - POST `/tenant` - Create tenant
    - GET `/tenant/{tenantId}` - Get tenant
    - DELETE `/tenant/{tenantId}` - Delete tenant
    - GET `/tenants` - List tenants

12. **TenantProfileController** (`/api/tenantProfile/*`)
    - POST `/tenantProfile` - Create tenant profile
    - GET `/tenantProfile/{tenantProfileId}` - Get tenant profile
    - DELETE `/tenantProfile/{tenantProfileId}` - Delete tenant profile

13. **EdgeController** (`/api/edge/*`)
    - POST `/edge` - Create edge
    - GET `/edge/{edgeId}` - Get edge
    - DELETE `/edge/{edgeId}` - Delete edge
    - GET `/tenant/edges` - List tenant edges
    - GET `/customer/{customerId}/edges` - List customer edges

14. **WidgetTypeController** (`/api/widgetType/*`)
    - POST `/widgetType` - Create widget type
    - GET `/widgetType/{widgetTypeId}` - Get widget type
    - DELETE `/widgetType/{widgetTypeId}` - Delete widget type
    - GET `/widgetTypes` - List widget types

15. **WidgetsBundleController** (`/api/widgetsBundle/*`)
    - POST `/widgetsBundle` - Create widgets bundle
    - GET `/widgetsBundle/{widgetsBundleId}` - Get widgets bundle
    - DELETE `/widgetsBundle/{widgetsBundleId}` - Delete widgets bundle

16. **CalculatedFieldController** (`/api/cf/*`)
    - POST `/cf` - Create calculated field
    - GET `/cf/{cfId}` - Get calculated field
    - DELETE `/cf/{cfId}` - Delete calculated field

17. **OtaPackageController** (`/api/otaPackage/*`)
    - POST `/otaPackage` - Create OTA package
    - GET `/otaPackage/{otaPackageId}` - Get OTA package
    - DELETE `/otaPackage/{otaPackageId}` - Delete OTA package

18. **RuleChainController** (`/api/ruleChain/*`)
    - POST `/ruleChain` - Create rule chain
    - GET `/ruleChain/{ruleChainId}` - Get rule chain
    - GET `/ruleChain/{ruleChainId}/metadata` - Get rule chain metadata
    - DELETE `/ruleChain/{ruleChainId}` - Delete rule chain
    - POST `/ruleChain/{ruleChainId}/root` - Set as root rule chain

19. **EntityRelationController** (`/api/relation/*`)
    - POST `/relation` - Create relation
    - DELETE `/relation` - Delete relation
    - GET `/relations` - Get relations

20. **AiModelController** (`/api/aiModel/*`)
    - POST `/aiModel` - Create AI model
    - GET `/aiModel/{aiModelId}` - Get AI model
    - DELETE `/aiModel/{aiModelId}` - Delete AI model

21. **QueueController** (`/api/queue/*`)
    - POST `/queue` - Create queue
    - GET `/queue/{queueId}` - Get queue
    - DELETE `/queue/{queueId}` - Delete queue

22. **DomainController** (`/api/domain/*`)
    - POST `/domain` - Create domain
    - GET `/domain/{domainId}` - Get domain
    - DELETE `/domain/{domainId}` - Delete domain

23. **MobileAppController** (`/api/mobileApp/*`)
    - POST `/mobileApp` - Create mobile app
    - GET `/mobileApp/{mobileAppId}` - Get mobile app
    - DELETE `/mobileApp/{mobileAppId}` - Delete mobile app

24. **MobileAppBundleController** (`/api/mobileAppBundle/*`)
    - POST `/mobileAppBundle` - Create mobile app bundle
    - GET `/mobileAppBundle/{bundleId}` - Get mobile app bundle
    - DELETE `/mobileAppBundle/{bundleId}` - Delete mobile app bundle

25. **TbResourceController** (`/api/resource/*`)
    - POST `/resource` - Upload resource
    - GET `/resource/{resourceId}` - Download resource
    - DELETE `/resource/{resourceId}` - Delete resource
    - GET `/resource/{resourceType}/{scope}/{key}` - Get resource by key

26. **NotificationRuleController** (`/api/notification/rule/*`)
    - POST `/rule` - Create notification rule
    - GET `/rule/{id}` - Get notification rule
    - DELETE `/rule/{id}` - Delete notification rule

#### Data & Query Controllers (8 controllers)
27. **TelemetryController** (`/api/telemetry/*`)
    - GET `/{entityType}/{entityId}/keys/attributes` - Get attribute keys
    - GET `/{entityType}/{entityId}/values/attributes/{scope}` - Get attributes
    - POST `/{entityType}/{entityId}/attributes/{scope}` - Save attributes
    - POST `/{entityType}/{entityId}/timeseries/{scope}` - Save timeseries
    - GET `/{entityType}/{entityId}/values/timeseries` - Get timeseries
    - DELETE `/{entityType}/{entityId}/timeseries/delete` - Delete timeseries

28. **EntityQueryController** (`/api/entitiesQuery/*`)
    - POST `/find` - Find entities by query
    - POST `/count` - Count entities
    - POST `/alarmsQuery/find` - Find alarms by query
    - POST `/alarmsQuery/count` - Count alarms
    - POST `/edqs/system/request` - EDQS system request

29. **EventController** (`/api/event/*`)
    - GET `/events` - Get events

30. **AuditLogController** (`/api/audit/*`)
    - GET `/logs` - Get audit logs

31. **EntityViewController** (`/api/entityView/*`)
    - GET `/{entityViewId}/entities` - Get entity view members

32. **EntitiesVersionControlController** (`/api/entities/version/*`)
    - POST `/export` - Export entities
    - POST `/import` - Import entities

33. **RuleEngineController** (`/api/rule/*`)
    - GET `/nodeDefinitions` - Get rule node definitions

34. **ComponentDescriptorController** (`/api/component/*`)
    - GET `/descriptor/{componentType}` - Get component descriptor

#### Authentication & Security Controllers (7 controllers)
35. **AuthController** (`/api/auth/*`)
    - POST `/login` - User login
    - POST `/logout` - User logout
    - GET `/user` - Get current user
    - POST `/changePassword` - Change password
    - POST `/activateUser` - Activate user

36. **OAuth2Controller** (`/api/oauth2/*`)
    - POST `/authorize` - OAuth2 authorization
    - POST `/token` - OAuth2 token exchange
    - GET `/clients` - List OAuth2 clients

37. **OAuth2ConfigTemplateController** (`/api/oauth2/config/template/*`)
    - POST `/` - Create OAuth2 config template
    - DELETE `/{templateId}` - Delete OAuth2 config template
    - GET `/` - List OAuth2 config templates

38. **TwoFactorAuthController** (`/api/2fa/*`)
    - POST `/provision` - Provision 2FA
    - POST `/verify` - Verify 2FA code

39. **TwoFactorAuthConfigController** (`/api/2fa/config/*`)
    - POST `/` - Configure 2FA
    - GET `/` - Get 2FA configuration

40. **AdminController** (`/api/admin/*`)
    - GET `/settings` - Get system settings
    - POST `/settings` - Update system settings

41. **UiSettingsController** (`/api/ui/settings/*`)
    - GET `/` - Get UI settings
    - POST `/` - Update UI settings

#### Edge & RPC Controllers (6 controllers)
42. **EdgeEventController** (`/api/edge/{edgeId}/events/*`)
    - GET `/events` - Get edge events

43. **RpcV1Controller** (`/api/rpc/v1/*`)
    - POST `/{deviceId}/oneWay/{rpcName}` - One-way RPC call
    - POST `/{deviceId}/twoWay/{rpcName}` - Two-way RPC call

44. **RpcV2Controller** (`/api/rpc/v2/*`)
    - POST `/device/{deviceId}/rpcRequest` - Send RPC request
    - GET `/device/{deviceId}/rpcResponse/{rpcId}` - Get RPC response

45. **AbstractRpcController** - Base class for RPC controllers

#### Notification & Message Controllers (5 controllers)
46. **NotificationController** (`/api/notification/*`)
    - GET `/` - Get notifications
    - POST `/{notificationId}/read` - Mark as read

47. **NotificationTargetController** (`/api/notification/target/*`)
    - POST `/` - Create notification target
    - GET `/{id}` - Get notification target
    - DELETE `/{id}` - Delete notification target

48. **NotificationTemplateController** (`/api/notification/template/*`)
    - POST `/` - Create notification template
    - GET `/{id}` - Get notification template
    - DELETE `/{id}` - Delete notification template

49. **MailConfigTemplateController** (`/api/mailConfigTemplate/*`)
    - POST `/` - Create mail config
    - GET `/` - Get mail config

#### System & Configuration Controllers (4 controllers)
50. **SystemInfoController** (`/api/system/*`)
    - GET `/info` - Get system info
    - GET `/params` - Get system parameters

51. **QueueStatsController** (`/api/queue/stats/*`)
    - GET `/` - Get queue statistics

52. **UsageInfoController** (`/api/usage/*`)
    - GET `/info` - Get usage information

53. **AutoCommitController** (`/api/autocommit/*`)
    - GET `/state` - Get version control state

#### Protocol & Integration Controllers (5 controllers)
54. **DeviceConnectivityController** (`/api/device/connectivity/*`)
    - GET `/info` - Get device connectivity info

55. **Lwm2mController** (`/api/lwm2m/*`)
    - GET `/deviceProfile/bootstrap/{isBootstrapServer}` - Get LWM2M config
    - POST `/device-credentials` - Create LWM2M credentials

56. **TrendzController** (`/api/trendz/*`)
    - POST `/settings` - Save Trendz settings
    - GET `/settings` - Get Trendz settings

57. **QrCodeSettingsController** (`/api/qrCodeSettings/*`)
    - POST `/` - Save QR code settings
    - GET `/` - Get QR code settings

58. **ImageController** (`/api/image/*`)
    - POST `/upload` - Upload image
    - GET `/{imageId}` - Download image

59. **JobController** (`/api/job/*`)
    - POST `/` - Create job
    - GET `/{jobId}` - Get job
    - DELETE `/{jobId}` - Delete job

---

## 4. DATA ACCESS LAYER (DAO & REPOSITORIES)

### 4.1 DAO Interfaces (60+ interface classes)

**Location**: `/dao/src/main/java/org/thingsboard/server/dao/`

#### Device Management DAOs
- `device/DeviceDao.java` - Device data access
- `device/DeviceProfileDao.java` - Device profile data access
- `device/DeviceCredentialsDao.java` - Device credentials access
- `sql/device/JpaDeviceDao.java` - JPA Device implementation
- `sql/device/JpaDeviceProfileDao.java` - JPA Device profile implementation
- `sql/device/JpaDeviceCredentialsDao.java` - JPA credentials implementation
- `sql/device/DeviceRepository.java` - Spring Data Repository
- `sql/device/DeviceProfileRepository.java` - Spring Data Repository
- `sql/device/DeviceCredentialsRepository.java` - Spring Data Repository

#### Asset Management DAOs
- `asset/AssetDao.java` - Asset data access
- `asset/AssetProfileDao.java` - Asset profile data access
- `sql/asset/JpaAssetDao.java` - JPA Asset implementation
- `sql/asset/JpaAssetProfileDao.java` - JPA Asset profile implementation
- `sql/asset/AssetRepository.java` - Spring Data Repository
- `sql/asset/AssetProfileRepository.java` - Spring Data Repository

#### Alarm & Event DAOs
- `alarm/AlarmDao.java` - Alarm data access
- `alarm/AlarmCommentDao.java` - Alarm comment data access
- `alarm/AlarmQueryDao.java` - Alarm query operations
- `sql/alarm/JpaAlarmDao.java` - JPA Alarm implementation
- `sql/alarm/JpaAlarmCommentDao.java` - JPA comment implementation
- `event/EventDao.java` - Event data access
- `sql/event/EventRepository.java` - Spring Data Repository

#### User & Access DAOs
- `user/UserDao.java` - User data access
- `customer/CustomerDao.java` - Customer data access
- `tenant/TenantDao.java` - Tenant data access
- `tenant/TenantProfileDao.java` - Tenant profile data access
- `audit/AuditLogDao.java` - Audit log data access
- `sql/user/UserRepository.java` - Spring Data Repository
- `sql/user/UserCredentialsRepository.java` - Spring Data Repository
- `sql/customer/CustomerRepository.java` - Spring Data Repository
- `sql/tenant/TenantRepository.java` - Spring Data Repository
- `sql/audit/AuditLogRepository.java` - Spring Data Repository

#### Dashboard & Entity View DAOs
- `dashboard/DashboardDao.java` - Dashboard data access
- `entityview/EntityViewDao.java` - Entity view data access
- `sql/dashboard/DashboardRepository.java` - Spring Data Repository
- `sql/entityview/EntityViewRepository.java` - Spring Data Repository

#### Attribute & Timeseries DAOs
- `attributes/AttributesDao.java` - Attributes data access
- `timeseries/TimeseriesDao.java` - Timeseries data access
- `timeseries/TimeseriesLatestDao.java` - Latest timeseries data
- `sql/attributes/JpaAttributeDao.java` - JPA Attributes implementation
- `sql/attributes/AttributeKvRepository.java` - Spring Data Repository
- `sqlts/timeseries/SqlTimeseriesDao.java` - SQL Timeseries implementation

#### Rule Chain & Node DAOs
- `rule/RuleChainDao.java` - Rule chain data access
- `rule/RuleNodeDao.java` - Rule node data access
- `rule/RuleNodeStateDao.java` - Rule node state access
- `sql/rule/JpaRuleChainDao.java` - JPA Rule chain implementation
- `sql/rule/JpaRuleNodeDao.java` - JPA Rule node implementation
- `sql/rule/RuleChainRepository.java` - Spring Data Repository
- `sql/rule/RuleNodeRepository.java` - Spring Data Repository

#### Additional DAOs (30+)
- `relation/RelationDao.java` - Entity relations
- `rpc/RpcDao.java` - RPC requests/responses
- `widget/WidgetTypeDao.java` - Widget types
- `queue/QueueDao.java` - Queue configuration
- `edge/EdgeDao.java` - Edge devices
- `notification/NotificationRuleDao.java` - Notification rules
- `notification/NotificationTargetDao.java` - Notification targets
- `notification/NotificationTemplateDao.java` - Notification templates
- `notification/NotificationDao.java` - Notifications
- `ota/OtaPackageDao.java` - OTA packages
- `ai/AiModelDao.java` - AI models
- `component/ComponentDescriptorDao.java` - Component descriptors
- `domain/DomainDao.java` - Domain configuration
- `oauth2/OAuth2ClientDao.java` - OAuth2 clients
- `settings/AdminSettingsDao.java` - System settings
- `resource/ResourceDao.java` - File resources
- `cf/CalculatedFieldDao.java` - Calculated fields
- `job/JobDao.java` - Background jobs
- `mobile/MobileAppDao.java` - Mobile app configuration
- `usagerecord/ApiUsageStateDao.java` - API usage tracking
- `dictionary/DictionaryDao.java` - Dictionary data

### 4.2 JPA Repository Interfaces (84 repositories)

**Location**: `/dao/src/main/java/org/thingsboard/server/dao/sql/`

#### Spring Data JPA Repositories (Sample)
- `device/DeviceRepository.java`
- `asset/AssetRepository.java`
- `customer/CustomerRepository.java`
- `tenant/TenantRepository.java`
- `user/UserRepository.java`
- `dashboard/DashboardRepository.java`
- `attributes/AttributeKvRepository.java`
- `rule/RuleChainRepository.java`
- `notification/NotificationRuleRepository.java`
- `oauth2/OAuth2ClientRepository.java`
- `queue/QueueRepository.java`
- `edge/EdgeRepository.java`
- And many more...

---

## 5. SERVICE LAYER ARCHITECTURE

### 5.1 Entity Services (30+ service classes)

**Location**: `/application/src/main/java/org/thingsboard/server/service/entitiy/`

#### Main Entity Services
1. **TbDeviceService**
   - `DefaultTbDeviceService` - Device CRUD & management
   - Methods: save(), delete(), findById(), findByTenantId(), etc.

2. **TbAssetService**
   - `DefaultTbAssetService` - Asset CRUD & management
   - Methods: save(), delete(), findById(), findByTenantId(), etc.

3. **TbAlarmService**
   - `DefaultTbAlarmService` - Alarm management
   - Methods: createAlarm(), acknowledgeAlarm(), clearAlarm(), assignAlarm(), etc.

4. **TbAlarmCommentService**
   - `DefaultTbAlarmCommentService` - Alarm comment management

5. **TbDashboardService**
   - `DefaultTbDashboardService` - Dashboard management
   - DashboardSyncService for synchronization

6. **TbCustomerService**
   - `DefaultTbCustomerService` - Customer management

7. **TbTenantService**
   - `DefaultTbTenantService` - Tenant management

8. **TbUserService**
   - `DefaultUserService` - User account management
   - TbUserSettingsService - User preferences

9. **TbDeviceProfileService**
   - `DefaultTbDeviceProfileService` - Device profile management

10. **TbAssetProfileService**
    - `DefaultTbAssetProfileService` - Asset profile management

11. **TbTenantProfileService**
    - `DefaultTbTenantProfileService` - Tenant profile management

12. **TbEntityViewService**
    - `DefaultTbEntityViewService` - Entity view management

13. **TbEntityRelationService**
    - `DefaultTbEntityRelationService` - Entity relation management

14. **TbCalculatedFieldService**
    - `DefaultTbCalculatedFieldService` - Calculated field management

15. **TbOtaPackageService**
    - `DefaultTbOtaPackageService` - OTA package management

16. **TbQueueService**
    - `DefaultTbQueueService` - Queue configuration service

17. **TbDomainService**
    - `DefaultTbDomainService` - Domain management

18. **TbEdgeService**
    - `DefaultTbEdgeService` - Edge device management

19. **TbWidgetsBundleService**
    - `DefaultWidgetsBundleService` - Widgets bundle management

20. **TbWidgetTypeService**
    - `DefaultWidgetTypeService` - Widget type management

21. **TbOauth2ClientService**
    - `DefaultTbOauth2ClientService` - OAuth2 client management

22. **TbMobileAppService**
    - `DefaultTbMobileAppService` - Mobile app management

23. **TbMobileAppBundleService**
    - `DefaultTbMobileAppBundleService` - Mobile app bundle management

24. **TbAiModelService**
    - `DefaultTbAiModelService` - AI model management

### 5.2 Core Business Services (25+ service classes)

**Location**: `/application/src/main/java/org/thingsboard/server/service/`

#### Telemetry & Data Services
- **TelemetrySubscriptionService** - Telemetry subscriptions
- **AttributesService** - Attributes management
- **TimeseriesService** - Timeseries data management
- **BaseTimeseriesService** - Timeseries base implementation
- **TbAttributesService** - Attributes business logic

#### Rule Engine Services
- **DefaultRuleEngineService** - Rule engine execution
- **RuleEngineComponentDiscoveryService** - Rule node discovery
- **ComponentDiscoveryService** - Component descriptor service
- **AnnotationComponentDiscoveryService** - Annotation-based discovery

#### Notification Services
- **DefaultNotificationService** - Notification dispatch
- **DefaultNotificationRuleService** - Notification rule management
- **DefaultNotificationTemplateService** - Notification template management
- **DefaultNotificationTargetService** - Notification target management
- **DefaultNotificationRequestService** - Notification request tracking

#### Edge & Sync Services
- **EdgeRpcService** - Edge RPC communication
- **EdgeGrpcService** - Edge gRPC communication
- **EdgeEventSourcingListener** - Edge event synchronization
- **RelatedEdgesSourcingListener** - Related edges sync
- **EntityStateSourcingListener** - Entity state sourcing
- **DashboardSyncService** - Dashboard synchronization

#### AI & ML Services
- **AiChatModelService** - AI chat model integration
- **DefaultAiRequestsExecutor** - AI request execution
- **Langchain4jChatModelConfigurerImpl** - Langchain4j configuration

#### Calculated Fields Services
- **DefaultCalculatedFieldCache** - Calculated field caching
- **DefaultCalculatedFieldProcessingService** - CF computation
- **DefaultCalculatedFieldQueueService** - CF queue management
- **CalculatedFieldStateService** - CF state management
- **CalculatedFieldScriptEngine** - CF script evaluation
- **CalculatedFieldTbelScriptEngine** - TBEL script engine

#### System & Admin Services
- **SystemSecurityService** - System security
- **DefaultSecuritySettingsService** - Security settings
- **DefaultTbApiUsageStateService** - API usage tracking
- **TbApiUsageStateService** - API state interface
- **DefaultApiLimitService** - API rate limiting
- **UsageInfoService** - Usage information

#### SMS & Communication Services
- **SmsSenderService** - SMS sending (abstract)
- **TwilioSmsSender** - Twilio integration
- **AwsSmsSender** - AWS SNS integration
- **SmppSmsSender** - SMPP protocol support

#### Job & Scheduling Services
- **DefaultJobService** - Background job execution
- **JobScheduler** - Job scheduling

#### Device Services
- **DeviceBulkImportService** - Bulk device import
- **AssetBulkImportService** - Bulk asset import
- **DeviceProvisionServiceImpl** - Device provisioning
- **ClaimDevicesServiceImpl** - Device claiming

#### Other Services
- **DefaultEdqsService** - Entity Data Query System
- **DefaultEdqsApiService** - EDQS API service
- **EdgeStatsService** - Edge statistics
- **QueueStatsService** - Queue statistics
- **EntityActionService** - Entity action tracking
- **TbLogEntityActionService** - Entity action logging

### 5.3 DAO Services Base Classes

**Location**: `/dao/src/main/java/org/thingsboard/server/dao/`

- **BaseEntityService** - Generic entity service
- **AbstractEntityService** - Entity service abstract
- **CachedEntityService** - Cached entity access
- **AbstractCachedEntityService** - Cached entity abstract
- **BaseEntityCountService** - Entity counting
- **BaseTimeseriesService** - Timeseries base
- **BaseAttributesService** - Attributes base
- **BaseRelationService** - Relations base
- **BaseRuleChainService** - Rule chain base
- **BaseRpcService** - RPC base
- **BaseQueueService** - Queue base
- **BaseOtaPackageService** - OTA base
- **BaseResourceService** - Resource base
- **BaseImageService** - Image base
- **BaseEdgeEventService** - Edge event base

---

## 6. WEBSOCKET IMPLEMENTATION

### 6.1 WebSocket Service Architecture

**Location**: `/application/src/main/java/org/thingsboard/server/service/ws/`

#### Core WebSocket Files
1. **WebSocketService** (Interface)
   - `handleSessionEvent()` - Handle connection/disconnection
   - `handleCommands()` - Process WebSocket commands
   - `sendUpdate()` - Send telemetry updates
   - `sendError()` - Send error messages
   - `close()` - Close connection
   - `cleanupIfStale()` - Cleanup stale sessions

2. **DefaultWebSocketService** (Implementation)
   - Session management
   - Command dispatching
   - Update broadcasting

3. **WebSocketSessionRef** - Session reference
4. **WebSocketSessionType** - Enumeration of session types
5. **WebSocketMsgEndpoint** - Message endpoint

### 6.2 WebSocket Controller & Handler

**Location**: `/application/src/main/java/org/thingsboard/server/controller/plugin/`

1. **TbWebSocketHandler**
   - Handles WebSocket lifecycle
   - Message routing
   - Error handling

2. **TbWebSocketMsg** (Interface)
   - Base message interface

3. **TbWebSocketTextMsg** - Text message
4. **TbWebSocketPingMsg** - Ping message
5. **TbWebSocketMsgType** - Message type enumeration

### 6.3 WebSocket Configuration

**Location**: `/application/src/main/java/org/thingsboard/server/config/`

- **WebSocketConfiguration** - Spring WebSocket configuration
- STOMP & SockJS support
- Message broker configuration

### 6.4 Telemetry WebSocket Subscriptions

**Location**: `/application/src/main/java/org/thingsboard/server/service/ws/telemetry/`

- **TelemetryWebSocketTextMsg** - Telemetry message format
- Telemetry subscription management
- Real-time data push

### 6.5 Test Support

- **TbTestWebSocketClient** - WebSocket test client
- **TbWebSocketHandlerTest** - Handler tests

---

## 7. SECURITY & AUTHENTICATION IMPLEMENTATION

### 7.1 Security Configuration Files

**Location**: `/application/src/main/java/org/thingsboard/server/config/`

1. **ThingsboardSecurityConfiguration**
   - Spring Security configuration
   - JWT configuration
   - CORS configuration
   - Servlet filter configuration

2. **TbRuleEngineSecurityConfiguration**
   - Rule engine security context

### 7.2 Security Services

**Location**: `/application/src/main/java/org/thingsboard/server/service/security/`

#### Core Security Services
1. **SecurityUser** - Current user principal
2. **SystemSecurityService** - System security operations
3. **DefaultSystemSecurityService** - Default implementation

#### Security Models
- **UserAuthSettings** - User authentication settings
- **UserCredentials** - User password credentials
- **DeviceTokenCredentials** - Device access token
- **DeviceX509Credentials** - Device X.509 certificate
- **DeviceCredentials** - Base credential class

#### Security Settings
- **SecuritySettings** - Security configuration model
- **UserPasswordPolicy** - Password policy enforcement
- **SecuritySettingsService** - Settings persistence

### 7.3 Authentication Types

1. **JWT (JSON Web Tokens)**
   - Token-based authentication
   - Refresh token support
   - JJWT library (v0.12.5)

2. **OAuth2**
   - OAuth2ClientServiceImpl
   - HybridClientRegistrationRepository
   - OAuth2Configuration
   - 20+ OAuth2 related classes

3. **Device Credentials**
   - Access tokens
   - X.509 certificates
   - Pre-shared keys

4. **Two-Factor Authentication (2FA)**
   - TwoFactorAuthController
   - TwoFactorAuthConfigController
   - TOTP support (Time-based One-Time Password)

### 7.4 Permission & Authorization

**Location**: `/service/security/permission/`

- **Resource** - Resource types (Device, Asset, etc.)
- **Operation** - Operations (Read, Write, Create, Delete)
- Role-based access control (RBAC)

### 7.5 Transport Security

**Location**: `/common/transport/http/src/main/java/org/thingsboard/server/transport/http/config/`

- **TransportSecurityConfiguration** - Transport layer security
- MQTT TLS support
- CoAP DTLS support
- HTTP/HTTPS support

---

## 8. RULE ENGINE STRUCTURE & COMPONENTS

### 8.1 Rule Engine Overview

**Location**: `/rule-engine/`

The rule engine consists of 91 node implementations organized into multiple categories.

### 8.2 Rule Engine API

**Location**: `/rule-engine/rule-engine-api/src/main/java/org/thingsboard/rule/engine/api/`

#### Core Interfaces & Classes
1. **RuleNode** (Interface)
   - Node lifecycle methods
   - Configuration access
   - Context binding

2. **RuleNodeCtx** - Rule node execution context
3. **TbMsg** - Message structure
4. **TbMsgMetaData** - Message metadata
5. **NodeConfiguration** - Node configuration interface
6. **RuleChainInputMsg** - Input message
7. **RuleChainOutputMsg** - Output message

### 8.3 Rule Engine Components (91 Total Nodes)

#### Filter Nodes (10 nodes)
1. **TbMsgTypeFilterNode** - Filter by message type
2. **TbOriginatorTypeFilterNode** - Filter by originator type
3. **TbDeviceTypeSwitchNode** - Switch by device type
4. **TbAssetTypeSwitchNode** - Switch by asset type
5. **TbOriginatorTypeSwitchNode** - Switch by originator type
6. **TbMsgTypeSwitchNode** - Switch by message type
7. **TbJsFilterNode** - JavaScript filter
8. **TbJsSwitchNode** - JavaScript switch
9. **TbCheckMessageNode** - Message validation
10. **TbCheckRelationNode** - Relation validation
11. **TbCheckAlarmStatusNode** - Alarm status check

#### Transform Nodes (7 nodes)
1. **TbTransformMsgNode** - Transform message payload
2. **TbChangeOriginatorNode** - Change message originator
3. **TbCopyKeysNode** - Copy keys in message
4. **TbRenameKeysNode** - Rename keys
5. **TbDeleteKeysNode** - Delete keys
6. **TbJsonPathNode** - Extract data via JSONPath
7. **TbSplitArrayMsgNode** - Split array into messages

#### Action Nodes (11 nodes)
1. **TbCreateAlarmNode** - Create alarm
2. **TbClearAlarmNode** - Clear alarm
3. **TbDeviceStateNode** - Track device state
4. **TbLogNode** - Log message
5. **TbMsgCountNode** - Count messages
6. **TbCreateRelationNode** - Create entity relation
7. **TbDeleteRelationNode** - Delete relation
8. **TbAssignToCustomerNode** - Assign entity to customer
9. **TbUnassignFromCustomerNode** - Unassign from customer
10. **TbCopyAttributesToEntityViewNode** - Copy attributes to entity view
11. **TbSaveToCustomCassandraTableNode** - Save to Cassandra

#### External Integration Nodes (8 nodes)
1. **TbRestApiCallNode** - Call REST API
2. **TbSendRestApiCallReplyNode** - Send REST reply
3. **TbKafkaNode** - Kafka producer
4. **TbRabbitMqNode** - RabbitMQ producer
5. **TbSendSmsNode** - Send SMS
6. **TbNotificationNode** - Send notification
7. **TbSlackNode** - Send Slack message
8. **TbAbstractExternalNode** - Base external node

#### Flow Control Nodes (6 nodes)
1. **TbRuleChainInputNode** - Chain input
2. **TbRuleChainOutputNode** - Chain output
3. **TbAckNode** - Acknowledge message
4. **TbCheckpointNode** - Message checkpoint
5. **TbSynchronizationBeginNode** - Begin synchronization
6. **TbSynchronizationEndNode** - End synchronization

#### Other Nodes (49 nodes)
1. **TbMsgGeneratorNode** - Generate test messages
2. **TbMsgDeduplicationNode** - Deduplicate messages
3. Various other specialized nodes for specific use cases

### 8.4 Rule Chain Management

**Location**: `/dao/src/main/java/org/thingsboard/server/dao/rule/`

- **RuleChainDao** - Rule chain persistence
- **RuleNodeDao** - Rule node persistence
- **RuleNodeStateDao** - Rule node state persistence
- **BaseRuleChainService** - Rule chain service
- **BaseRuleNodeStateService** - Rule node state service

### 8.5 Rule Chain Data Models

**Location**: `/common/data/src/main/java/org/thingsboard/server/common/data/rule/`

- **RuleChain** - Rule chain definition
- **RuleChainMetaData** - Rule chain metadata with connections
- **RuleNode** - Individual rule node
- **RuleNodeState** - Node execution state
- **NodeConnectionInfo** - Connection between nodes
- **RuleType** - Rule type enumeration

---

## 9. TRANSPORT LAYER IMPLEMENTATION

### 9.1 Transport Modules

**Location**: `/transport/`

#### Available Transports
1. **HTTP/HTTPS** (`/transport/http/`)
   - REST API for telemetry submission
   - Device connectivity over HTTP
   - Configuration & management

2. **MQTT** (`/transport/mqtt/`)
   - MQTT protocol support
   - Subscribe/publish for telemetry
   - Device provisioning over MQTT
   - QoS support

3. **CoAP** (`/transport/coap/`)
   - CoAP (Constrained Application Protocol)
   - Lightweight M2M support
   - Resource discovery

4. **LWM2M** (`/transport/lwm2m/`)
   - Lightweight M2M protocol (OMA)
   - Bootstrap server support
   - Security modes (NoSec, PSK, RPK, X.509)
   - Firmware updates

5. **SNMP** (`/transport/snmp/`)
   - SNMP protocol support
   - Trap handling
   - Device integration

### 9.2 HTTP Transport

**Location**: `/common/transport/http/src/main/java/org/thingsboard/server/transport/http/`

#### Key Classes
1. **DeviceApiController** - Device API endpoints
   - `/api/v1/` endpoints for device communication
   - Telemetry push
   - Attribute updates
   - RPC handling

2. **HttpTransportContext** - HTTP transport context
3. **TransportSecurityConfiguration** - TLS/HTTPS setup
4. **PayloadSizeFilter** - Request size limiting

### 9.3 Transport API Interfaces

**Location**: `/common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/`

#### Core Interfaces
1. **SessionMsgListener** - Message listener interface
2. **DeviceAuthService** - Device authentication
   - ValidateDeviceCredentialsResponse
   - GetOrCreateDeviceFromGatewayResponse
   - DeviceAuthResult
   - TransportDeviceInfo

3. **SessionInfoCreator** - Session creation
4. **DeviceProfileAware** - Device profile support

### 9.4 MQTT Configuration

**Location**: `/common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/`

- MQTT broker configuration
- Topic mapping (v1, v3.1.1)
- QoS levels (0, 1, 2)
- Client ID management

### 9.5 LWM2M Bootstrap

**Location**: `/common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/bootstrap/`

- **LwM2MTransportBootstrapService** - Bootstrap service
- **LwM2MBootstrapConfig** - Bootstrap configuration
- **LwM2MBootstrapSecurityStore** - Security credentials
- **LwM2MInMemoryBootstrapConfigStore** - In-memory store
- Certificate & PSK management

---

## 10. COMPLETE API ENDPOINTS ORGANIZED BY ENTITY

### 10.1 DEVICE ENDPOINTS

```
Device Management:
  POST   /api/device                          - Create device
  GET    /api/device/{deviceId}               - Get device by ID
  DELETE /api/device/{deviceId}               - Delete device
  GET    /api/tenant/devices                  - Paginated list of tenant devices
  GET    /api/tenant/deviceInfos              - Paginated tenant device info
  GET    /api/customer/{customerId}/devices   - List customer devices
  GET    /api/customer/{customerId}/deviceInfos - Paginated customer devices
  GET    /api/devices                         - List devices by IDs
  POST   /api/device/{deviceId}/credentials   - Update device credentials
  
Device Searching:
  POST   /api/devices/search                  - Search devices by query
  GET    /api/deviceTypes                     - Get available device types
  
Device Claiming:
  POST   /api/device/claim                    - Claim device by name
  POST   /api/device/{deviceId}/reclaim       - Reclaim device
  
Device Connectivity:
  GET    /api/device/connectivity/info        - Get device connectivity status
```

### 10.2 ASSET ENDPOINTS

```
Asset Management:
  POST   /api/asset                           - Create asset
  GET    /api/asset/{assetId}                 - Get asset by ID
  DELETE /api/asset/{assetId}                 - Delete asset
  GET    /api/tenant/assets                   - List tenant assets
  GET    /api/tenant/assetInfos               - Paginated tenant asset info
  GET    /api/customer/{customerId}/assets    - List customer assets
  GET    /api/customer/{customerId}/assetInfos - Paginated customer assets
  GET    /api/assets                          - List assets by IDs
  
Asset Assignment:
  POST   /api/asset/{assetId}/customers/{customerId}  - Assign to customer
  DELETE /api/asset/{assetId}/customers/{customerId}  - Unassign from customer
  
Asset Searching:
  POST   /api/assets/search                   - Search assets by query
  GET    /api/assetTypes                      - Get available asset types
```

### 10.3 ALARM ENDPOINTS

```
Alarm Management:
  POST   /api/alarm                           - Create alarm
  GET    /api/alarm/{alarmId}                 - Get alarm by ID
  DELETE /api/alarm/{alarmId}                 - Delete alarm
  GET    /api/alarms                          - Search alarms
  
Alarm Actions:
  POST   /api/alarm/{alarmId}/ack             - Acknowledge alarm
  DELETE /api/alarm/{alarmId}/ack             - Unacknowledge alarm
  POST   /api/alarm/{alarmId}/clear           - Clear alarm
  POST   /api/alarm/{alarmId}/assign          - Assign alarm to user
  DELETE /api/alarm/{alarmId}/assign          - Unassign alarm from user
  
Alarm Comments:
  POST   /api/alarmComment                    - Add comment to alarm
  GET    /api/alarmComments/{alarmId}         - Get alarm comments
  DELETE /api/alarmComment/{commentId}        - Delete alarm comment
```

### 10.4 DASHBOARD ENDPOINTS

```
Dashboard Management:
  POST   /api/dashboard                       - Create dashboard
  GET    /api/dashboard/{dashboardId}         - Get dashboard
  DELETE /api/dashboard/{dashboardId}         - Delete dashboard
  GET    /api/tenant/dashboards               - List tenant dashboards
  GET    /api/customer/{customerId}/dashboards - List customer dashboards
  GET    /api/dashboards                      - List dashboards by IDs
  
Dashboard Sharing:
  POST   /api/dashboard/{dashboardId}/customers/{customerId}    - Share dashboard
  DELETE /api/dashboard/{dashboardId}/customers/{customerId}    - Unshare dashboard
  
Home Dashboard:
  GET    /api/homeDashboard                   - Get home dashboard
  POST   /api/homeDashboard/{dashboardId}     - Set home dashboard
```

### 10.5 USER ENDPOINTS

```
User Management:
  POST   /api/user                            - Create user
  GET    /api/user/{userId}                   - Get user
  DELETE /api/user/{userId}                   - Delete user
  GET    /api/user/current                    - Get current user
  GET    /api/tenant/users                    - List tenant users
  GET    /api/customer/{customerId}/users     - List customer users
  GET    /api/users                           - List users by IDs
  
User Authentication:
  POST   /api/auth/login                      - Login
  POST   /api/auth/logout                     - Logout
  GET    /api/auth/user                       - Get current user
  POST   /api/auth/changePassword             - Change password
  POST   /api/auth/activateUser               - Activate user account
  
User Settings:
  GET    /api/user/{userId}/settings          - Get user settings
  POST   /api/user/{userId}/settings          - Update user settings
  
Two-Factor Auth:
  POST   /api/2fa/provision                   - Provision 2FA
  POST   /api/2fa/verify                      - Verify 2FA code
```

### 10.6 CUSTOMER ENDPOINTS

```
Customer Management:
  POST   /api/customer                        - Create customer
  GET    /api/customer/{customerId}           - Get customer
  DELETE /api/customer/{customerId}           - Delete customer
  GET    /api/customers                       - List all customers (paginated)
  GET    /api/customers/info                  - List customer info (paginated)
```

### 10.7 TENANT ENDPOINTS

```
Tenant Management:
  POST   /api/tenant                          - Create tenant
  GET    /api/tenant/{tenantId}               - Get tenant
  DELETE /api/tenant/{tenantId}               - Delete tenant
  GET    /api/tenants                         - List all tenants (paginated)
  GET    /api/tenantInfos                     - List tenant info (paginated)
```

### 10.8 TELEMETRY ENDPOINTS

```
Attributes:
  GET    /api/telemetry/{entityType}/{entityId}/keys/attributes          - Get attribute keys
  GET    /api/telemetry/{entityType}/{entityId}/values/attributes        - Get all attributes
  GET    /api/telemetry/{entityType}/{entityId}/values/attributes/{scope} - Get scoped attributes
  POST   /api/telemetry/{entityType}/{entityId}/attributes/{scope}       - Save attributes
  DELETE /api/telemetry/{entityType}/{entityId}/{scope}                  - Delete attributes

Timeseries:
  GET    /api/telemetry/{entityType}/{entityId}/keys/timeseries          - Get timeseries keys
  GET    /api/telemetry/{entityType}/{entityId}/values/timeseries        - Get timeseries values
  GET    /api/telemetry/{entityType}/{entityId}/values/timeseries?keys&startTs&endTs - Get range
  POST   /api/telemetry/{entityType}/{entityId}/timeseries/{scope}       - Save timeseries
  POST   /api/telemetry/{entityType}/{entityId}/timeseries/{scope}/{ttl}  - Save with TTL
  DELETE /api/telemetry/{entityType}/{entityId}/timeseries/delete        - Delete timeseries
```

### 10.9 ENTITY RELATION ENDPOINTS

```
Relation Management:
  POST   /api/relation                        - Create relation
  POST   /api/v2/relation                     - Create relation (v2)
  DELETE /api/relation?params                 - Delete relation
  DELETE /api/v2/relation?params              - Delete relation (v2)
  
Relation Queries:
  GET    /api/relations?fromId&fromType&relationTypeId&toId&toType  - Get relation
  GET    /api/relations?fromId&fromType                             - Get from relations
  GET    /api/relations?toId&toType                                 - Get to relations
  GET    /api/relations?fromId&fromType&relationTypeId              - Get typed relations
  GET    /api/relations/info?params                                 - Get relation info
  POST   /api/relations                       - Create multiple relations
  DELETE /api/relations?entityId&entityType   - Delete all relations
```

### 10.10 ENTITY QUERY ENDPOINTS

```
Entity Queries:
  POST   /api/entitiesQuery/find              - Find entities by query
  POST   /api/entitiesQuery/count             - Count entities matching query
  POST   /api/entitiesQuery/find/keys         - Find entity keys
  
Alarm Queries:
  POST   /api/alarmsQuery/find                - Find alarms by query
  POST   /api/alarmsQuery/count               - Count alarms matching query
  
EDQS (Entity Data Query System):
  POST   /api/edqs/system/request             - EDQS system request
  GET    /api/edqs/state                      - Get EDQS state
```

### 10.11 RULE CHAIN ENDPOINTS

```
Rule Chain Management:
  POST   /api/ruleChain                       - Create rule chain
  GET    /api/ruleChain/{ruleChainId}         - Get rule chain
  DELETE /api/ruleChain/{ruleChainId}         - Delete rule chain
  GET    /api/ruleChain/{ruleChainId}/metadata          - Get rule chain metadata
  GET    /api/ruleChain/{ruleChainId}/output/labels    - Get output labels
  GET    /api/ruleChain/{ruleChainId}/output/labels/usage - Get label usage
  POST   /api/ruleChain/{ruleChainId}/root   - Set as root rule chain
  POST   /api/ruleChain/device/default        - Set device default rule chain
  GET    /api/ruleChains                      - List rule chains
```

### 10.12 RPC ENDPOINTS

```
RPC v1:
  POST   /api/rpc/v1/{deviceId}/oneWay/{rpcName}  - One-way RPC
  POST   /api/rpc/v1/{deviceId}/twoWay/{rpcName}  - Two-way RPC

RPC v2:
  POST   /api/rpc/v2/device/{deviceId}/rpcRequest    - Send RPC request
  GET    /api/rpc/v2/device/{deviceId}/rpcResponse/{rpcId} - Get RPC response
```

### 10.13 WIDGET ENDPOINTS

```
Widget Types:
  POST   /api/widgetType                      - Create widget type
  GET    /api/widgetType/{widgetTypeId}       - Get widget type
  DELETE /api/widgetType/{widgetTypeId}       - Delete widget type
  GET    /api/widgetTypes                     - List widget types
  GET    /api/widgetTypesDetails              - List widget type details
  
Widgets Bundle:
  POST   /api/widgetsBundle                   - Create widgets bundle
  GET    /api/widgetsBundle/{bundleId}        - Get widgets bundle
  DELETE /api/widgetsBundle/{bundleId}        - Delete widgets bundle
  GET    /api/widgetsBundles                  - List widget bundles
```

### 10.14 NOTIFICATION ENDPOINTS

```
Notification Management:
  GET    /api/notification                    - Get notifications (paginated)
  POST   /api/notification/{id}/read          - Mark as read
  
Notification Rules:
  POST   /api/notification/rule               - Create rule
  GET    /api/notification/rule/{id}          - Get rule
  DELETE /api/notification/rule/{id}          - Delete rule
  GET    /api/notification/rules              - List rules
  
Notification Targets:
  POST   /api/notification/target             - Create target
  GET    /api/notification/target/{id}        - Get target
  DELETE /api/notification/target/{id}        - Delete target
  
Notification Templates:
  POST   /api/notification/template           - Create template
  GET    /api/notification/template/{id}      - Get template
  DELETE /api/notification/template/{id}      - Delete template
```

### 10.15 SYSTEM ENDPOINTS

```
System Information:
  GET    /api/system/info                     - Get system information
  GET    /api/system/params                   - Get system parameters
  
Admin Settings:
  GET    /api/admin/settings                  - Get admin settings
  POST   /api/admin/settings                  - Update admin settings
  
UI Settings:
  GET    /api/ui/settings                     - Get UI settings
  POST   /api/ui/settings                     - Update UI settings
  
Usage Information:
  GET    /api/usage/info                      - Get usage information
  
Queue Statistics:
  GET    /api/queue/stats                     - Get queue statistics
```

---

## 11. COMPLETE TECHNOLOGY STACK

### 11.1 Core Framework
- **Spring Boot** 3.4.10
- **Spring Framework** (via Spring Boot)
- **Spring Security** with OAuth2
- **Spring Data JPA** with Hibernate

### 11.2 Data Persistence
- **PostgreSQL** (Primary)
- **MySQL** (Supported)
- **H2 Database** (Testing)
- **Cassandra** (NoSQL option)
- **TimescaleDB** (Timeseries)
- **Hibernate** (ORM)
- **JPA** (Java Persistence API)

### 11.3 Message & Queuing
- **Apache Kafka** 3.9.1 (Primary)
- **RabbitMQ** (Alternative)
- **Redis** 5.1.5 (Caching & Sessions)
- **Apache Zookeeper** 3.9.3 (Coordination)

### 11.4 Network & Protocols
- **MQTT** - Paho client 1.2.5
- **CoAP** - Californium 3.12.1
- **LWM2M** - Leshan 2.0.0-M15
- **HTTP/HTTPS** - Apache HttpClient 4.5.14
- **gRPC** 1.68.1 with Protobuf 3.25.5
- **WebSocket** - Java WebSocket 1.5.6
- **SNMP** - SNMP4j 3.8.0

### 11.5 Security & Cryptography
- **JWT (JJWT)** 0.12.5
- **OAuth2** - Nimbus JOSE JWT 10.0.2
- **Bouncy Castle** 1.78.1 (Cryptography)
- **Auth0-jwt** 4.4.0
- **Two-Factor Authentication** (TOTP)

### 11.6 Monitoring & Logging
- **SLF4J** with Logback
- **Micrometer** (Metrics)
- **Metrics-core** 4.2.25
- **Oshi** 6.6.0 (System monitoring)

### 11.7 Caching & Performance
- **Redis/Jedis** 5.1.5
- **RocksDB** 9.10.0 (Local persistence)
- **Bucket4j** 8.10.1 (Rate limiting)
- **Guava** 33.1.0 (Collections & caching)

### 11.8 Integration & Messaging
- **Slack API** 1.39.0
- **Twilio** 10.1.3 (SMS)
- **AWS SDK** 1.12.701
- **Google Cloud PubSub** 1.128.1
- **Firebase Admin** 9.2.0

### 11.9 Scripting & Expression
- **TBEL** 1.2.8 (ThingsBoard Expression Language)
- **Nashorn/Delight Sandbox** 0.4.5 (JavaScript)
- **ANTLR** 3.5.3 (Parser generator)
- **exp4j** 0.4.8 (Math expressions)

### 11.10 AI & Machine Learning
- **LangChain4j** 1.1.0 (AI integration)

### 11.11 Utilities & Libraries
- **Lombok** 1.18.38 (Annotation processing)
- **Commons Lang3** 3.18.0
- **Commons IO** 2.16.1
- **Commons CSV** 1.10.0
- **Jackson** (JSON processing)
- **JSON Schema Validator** 1.5.6
- **JGit** 6.10.1 (Version control)
- **Spatial4j** 0.8 (Geospatial)
- **JTS** 1.19.0 (Geometry)
- **Passay** 1.6.4 (Password validation)

### 11.12 Testing
- **JUnit** 5
- **Mockito** (Mocking)
- **DBUnit** 2.7.3 (Database testing)
- **TestNG** 7.10.1
- **Testcontainers** 1.20.6
- **Spring Test** (Framework)

### 11.13 Build & Deployment
- **Maven** 3
- **Docker** (Containerization)
- **Kubernetes** (Orchestration ready)
- **Linux** (Primary OS)
- **Windows** support via WinSW 2.0.1

---

## SUMMARY STATISTICS

- **Total Java Files in Application**: 905
- **Total Java Files in DAO Layer**: 742
- **Total Entity Classes**: 97
- **SQL Entity Classes**: 66
- **JPA Repositories**: 84
- **REST Controllers**: 59
- **Rule Engine Nodes**: 91
- **DAO/Service Interfaces**: 60+
- **Service Implementations**: 50+
- **Supported Protocols**: 6 (HTTP, MQTT, CoAP, LWM2M, SNMP, gRPC)
- **Database Support**: 4+ (PostgreSQL, MySQL, H2, Cassandra)
- **Message Queue Support**: 2+ (Kafka, RabbitMQ)
- **API Endpoints**: 200+

---

## KEY ARCHITECTURAL PATTERNS

1. **Layered Architecture**
   - Controller → Service → DAO → Repository

2. **Dependency Injection**
   - Spring @Autowired annotations

3. **Object-Relational Mapping**
   - Hibernate/JPA with Spring Data

4. **Message-Driven Architecture**
   - Kafka/RabbitMQ event processing

5. **Microservices Ready**
   - gRPC support for service-to-service communication

6. **Caching Strategy**
   - Redis for distributed caching
   - Local caching with Guava

7. **Security**
   - JWT token-based authentication
   - OAuth2 integration
   - Role-based access control

8. **Rule Processing**
   - Visual rule chain designer
   - 91 pre-built nodes
   - Extensible node architecture

9. **Multi-tenancy**
   - Full tenant isolation
   - Customer hierarchies
   - Resource sharing controls

10. **Real-time Communication**
    - WebSocket for live data
    - MQTT for device communication
    - gRPC for service communication


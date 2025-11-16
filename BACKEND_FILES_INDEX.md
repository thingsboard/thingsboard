# ThingsBoard Java Backend - Complete Files Index

## CRITICAL ENTRY POINTS

### Main Application
- `/application/src/main/java/org/thingsboard/server/ThingsboardServerApplication.java` - Entry point
- `/application/src/main/java/org/thingsboard/server/config/ThingsboardSecurityConfiguration.java` - Security setup
- `/application/src/main/java/org/thingsboard/server/config/WebSocketConfiguration.java` - WebSocket setup

---

## CONTROLLERS (59 Total)

### Device Management
- `/application/src/main/java/org/thingsboard/server/controller/DeviceController.java` (95+ endpoints)
- `/application/src/main/java/org/thingsboard/server/controller/DeviceProfileController.java`

### Asset Management
- `/application/src/main/java/org/thingsboard/server/controller/AssetController.java`
- `/application/src/main/java/org/thingsboard/server/controller/AssetProfileController.java`

### Alarm & Events
- `/application/src/main/java/org/thingsboard/server/controller/AlarmController.java`
- `/application/src/main/java/org/thingsboard/server/controller/AlarmCommentController.java`
- `/application/src/main/java/org/thingsboard/server/controller/EventController.java`

### Dashboard & Visualization
- `/application/src/main/java/org/thingsboard/server/controller/DashboardController.java`
- `/application/src/main/java/org/thingsboard/server/controller/WidgetTypeController.java`
- `/application/src/main/java/org/thingsboard/server/controller/WidgetsBundleController.java`

### Users & Access Control
- `/application/src/main/java/org/thingsboard/server/controller/UserController.java`
- `/application/src/main/java/org/thingsboard/server/controller/CustomerController.java`
- `/application/src/main/java/org/thingsboard/server/controller/TenantController.java`
- `/application/src/main/java/org/thingsboard/server/controller/TenantProfileController.java`

### Authentication & Security
- `/application/src/main/java/org/thingsboard/server/controller/AuthController.java`
- `/application/src/main/java/org/thingsboard/server/controller/OAuth2Controller.java`
- `/application/src/main/java/org/thingsboard/server/controller/OAuth2ConfigTemplateController.java`
- `/application/src/main/java/org/thingsboard/server/controller/TwoFactorAuthController.java`
- `/application/src/main/java/org/thingsboard/server/controller/TwoFactorAuthConfigController.java`

### Data & Telemetry
- `/application/src/main/java/org/thingsboard/server/controller/TelemetryController.java` (Attributes & Timeseries)
- `/application/src/main/java/org/thingsboard/server/controller/EntityQueryController.java` (Advanced queries)
- `/application/src/main/java/org/thingsboard/server/controller/AuditLogController.java`

### Rule Engine & Processing
- `/application/src/main/java/org/thingsboard/server/controller/RuleChainController.java`
- `/application/src/main/java/org/thingsboard/server/controller/RuleEngineController.java`
- `/application/src/main/java/org/thingsboard/server/controller/ComponentDescriptorController.java`

### RPC & Communication
- `/application/src/main/java/org/thingsboard/server/controller/RpcV1Controller.java`
- `/application/src/main/java/org/thingsboard/server/controller/RpcV2Controller.java`

### Edge Computing
- `/application/src/main/java/org/thingsboard/server/controller/EdgeController.java`
- `/application/src/main/java/org/thingsboard/server/controller/EdgeEventController.java`

### Notifications
- `/application/src/main/java/org/thingsboard/server/controller/NotificationController.java`
- `/application/src/main/java/org/thingsboard/server/controller/NotificationRuleController.java`
- `/application/src/main/java/org/thingsboard/server/controller/NotificationTargetController.java`
- `/application/src/main/java/org/thingsboard/server/controller/NotificationTemplateController.java`

### Entity Relations & Views
- `/application/src/main/java/org/thingsboard/server/controller/EntityRelationController.java`
- `/application/src/main/java/org/thingsboard/server/controller/EntityViewController.java`
- `/application/src/main/java/org/thingsboard/server/controller/EntitiesVersionControlController.java`

### System & Configuration
- `/application/src/main/java/org/thingsboard/server/controller/AdminController.java`
- `/application/src/main/java/org/thingsboard/server/controller/SystemInfoController.java`
- `/application/src/main/java/org/thingsboard/server/controller/UiSettingsController.java`
- `/application/src/main/java/org/thingsboard/server/controller/UsageInfoController.java`
- `/application/src/main/java/org/thingsboard/server/controller/AuditLogController.java`

### Additional Features
- `/application/src/main/java/org/thingsboard/server/controller/AiModelController.java` (AI/ML)
- `/application/src/main/java/org/thingsboard/server/controller/QueueController.java` (Message queues)
- `/application/src/main/java/org/thingsboard/server/controller/DomainController.java` (Domain config)
- `/application/src/main/java/org/thingsboard/server/controller/MobileAppController.java`
- `/application/src/main/java/org/thingsboard/server/controller/MobileAppBundleController.java`
- `/application/src/main/java/org/thingsboard/server/controller/TbResourceController.java` (File resources)
- `/application/src/main/java/org/thingsboard/server/controller/CalculatedFieldController.java`
- `/application/src/main/java/org/thingsboard/server/controller/OtaPackageController.java`
- `/application/src/main/java/org/thingsboard/server/controller/JobController.java` (Background jobs)
- `/application/src/main/java/org/thingsboard/server/controller/Lwm2mController.java`
- `/application/src/main/java/org/thingsboard/server/controller/DeviceConnectivityController.java`
- `/application/src/main/java/org/thingsboard/server/controller/TrendzController.java`
- `/application/src/main/java/org/thingsboard/server/controller/QrCodeSettingsController.java`
- `/application/src/main/java/org/thingsboard/server/controller/ImageController.java`
- `/application/src/main/java/org/thingsboard/server/controller/QueueStatsController.java`
- `/application/src/main/java/org/thingsboard/server/controller/AutoCommitController.java`

---

## WEBSOCKET IMPLEMENTATION

- `/application/src/main/java/org/thingsboard/server/service/ws/WebSocketService.java` (Interface)
- `/application/src/main/java/org/thingsboard/server/service/ws/DefaultWebSocketService.java` (Implementation)
- `/application/src/main/java/org/thingsboard/server/service/ws/WebSocketSessionRef.java`
- `/application/src/main/java/org/thingsboard/server/controller/plugin/TbWebSocketHandler.java`
- `/application/src/main/java/org/thingsboard/server/service/ws/telemetry/TelemetryWebSocketTextMsg.java`

---

## ENTITY MODELS (97 Classes)

### Core Entities
- `/common/data/src/main/java/org/thingsboard/server/common/data/Device.java`
- `/common/data/src/main/java/org/thingsboard/server/common/data/Asset.java`
- `/common/data/src/main/java/org/thingsboard/server/common/data/Alarm.java`
- `/common/data/src/main/java/org/thingsboard/server/common/data/Dashboard.java`
- `/common/data/src/main/java/org/thingsboard/server/common/data/User.java`
- `/common/data/src/main/java/org/thingsboard/server/common/data/Customer.java`
- `/common/data/src/main/java/org/thingsboard/server/common/data/Tenant.java`
- `/common/data/src/main/java/org/thingsboard/server/common/data/EntityView.java`

### Security Models
- `/common/data/src/main/java/org/thingsboard/server/common/data/security/DeviceCredentials.java`
- `/common/data/src/main/java/org/thingsboard/server/common/data/security/UserCredentials.java`
- `/common/data/src/main/java/org/thingsboard/server/common/data/security/DeviceTokenCredentials.java`
- `/common/data/src/main/java/org/thingsboard/server/common/data/security/DeviceX509Credentials.java`

### Profile Models
- `/common/data/src/main/java/org/thingsboard/server/common/data/DeviceProfile.java`
- `/common/data/src/main/java/org/thingsboard/server/common/data/asset/AssetProfile.java`
- `/common/data/src/main/java/org/thingsboard/server/common/data/TenantProfile.java`

### Alarm Models
- `/common/data/src/main/java/org/thingsboard/server/common/data/alarm/AlarmComment.java`
- `/common/data/src/main/java/org/thingsboard/server/common/data/alarm/AlarmSeverity.java`
- `/common/data/src/main/java/org/thingsboard/server/common/data/alarm/AlarmStatus.java`

### Rule Engine Models
- `/common/data/src/main/java/org/thingsboard/server/common/data/rule/RuleChain.java`
- `/common/data/src/main/java/org/thingsboard/server/common/data/rule/RuleNode.java`
- `/common/data/src/main/java/org/thingsboard/server/common/data/rule/RuleChainMetaData.java`

---

## SQL ENTITY CLASSES (66 Classes)

Location: `/dao/src/main/java/org/thingsboard/server/dao/model/sql/`

### Key Entity Tables
- `DeviceEntity.java` - Device table mapping
- `DeviceProfileEntity.java` - Device profiles
- `DeviceCredentialsEntity.java` - Device authentication
- `AssetEntity.java` - Assets
- `AssetProfileEntity.java` - Asset profiles
- `AlarmEntity.java` - Alarms
- `AlarmCommentEntity.java` - Alarm comments
- `UserEntity.java` - Users
- `TenantEntity.java` - Tenants
- `CustomerEntity.java` - Customers
- `DashboardEntity.java` - Dashboards
- `EntityViewEntity.java` - Entity views
- `RuleChainEntity.java` - Rule chains
- `RuleNodeEntity.java` - Rule nodes
- `AttributeKvEntity.java` - Attributes
- `RpcEntity.java` - RPC requests/responses
- `EventEntity.java` - Events
- `AuditLogEntity.java` - Audit logs
- `EdgeEntity.java` - Edge devices
- `EdgeEventEntity.java` - Edge events
- `NotificationRuleEntity.java`
- `NotificationTargetEntity.java`
- `NotificationTemplateEntity.java`
- `NotificationRequestInfoEntity.java`
- `OAuth2ClientEntity.java`
- `OtaPackageInfoEntity.java`
- `QueueEntity.java`
- `JobEntity.java`
- `AdminSettingsEntity.java`
- `ApiUsageStateEntity.java`

---

## DAO INTERFACES (60+ Classes)

Location: `/dao/src/main/java/org/thingsboard/server/dao/`

### Core DAOs
- `device/DeviceDao.java` - Device interface
- `asset/AssetDao.java` - Asset interface
- `alarm/AlarmDao.java` - Alarm interface
- `user/UserDao.java` - User interface
- `tenant/TenantDao.java` - Tenant interface
- `customer/CustomerDao.java` - Customer interface
- `dashboard/DashboardDao.java` - Dashboard interface
- `attributes/AttributesDao.java` - Attributes interface
- `timeseries/TimeseriesDao.java` - Timeseries interface
- `rule/RuleChainDao.java` - Rule chain interface
- `rpc/RpcDao.java` - RPC interface
- And 50+ more...

---

## JPA REPOSITORY INTERFACES (84 Classes)

Location: `/dao/src/main/java/org/thingsboard/server/dao/sql/`

### Spring Data Repositories
- `device/DeviceRepository.java`
- `asset/AssetRepository.java`
- `user/UserRepository.java`
- `customer/CustomerRepository.java`
- `tenant/TenantRepository.java`
- `dashboard/DashboardRepository.java`
- `attributes/AttributeKvRepository.java`
- `rule/RuleChainRepository.java`
- `notification/NotificationRuleRepository.java`
- `oauth2/OAuth2ClientRepository.java`
- `queue/QueueRepository.java`
- `edge/EdgeRepository.java`
- And 72+ more...

---

## SERVICE LAYER (55+ Classes)

Location: `/application/src/main/java/org/thingsboard/server/service/`

### Entity Services
- `entitiy/device/TbDeviceService.java`
- `entitiy/device/DefaultTbDeviceService.java`
- `entitiy/asset/TbAssetService.java`
- `entitiy/asset/DefaultTbAssetService.java`
- `entitiy/alarm/TbAlarmService.java`
- `entitiy/alarm/DefaultTbAlarmService.java`
- `entitiy/dashboard/TbDashboardService.java`
- `entitiy/dashboard/DefaultTbDashboardService.java`
- `entitiy/user/TbUserService.java`
- `entitiy/user/DefaultUserService.java`
- And 45+ more entity services...

### Core Services
- `ws/WebSocketService.java` - WebSocket interface
- `ws/DefaultWebSocketService.java` - WebSocket implementation
- `ruleengine/DefaultRuleEngineService.java` - Rule engine
- `notification/DefaultNotificationService.java` - Notifications
- `edge/EdgeRpcService.java` - Edge RPC
- `edqs/DefaultEdqsService.java` - EDQS (Entity Data Query System)
- `ota/DefaultOtaService.java` - OTA updates
- `job/DefaultJobService.java` - Background jobs
- `cf/DefaultCalculatedFieldProcessingService.java` - Calculated fields
- `sms/SmsSenderService.java` - SMS integration

### Authentication & Security
- `security/model/SecurityUser.java` - Current user
- `security/system/SystemSecurityService.java` - System security
- `security/system/DefaultSystemSecurityService.java`

---

## RULE ENGINE (91 Nodes)

Location: `/rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/`

### Filter Nodes (10)
- `filter/TbMsgTypeFilterNode.java`
- `filter/TbOriginatorTypeFilterNode.java`
- `filter/TbDeviceTypeSwitchNode.java`
- `filter/TbAssetTypeSwitchNode.java`
- `filter/TbOriginatorTypeSwitchNode.java`
- `filter/TbMsgTypeSwitchNode.java`
- `filter/TbJsFilterNode.java`
- `filter/TbJsSwitchNode.java`
- `filter/TbCheckMessageNode.java`
- `filter/TbCheckRelationNode.java`
- `filter/TbCheckAlarmStatusNode.java`

### Transform Nodes (7)
- `transform/TbTransformMsgNode.java`
- `transform/TbChangeOriginatorNode.java`
- `transform/TbCopyKeysNode.java`
- `transform/TbRenameKeysNode.java`
- `transform/TbDeleteKeysNode.java`
- `transform/TbJsonPathNode.java`
- `transform/TbSplitArrayMsgNode.java`

### Action Nodes (11)
- `action/TbCreateAlarmNode.java`
- `action/TbClearAlarmNode.java`
- `action/TbDeviceStateNode.java`
- `action/TbLogNode.java`
- `action/TbMsgCountNode.java`
- `action/TbCreateRelationNode.java`
- `action/TbDeleteRelationNode.java`
- `action/TbAssignToCustomerNode.java`
- `action/TbUnassignFromCustomerNode.java`
- `action/TbCopyAttributesToEntityViewNode.java`
- `action/TbSaveToCustomCassandraTableNode.java`

### External Integration Nodes (8)
- `external/TbRestApiCallNode.java`
- `external/TbSendRestApiCallReplyNode.java`
- `kafka/TbKafkaNode.java`
- `rabbitmq/TbRabbitMqNode.java`
- `sms/TbSendSmsNode.java`
- `notification/TbNotificationNode.java`
- `notification/TbSlackNode.java`

### Flow Control Nodes (6)
- `flow/TbRuleChainInputNode.java`
- `flow/TbRuleChainOutputNode.java`
- `flow/TbAckNode.java`
- `flow/TbCheckpointNode.java`
- `transaction/TbSynchronizationBeginNode.java`
- `transaction/TbSynchronizationEndNode.java`

### Other Nodes (49)
- `debug/TbMsgGeneratorNode.java`
- `deduplication/TbMsgDeduplicationNode.java`
- And 47 more specialized nodes...

---

## DATABASE SCHEMA FILES

Location: `/dao/src/main/resources/sql/`

- `schema-entities.sql` - Main entity tables
- `schema-ts-latest-psql.sql` - Timeseries latest (PostgreSQL)
- `schema-ts-psql.sql` - Timeseries data (PostgreSQL)
- `schema-timescale.sql` - TimescaleDB support
- `schema-entities-idx.sql` - Database indexes
- `schema-entities-idx-psql-addon.sql` - PostgreSQL indexes
- `schema-views-and-functions.sql` - Views & functions

---

## TRANSPORT IMPLEMENTATIONS

Location: `/common/transport/` and `/transport/`

### HTTP Transport
- `/common/transport/http/src/main/java/org/thingsboard/server/transport/http/DeviceApiController.java`

### MQTT Transport
- `/transport/mqtt/` - MQTT broker configuration

### CoAP Transport
- `/transport/coap/` - CoAP protocol support

### LWM2M Transport
- `/transport/lwm2m/` - Lightweight M2M support
- Bootstrap server support
- Security configuration

### SNMP Transport
- `/transport/snmp/` - SNMP device integration

---

## SECURITY & AUTHENTICATION

- `/application/src/main/java/org/thingsboard/server/config/ThingsboardSecurityConfiguration.java`
- `/application/src/main/java/org/thingsboard/server/config/TbRuleEngineSecurityConfiguration.java`
- `/application/src/main/java/org/thingsboard/server/service/security/system/SystemSecurityService.java`
- `/application/src/main/java/org/thingsboard/server/service/security/model/SecurityUser.java`
- `/dao/src/main/java/org/thingsboard/server/dao/settings/DefaultSecuritySettingsService.java`
- `/dao/src/main/java/org/thingsboard/server/dao/oauth2/OAuth2ClientServiceImpl.java`
- `/dao/src/main/java/org/thingsboard/server/dao/oauth2/OAuth2ConfigTemplateServiceImpl.java`

---

## CONFIGURATION FILES

- `/application/src/main/java/org/thingsboard/server/config/WebSocketConfiguration.java`
- `/application/src/main/java/org/thingsboard/server/config/ThingsboardSecurityConfiguration.java`
- `/common/transport/http/src/main/java/org/thingsboard/server/transport/http/config/TransportSecurityConfiguration.java`
- `/pom.xml` - Maven configuration with all dependencies

---

## KEY BUILD INFORMATION

- **Version**: 4.3.0-SNAPSHOT
- **Java**: 17+
- **Spring Boot**: 3.4.10
- **Maven Modules**: 19+
- **Total Java Files**: 1600+
- **Total Line Count**: 200,000+


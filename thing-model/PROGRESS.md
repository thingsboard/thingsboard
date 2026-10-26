# thing-model 实施进度

> 对照 `docs/` 设计文档与最初 P0–P5 实施路线整理。
> 更新时间：2026-10-23

## 1. 总体进度

| 阶段 | 状态 | 说明 |
| --- | --- | --- |
| P0 准备 | 部分完成 | 本地 TB 4.3.1.4 + PostgreSQL 18 已运行；缺少厂商开通流程、域名证书、统一入口 |
| P1 物模型 | 核心完成 | TSL 校验、版本存储、压缩 schema、Device Profile/Asset 同步、产品 API 已闭环 |
| P2 接入 | 部分完成 | Kafka 与按产品规则链已生成并绑定；设备批量开通、凭证、RPC、OTA、SDK 尚未实现 |
| P3 台账与事件 | 未开始 | 组织树、项目/工点、绑定、汇聚指标、事件中心未实现 |
| P4 开放与前端 | 部分完成 | 产品后端 API 已有；开放 API、BFF、前端页面未实现 |
| P5 OTA 与试点 | 未开始 | OTA、远程配置、影子、NTP、试点迁移未实现 |

## 2. 按文档章节

| 文档 | 状态 | 已完成 | 未完成 |
| --- | --- | --- | --- |
| 01 概述 | 部分完成 | 目标、租户=厂商、TSL 主线已落地一部分 | 厂商自助接入、业务开放、腾讯迁移整体未完成 |
| 02 总体设计 | 部分完成 | TB + PG 本地运行；独立 thing-model 服务；规则链使用外部 Kafka | TDengine、HAProxy、K8s、ingest-worker、前端未实现 |
| 03 实体模型 | 部分完成 | 租户隔离、Device Profile、产品 Asset、产品/TSL 表 | 组织树、项目/工点、设备绑定、Entity View、存量迁移未实现 |
| 04 物模型 | 基本完成 | TSL 解析、JSON Schema + 业务校验、压缩 schema、版本 DRAFT/PUBLISHED、模板/资产同步 | DEPRECATED、兼容性规则、灰度回滚、规则链生成、模板审计未完成 |
| 05 设备接入 | 未开始 | TB MQTT Transport 已运行 | 设备批量创建、凭证、命名校验、事件上报、RPC、OTA、配置、影子、SDK 未实现 |
| 06 数字孪生与数据加工 | 未开始 | 无 | 组织/工点孪生、汇聚指标、事件中心、TDengine 留存未实现 |
| 07 二次开发 | 部分完成 | `thing-model` 服务、产品/TSL 模块、厂商账号管理 | ingest-worker、registry、metrics、event、openapi、bff 未实现 |
| 08 非功能设计 | 部分完成 | TB JWT、租户隔离、AES-GCM 服务账号 | HTTPS、限流、KMS/Vault、审计、Prometheus、HA、备份未实现 |
| 09 实施路线 | 部分完成 | P1 核心链路已跑通 | P2–P5 未开始 |
| 10 附录 | 部分完成 | TSL 类型映射已用于校验；JSON Schema 已提供 | tsl2tb.py、完整附录产物、CI 校验未实现 |
| 11 前端联动 | 部分完成 | 后端产品/TSL/同步 API 已有 | 前端页面、会话换发、Nginx `/crec-api`、物模型编辑器未实现 |
| 12 遥测单通道 | 部分完成 | Kafka 三 topic 已创建，规则链已投递；Attributes 白名单、TDengine、事件表、事件中心未实现 |
| 13 物模型入湖 | 部分完成 | 规则链自动下发、Kafka topic 已实现 | ingest-worker、TDengine DDL/写入未实现 |

## 3. 已完成能力

- 独立 `thing-model` Maven 工程，JDK 25 / Spring Boot 3.5.15。
- TSL 解析：属性、事件、行为、`profile.ProductId`、`CategoryId`、`version`。
- TSL 校验：
  - JSON Schema 结构校验
  - 标识符、重复、范围、枚举、结构体、功能点数量等业务校验
  - 保留键 `ID_RESERVED` 警告
- 压缩 schema 生成并写入产品 Asset 的 `tsl_schema`。
- 数据表：
  - `plat_product`
  - `plat_product_tsl`
  - `plat_tsl_property`
  - `plat_tsl_event`
  - `plat_tsl_action`
  - `plat_profile_binding`
  - `plat_vendor_account`
- 产品 API：
  - `GET/POST /api/products`
  - `GET /api/products/{productId}`
  - `PUT /api/products/{productId}/thing-model`
  - `POST /api/products/{productId}/thing-model/validate`
  - `GET /api/products/{productId}/catalog`
  - `POST /api/products/{productId}/sync`
- ThingsBoard 同步：
  - Device Profile 幂等创建/更新
  - 产品 Asset 创建/更新
  - `product_id`、`category_id`、`tsl_version`、`tsl_json`、`tsl_schema` 等 SERVER_SCOPE 属性
  - `plat_profile_binding` 绑定记录
- 规则链：
  - 按产品生成 `auto_<ProductId>_telemetry`
  - 绑定 `DeviceProfile.defaultRuleChainId`
  - Kafka 出口：遥测 / 事件 / 校验失败
  - 非遥测消息转发租户根规则链
- Kafka：
  - 使用外部 Kafka，地址由 `thingmodel.rulechain.kafka-bootstrap` / `THINGMODEL_KAFKA_BOOTSTRAP` 配置
  - 规则链投递三个 topic
  - TB 队列保持 in-memory
- 认证与权限：
  - ThingsBoard JWT 校验
  - 租户隔离
  - `@RequireAuthority`
  - `BaseController` / `AccessGuard`
- 厂商服务账号：
  - `/api/admin/tenants/{tenantId}/account`
  - AES-GCM 加密存储
- 数据库：
  - Flyway 迁移
  - `DatabaseGuard` 校验库名/schema
  - 当前使用独立库 `thingmodel`
  - jOOQ `DSLContext` 数据访问，已移除 `JdbcTemplate`
- OpenAPI / Swagger UI。
- Python 数据模拟脚本：`scripts/simulate_telemetry.py`，自动创建并配置测试设备，支持遥测/事件/非法数据混合上报。
- Docker Compose：TB + PostgreSQL，PostgreSQL 固定 `5432` 外部访问；Kafka 使用外部集群。
- 59 个单元/集成测试通过，包含产品创建 → 规则链生成 → Device Profile 绑定 → 数据库回写的完整链路测试。

## 4. 当前运行状态

| 项 | 值 |
| --- | --- |
| thing-model | `mvn spring-boot:run`，端口 `8090`，健康检查 `UP` |
| ThingsBoard | `http://localhost:18080` |
| PostgreSQL | `localhost:5432` |
| thing-model 数据库 | `thingmodel` |
| thing-model 用户 | `thing_model` / `thingmodel` |
| ThingsBoard 数据库 | `thingsboard` |
| 调试产品 | `9HYN9PJNOY`，Device Profile/Asset 已同步 |

## 5. 主要未完成项

1. 设备批量创建、Access Token 下发、设备命名校验。
2. `ingest-worker` 与 TDengine 超级表/子表/DDL。
3. `plat_event` 事件中心、去重、收敛、通知、处置。
4. 组织树、项目/工点、设备绑定、汇聚指标。
5. 前端物模型/台账/事件中心页面。
6. 开放 API `/open/v1/*`、AK/SK、WebSocket。
7. OTA、远程配置、设备影子、NTP。
8. JT/T808、HTTPS、CoAP 接入。
9. K8s、HAProxy、TLS、限流、审计、Prometheus、备份。
10. 真实设备接入联调与试点迁移。

## 6. 与设计文档的偏差

| 项 | 文档 | 当前实现 |
| --- | --- | --- |
| 服务名 | `platform-service` | `thing-model` |
| API 路径 | `/api/crec/products`、`/api/tsl/products` | `/api/products` |
| 数据库 | 图示为 TB 元数据 + `plat_*` 同库 | 当前独立库 `thingmodel`；同库 schema 模式已支持但未启用 |
| TB 版本 | 文档/根 POM 为 4.4.0-SNAPSHOT | 运行镜像为 4.3.1.4，PE client 为 4.4.0 |
| 告警 | 不使用 Alarm | 未实现告警；PE client 模型不含 `alarms`，服务端允许为空 |
| 规则链 | 发布时自动下发 | 已按产品生成并绑定 `defaultRuleChainId` |
| 遥测入湖 | Kafka → ingest → TDengine | 未实现 |

## 7. 建议下一步

优先完成 P2 + 文档 13 的最小闭环：

1. 设备批量创建与凭证下发。
2. 增加 `ingest-worker`，消费 `iot.telemetry.normalized` 并写入 TDengine/PostgreSQL。
3. 建 `plat_event` 与事件中心。
4. 再做前端产品/物模型页面和台账绑定。

当前已能管理产品、物模型、Device Profile 和按产品规则链，但还没有设备数据落库与业务查询。

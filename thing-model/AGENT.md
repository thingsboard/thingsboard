# thing-model AGENT 指南

## 1. 项目定位

`thing-model` 是独立的产品与物模型服务，不属于 ThingsBoard 根 Maven reactor。

- 通过 `thingsboard-pe-client` 调用 ThingsBoard REST API。
- 不修改 ThingsBoard 源码。
- 负责产品、TSL、Device Profile、产品 Asset、规则链和厂商服务账号。
- 默认端口 `8090`，ThingsBoard 默认地址 `http://localhost:18080`。

## 2. 技术栈

- JDK 25
- Maven 3.9+
- Spring Boot 3.5.15
- PostgreSQL 14+
- jOOQ + Flyway
- ThingsBoard PE Client 4.4.0，运行时可能是 ThingsBoard 4.3.1.4
- 测试：JUnit 5、Mockito、Testcontainers

## 3. 常用命令

```powershell
# 全量测试（包含外部 ThingsBoard 集成测试）
mvn -f thing-model/pom.xml test

# 只跑单元/Web Slice 测试，不需要 Docker 和 ThingsBoard
mvn -f thing-model/pom.xml test "-Dtest=!*IntegrationTest"

# 只跑数据库集成测试
mvn -f thing-model/pom.xml test -Dtest=RepositoryIntegrationTest

# 只跑外部 ThingsBoard 集成测试
mvn -f thing-model/pom.xml test -Dtest=ProductRuleChainTbIntegrationTest

# 编译
mvn -f thing-model/pom.xml -DskipTests compile

# 启动
mvn -f thing-model/pom.xml spring-boot:run
```

启动后：

```text
http://localhost:8090/actuator/health
http://localhost:8090/swagger-ui.html
```

## 4. 目录结构

```text
thing-model/
├── src/main/java/com/iot/thingmodel/
│   ├── admin/      厂商服务账号、AES-GCM 加密
│   ├── auth/       JWT 校验、权限、租户隔离
│   ├── common/     常量、错误、异常处理
│   ├── config/     Properties、DatabaseGuard、Flyway、OpenAPI
│   ├── controller/ Product、Admin、Auth、BaseController
│   ├── db/         jOOQ JSONB/时间编解码
│   ├── product/    产品服务、TSL 持久化、目录、绑定
│   ├── sync/       Device Profile、Asset、规则链同步
│   ├── tb/         ThingsBoard Client、异常转换
│   └── tsl/        TSL 解析、校验、压缩 schema
├── src/main/resources/
│   ├── db/migration/              Flyway V1/V2
│   ├── rulechain/telemetry_to_kafka.js
│   ├── rulechain/apply_envelope.js
│   └── schemas/tsl-v1.5.schema.json
├── src/test/java/com/iot/thingmodel/
│   ├── support/    测试支撑类，不放业务测试
│   ├── db/         RepositoryIntegrationTest
│   ├── product/    ProductServiceTest、ProductRuleChainTbIntegrationTest
│   └── ...         其余单元测试与业务包同目录
├── scripts/        Python 模拟脚本、Node 规则链脚本测试
├── deploy/         PostgreSQL 初始化 SQL
├── README.md
├── PROGRESS.md
└── REVIEW.md
```

## 5. 核心流程

### 产品创建/发布

1. 校验 JWT、租户与权限。
2. 解析 TSL。
3. JSON Schema 结构校验 + 业务校验。
4. 写入 `plat_product`、`plat_product_tsl`（DRAFT）。
5. 获取该租户的 ThingsBoard 厂商服务账号。
6. 生成/更新规则链，并绑定到 `DeviceProfile.defaultRuleChainId`。
7. 创建/更新 Device Profile、产品 Asset，写入 `tsl_json`、`tsl_schema` 等 SERVER_SCOPE 属性。
8. 写入 `plat_profile_binding`，TSL 置为 PUBLISHED。

同步失败时保留 DRAFT，接口返回 `502`，可调用：

```http
POST /api/products/{productId}/sync
```

### TSL 校验

- `TslValidator`：JSON Schema + 业务校验。
- `TslParser`：解析 properties、events、actions、profile、version。
- `CompactSchema`：生成运行时压缩 schema，包含：
  - `schemaVersion`
  - `productId`
  - `tslVersion`
  - `strictUnknown`
  - `keys`：属性类型、范围、枚举、required
  - `events`：事件类型与参数定义

## 6. 规则链设计

规则链名称直接使用 ProductId，例如：

```text
9HYN9PJNOY
```

旧规则链 `auto_<ProductId>_telemetry` 会在下次发布/同步时复用并重命名为 ProductId。

当前拓扑：

```text
Message Type Switch
├─ Post telemetry → Get TSL Attributes → TSL Validate → Route By Validation
│  ├─ valid   → Save Timeseries
│  └─ invalid → Normalize Envelope
├─ Post attributes → Save Client Attributes
├─ RPC Request from Device → Log RPC from Device
├─ RPC Request to Device → RPC Call Request
└─ Other → Log Other

Save Timeseries → Normalize Envelope → Route By Kind
Route By Kind → Kafka Telemetry / Kafka Event / Kafka Invalid
```

关键约束：

- 共 14 个节点、14 条连接。
- `Route By Validation` 只输出 `valid` / `invalid`。
- 校验使用生成时内嵌的压缩 TSL schema，不要求设备存在 `tsl_schema`。
- 合法遥测/事件写入 ThingsBoard 时序库。
- 非法数据不写时序库，只投递 `iot.telemetry.invalid`。
- 运行时校验覆盖：
  - 报文结构、大小、时间戳单位
  - 属性类型、范围、required、未知字段、枚举、int32、struct
  - 事件 id 必须属于 TSL `events[].id`，支持 `event_id`，兼容 `id`（`event_id` 优先）
  - `event_type` 与 TSL 一致
  - 事件参数类型与范围
- 归一化事件固定为 `id/type/ts/data`；`event.id` 即 TSL `events[].id`，不使用随机 id，也不输出 `code`；`level` 由 ingest/event-center 按 `type` 映射。
- 支持 `msg.events` 事件数组：归一化后信封使用 `events` 数组，一条消息可包含多个事件；同一包内 `event_id` 必须一致，否则标记 `events:mixed`；单事件仍使用 `event` 对象。

## 7. 数据与配置

默认使用独立数据库 `thingmodel`：

```text
PostgreSQL:5432
├── thingsboard
└── thingmodel
```

也支持同库 + `thingmodel` schema。`DatabaseGuard` 会校验库名/schema，拒绝误连 `thingsboard`。

主要环境变量：

| 变量 | 默认值 |
| --- | --- |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/thingmodel` |
| `SPRING_DATASOURCE_USERNAME` | `postgres` |
| `SPRING_DATASOURCE_PASSWORD` | `postgres` |
| `THINGMODEL_DB_NAME` | `thingmodel` |
| `THINGMODEL_DB_SCHEMA` | 空 |
| `THINGMODEL_TB_URL` | `http://localhost:18080` |
| `THINGMODEL_SECRET_KEY` | 空，厂商账号加密必需 |
| `THINGMODEL_KAFKA_BOOTSTRAP` | `kafka-test1:9095,kafka-test2:9095,kafka-test3:9095` |
| `THINGMODEL_RULECHAIN_ENABLED` | `true` |

`THINGMODEL_SECRET_KEY` 必须是 Base64 编码的 16/24/32 字节 AES 密钥。更换该密钥后，必须在 ThingsBoard 租户下重新注册厂商服务账号，否则同步会因无法解密密码而失败。

## 8. 认证与权限

- 所有 `/api/**` 使用 ThingsBoard JWT：
  ```http
  X-Authorization: Bearer <TB_JWT>
  ```
- `AuthInterceptor` 校验 token，`@RequireAuthority` 声明角色。
- `AccessGuard` 负责租户写入解析和租户隔离。
- Controller 继承 `BaseController`。
- `TENANT_ADMIN` 只能操作本租户；`SYS_ADMIN` 创建产品必须显式传 `tenantId`。
- 厂商服务账号通过：
  ```http
  POST /api/admin/tenants/{tenantId}/account
  ```
  仅 `SYS_ADMIN` 可调用，密码 AES-GCM 加密存储。

## 9. 测试规范

### 分层

- 单元测试 / Web Slice：不需要 Docker 或 ThingsBoard。
- 数据库集成测试：`db.RepositoryIntegrationTest`，使用 Testcontainers Postgres。
- 外部集成测试：`product.ProductRuleChainTbIntegrationTest`，使用 Testcontainers Postgres + 现有 ThingsBoard。

**不要重新引入独立的 `integration` 包，也不要恢复 `@Tag("integration")` / `@Tag("external")`。** 集成测试与业务代码同包。

### 测试支撑类

```text
com.iot.thingmodel.support/
├── TslFixtures.java
├── TbClientStubs.java
├── RuleChainAssertions.java
└── TbRestTestClient.java
```

### 外部 ThingsBoard 集成测试

- 默认账号：
  - `tenant@thingsboard.org / tenant`
  - `sysadmin@thingsboard.org / sysadmin`
- 可通过 `TB_URL`、`TB_USERNAME`、`TB_PASSWORD`、`TB_SYSADMIN_USERNAME`、`TB_SYSADMIN_PASSWORD` 覆盖。
- 固定 ProductId：`E2E_RULE_CHAIN`。
- 测试开始会删除旧的 E2E Device Profile 和 Rule Chain，测试结束**默认保留**新的 Device Profile、Asset、Rule Chain。
- 测试会打印：
  ```text
  Retained E2E product=... ruleChain=... profile=... asset=...
  ```
- 产品记录位于临时 Testcontainers Postgres，测试结束即销毁；UI 产品页不会显示该 E2E 产品。
- 测试要求本地 ThingsBoard 已启动，Docker 可用。
- 当前测试用例总数：79。

### 规则链脚本测试

```powershell
node thing-model/scripts/test_rulechain_validation.js
```

不依赖 ThingsBoard，验证合法/非法遥测、事件、枚举、int32、报文大小、时间戳等。

### 注意

- 运行 `mvn clean` 前必须先停止正在运行的 thing-model，否则 `target/classes` 被占用会导致清理失败。
- 外部集成测试会修改 ThingsBoard 中的 E2E 实体，但默认保留现场。

## 10. 开发约定

- 保持独立服务，不修改 ThingsBoard 源码。
- 数据库访问使用 jOOQ，不再使用 `JdbcTemplate`。
- 新增 TSL 运行时校验时，同时更新：
  - `CompactSchema`
  - `telemetry_to_kafka.js`
  - `TbRuleChainProvisionerCreationTest`
  - `RuleChainAssertions`
- 修改规则链拓扑时，同时更新：
  - `Constants.RuleChain`
  - `TbRuleChainProvisioner`
  - `TbRuleChainProvisionerCreationTest`
  - `RuleChainAssertions.EXPECTED_CONNECTIONS`
- 不要添加无意义注释；保持现有代码风格。
- 文档使用中文，与 README/PROGRESS/REVIEW 保持一致。

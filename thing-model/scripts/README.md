# thing-model 数据模拟脚本

用于向 ThingsBoard 模拟上报物模型数据，验证 Device Profile、规则链和 Kafka 出口。

## 环境要求

- Python 3.9+
- thing-model 已启动
- ThingsBoard 已启动
- 产品已同步；规则链由产品发布/同步自动生成
- 产品 Asset 上存在 `tsl_schema`（模拟脚本会复制到测试设备，便于兼容旧行为）
- ThingsBoard 账号具备 `TENANT_ADMIN` 权限

安装依赖：

```bash
python -m pip install -r thing-model/scripts/requirements.txt
```

## 基本用法

```bash
python thing-model/scripts/simulate_telemetry.py
```

默认行为：

- 登录 ThingsBoard
- 读取产品 `9HYN9PJNOY` 的物模型目录
- 创建临时测试设备并绑定产品 Device Profile
- 写入 `tsl_schema`、`tsl_version`、`tenant_id`、`device_uuid`、`site_code`、`product_id`
- 按 `mixed` 模式循环发送：
  - 属性遥测
  - 事件
- 退出时删除临时设备

### 按本地 TSL 生成 4DZDCQUA2D 数据

```powershell
python thing-model/scripts/simulate_telemetry.py `
  --product-id 4DZDCQUA2D `
  --tsl thing-model/migrate/4DZDCQUA2D.tsl.json `
  --mode mixed --count 2 --seed 1
```

指定 `--tsl` 后，属性/事件定义以本地 TSL 为准：

- struct 属性按 `define.specs` 生成扁平 JSON 对象。
- 事件 payload 仅包含事件 params 以及 `event_id/event_type/event_ts`，不包含属性数据。
- 发送真实遥测要求产品已同步（`tbProfileId`、`tbAssetId`、`tsl_schema`）。
- actions 不模拟，因为行为是平台到设备的 RPC，不是遥测上报。

## 注册设备

`register_devices.py` 按产品 Device Profile 注册设备，写入设备 server 属性，并把 Access Token 导出到 CSV。

单个设备：

```powershell
python thing-model/scripts/register_devices.py `
  --product-id 4DZDCQUA2D `
  --name 4DZDCQUA2D-0001 `
  --output device-credentials.csv `
  --password tenant
```

批量生成：

```powershell
python thing-model/scripts/register_devices.py `
  --product-id 4DZDCQUA2D `
  --prefix 4DZDCQUA2D --count 10 `
  --site-code SIM `
  --output device-credentials.csv `
  --password tenant
```

说明：

- 设备名通过 `--name` 指定（可重复），或通过 `--prefix` + `--count` 批量生成，两种方式二选一。
- 已存在的同名设备默认跳过，并在 CSV 中标记 `skipped`，脚本可重复执行。
- 产品必须已同步，缺少 `tbProfileId`、`tbAssetId`、`tenantId` 时脚本直接报错。
- 每个新设备会写入 `tsl_schema`、`tsl_version`、`tenant_id`、`device_uuid`、`site_code`、`product_id`。

CSV 列：

```text
product_id, device_name, device_id, access_token, tsl_version,
profile_id, site_code, status, error, created_at
```

> CSV 包含完整 Access Token，请妥善保管，不要提交到代码仓库。

### 向注册设备批量发送

使用注册脚本生成的 CSV，按本地 TSL 生成数据并批量发送：

```powershell
python thing-model/scripts/simulate_telemetry.py `
  --product-id 4DZDCQUA2D `
  --tsl thing-model/migrate/4DZDCQUA2D.tsl.json `
  --devices-csv device-credentials-4DZDCQUA2D.csv `
  --mode mixed --count 2 --workers 10 --interval 0 `
  --password tenant
```

说明：

- 只处理 CSV 中 `access_token` 非空的设备。
- `--count` 是每台设备发送的消息数；`mixed` 按 telemetry/event 循环。
- `--workers` 控制并发设备数。
- 批量模式不会创建或删除设备，也不要求 `tbProfileId/tbAssetId/tsl_schema`。

## 密码

推荐使用环境变量：

```powershell
$env:TB_PASSWORD = "tenant"
python thing-model/scripts/simulate_telemetry.py --count 10 --interval 2
```

未设置 `TB_PASSWORD` 时脚本会交互式提示输入密码。

## 常用示例

只发送正常遥测：

```bash
python thing-model/scripts/simulate_telemetry.py --mode telemetry --count 20
```

只发送事件：

```bash
python thing-model/scripts/simulate_telemetry.py --mode event --count 5
```

只发送非法数据：

```bash
python thing-model/scripts/simulate_telemetry.py --mode invalid --count 5
```

发送 10 条混合消息，间隔 1 秒：

```bash
python thing-model/scripts/simulate_telemetry.py --mode mixed --count 10 --interval 1
```

保留测试设备：

```bash
python thing-model/scripts/simulate_telemetry.py --keep-device
```

指定产品和设备名：

```bash
python thing-model/scripts/simulate_telemetry.py \
  --product-id 9HYN9PJNOY \
  --device-name my-sim-device \
  --count 5
```

## 参数

| 参数 | 默认值 | 说明 |
| --- | --- | --- |
| `--tb-url` | `http://localhost:18080` | ThingsBoard 地址 |
| `--thingmodel-url` | `http://localhost:8090` | thing-model 地址 |
| `--username` | `tenant@thingsboard.org` | ThingsBoard 租户管理员 |
| `--password` | `TB_PASSWORD` | ThingsBoard 密码 |
| `--product-id` | `9HYN9PJNOY` | 产品 ID |
| `--tsl` | 空 | 本地 TSL JSON 文件；指定后按其中的属性和事件生成数据 |
| `--device-name` | `sim-<product>-<timestamp>-<random>` | 测试设备名 |
| `--site-code` | `SIM` | 设备 `site_code` 属性 |
| `--mode` | `mixed` | `telemetry` / `event` / `invalid` / `mixed`；`mixed` = telemetry + event |
| `--interval` | `5` | 消息间隔秒数 |
| `--count` | `0` | 消息数量；单设备模式 0 表示无限，批量模式必须 > 0 |
| `--devices-csv` | 空 | 注册脚本输出的 CSV；指定后向 CSV 中的设备批量发送 |
| `--workers` | `10` | 批量发送并发数 |
| `--seed` | 空 | 随机种子 |
| `--keep-device` | 关闭 | 退出时保留设备 |
| `--timeout` | `10` | HTTP 超时秒数 |

## 数据生成规则

- `int`：在 `min/max` 范围内随机
- `float`：在 `min/max` 范围内随机
- `bool`：随机布尔值
- `string`：生成 `sim-<random>`，`bssid` 生成 MAC 地址
- `enum` / `enum_string`：从 mapping 中选择键；`enum` 转为整数，`enum_string` 保留字符串
- `timestamp`：当前时间
- `struct`：按 `define.specs` 生成扁平 JSON 对象，跳过嵌套 struct
- 事件：仅包含事件 params 以及 `event_id/event_type/event_ts`，不包含属性数据；`event_id` 直接使用 TSL `events[].id`，产品未定义事件时生成 `evt-<random>`
- `mixed`：属性/事件交替发送；`invalid` 为独立模式，仅发送属性类非法数据
- actions：不模拟（行为是平台到设备的 RPC，不是遥测上报）

## Kafka 验证

正常遥测：

```text
iot.telemetry.normalized
```

事件：

```text
iot.event.normalized
```

非法数据：

```text
iot.telemetry.invalid
```

如果使用外部 Kafka，可在能访问 Kafka 的机器上消费：

```bash
kcat -b kafka-test1:9095 -t iot.telemetry.normalized -C -o beginning -e
```

## 规则链脚本测试

不需要启动 ThingsBoard 即可验证校验脚本逻辑：

```bash
node thing-model/scripts/test_rulechain_validation.js
```

覆盖合法遥测、类型错误、越界、int32、报文超限、未知字段、枚举、时间戳归一、合法/未知事件、事件类型不匹配等情况。

## 注意

- 默认会创建并删除测试设备；使用 `--keep-device` 可保留。
- 脚本不会打印完整 Access Token。
- 产品 Asset 上没有 `tsl_schema` 时，脚本会提示先执行产品同步。
- 规则链校验使用生成时内嵌的产品 TSL schema，不再要求设备侧存在 `tsl_schema`；脚本仍写入该属性以兼容旧行为。

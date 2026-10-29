# 物联网设备接入对接文档

> 中铁信科物联网平台 · 外部设备 MQTT 接入说明  
> 文档版本：1.0  
> 更新时间：2026-10-29  
> 适用环境：当前测试环境（ThingsBoard 4.x）

---

## 目录

1. [文档说明](#1-文档说明)
2. [接入概览](#2-接入概览)
3. [连接参数与认证](#3-连接参数与认证)
4. [主题与报文约定](#4-主题与报文约定)
5. [属性/遥测上报](#5-属性遥测上报)
6. [事件上报](#6-事件上报)
7. [行为调用与 RPC](#7-行为调用与-rpc)
8. [平台校验与错误处理](#8-平台校验与错误处理)
9. [快速接入示例](#9-快速接入示例)
10. [附录 A：4DZDCQUA2D 物模型](#附录-a4dzdcqua2d-物模型)
11. [附录 B：与参考文档的差异](#附录-b与参考文档的差异)
12. [变更记录](#12-变更记录)

---

## 1. 文档说明

### 1.1 适用对象

本文面向需要将设备接入中铁信科物联网平台的外部设备厂商和集成方，说明设备侧需要实现的 MQTT 连接、认证、主题、属性上报、事件上报和行为响应。

### 1.2 接入方式

设备**直连平台内置的 ThingsBoard MQTT Transport**，不经过外部 Broker，也不需要在平台侧做协议转换。

```text
设备 → MQTT (1883) → ThingsBoard MQTT Transport → 物模型校验 → 平台数据
```

### 1.3 当前测试环境

| 项 | 值 |
| --- | --- |
| MQTT 地址 | `localhost:1883` |
| MQTT 协议 | MQTT 3.1.1，兼容 MQTT 5.0 |
| TLS 端口 | `8883`，当前测试环境尚未启用，待平台运维配置 |
| 测试产品 | `4DZDCQUA2D` |
| 产品分类 | `1` |
| TSL 版本 | `1.1` |

> `localhost:1883` 只适合在平台所在机器上测试。外部设备接入时，必须替换为平台提供的外网/局域网可达地址。

### 1.4 术语

| 术语 | 说明 |
| --- | --- |
| ProductId | 产品标识，对应平台 Device Profile，例如 `4DZDCQUA2D` |
| DeviceName | 设备名称，对应平台 `device.name`，建议不超过 48 字符，仅英文、数字、下划线 |
| Access Token | 设备接入凭证，一机一密；MQTT 连接时作为用户名使用 |
| TSL | 物模型定义，包含属性、事件和行为 |
| 属性 | 设备当前状态，上报后进入平台遥测 |
| 事件 | 设备发生的记录，例如加油、工作、油耗、告警 |
| 行为 | 平台下发到设备的 RPC 操作 |

---

## 2. 接入概览

### 2.1 接入流程

1. 平台管理员创建产品与设备，生成设备 Access Token。
2. 设备使用 Access Token 连接平台 MQTT。
3. 设备向 `v1/devices/me/telemetry` 上报属性或事件。
4. 平台按产品 TSL 校验数据。
5. 平台向设备下发行为/RPC 时，设备订阅 RPC 主题并返回结果。

### 2.2 设备预创建

平台采用**设备预创建**方式，不开放设备自动注册。设备必须先由平台创建并绑定到对应 Device Profile，设备侧才能使用发放的 Access Token 连接。

### 2.3 一机一密

- 每台设备使用独立的 Access Token。
- 不要多台设备共用同一个 Token。
- Token 泄露后应立即在平台重置，旧 Token 失效。
- 设备侧不要将 Token 写入日志或公开仓库。

---

## 3. 连接参数与认证

### 3.1 连接参数

| 参数 | 建议值 | 说明 |
| --- | --- | --- |
| Server URI | `tcp://localhost:1883` | 当前测试地址；外部设备替换为平台可达地址 |
| MQTT 版本 | 3.1.1 | 推荐；MQTT 5.0 兼容 |
| ClientId | `DeviceName` 或 `DeviceName-<随机后缀>` | 必须全局唯一 |
| Username | Access Token | 设备凭证 |
| Password | 空字符串 | Access Token 模式不需要密码 |
| Keep Alive | 60 秒 | 可按网络情况调整 |
| Clean Session | `true` | 设备本地缓存断网数据 |
| QoS | `1` | 至少一次 |
| 最大重连间隔 | 30 秒 | 建议指数退避 |
| 单条遥测 payload | 小于 4 KB | 平台会校验 payload 大小 |

### 3.2 认证方式

平台当前使用 **Access Token** 认证：

```text
MQTT Username = <设备 Access Token>
MQTT Password = 空字符串
```

示例（伪代码）：

```text
host     = localhost
port     = 1883
clientId = 4DZDCQUA2D-0001
username = <ACCESS_TOKEN>
password = ""
```

### 3.3 TLS

生产环境建议使用 MQTT over TLS（8883）。当前测试环境 `8883` 未启用 TLS，请以平台运维提供的证书和地址为准。

启用 TLS 后，设备需要：

- 使用平台提供的 CA 证书校验服务端；
- 使用 `ssl://` 或 `mqtts://` 连接；
- 不要把证书校验关闭后用于生产环境。

---

## 4. 主题与报文约定

### 4.1 主题

| 主题 | 方向 | 用途 |
| --- | --- | --- |
| `v1/devices/me/telemetry` | 上行 | 属性上报、事件上报 |
| `v1/devices/me/attributes` | 上行/下行 | 客户端属性、共享属性（可选） |
| `v1/devices/me/attributes/request/{requestId}` | 上行 | 设备请求服务端属性（可选） |
| `v1/devices/me/attributes/response/{requestId}` | 下行 | 平台返回属性请求结果（可选） |
| `v1/devices/me/rpc/request/{requestId}` | 下行 | 平台下发行为/控制 |
| `v1/devices/me/rpc/response/{requestId}` | 上行 | 设备返回行为/控制结果 |

> 网关型设备如需代子设备上报，可使用 `v1/gateway/*` 主题。本文以直连设备为主。

### 4.2 报文格式

- 编码：UTF-8 JSON。
- 时间戳：毫秒（ms）。
- 属性与事件都通过遥测主题上报。
- 遥测支持两种写法：

**写法一：扁平对象**

```json
{
  "imei": "860000000000001",
  "signalStrength": -65
}
```

**写法二：带时间戳和 values**

```json
{
  "ts": 1793240957000,
  "values": {
    "imei": "860000000000001",
    "signalStrength": -65
  }
}
```

推荐使用写法二，便于明确数据产生时间。

### 4.3 类型约定

| TSL 类型 | JSON 类型 | 说明 |
| --- | --- | --- |
| `int` | number（整数） | 32 位有符号整数 |
| `float` | number | 小数 |
| `bool` | boolean 或 0/1 | 平台会归一化 |
| `string` | string | UTF-8 字符串 |
| `enum` | number | 使用 mapping 中的数字键 |
| `enum_string` | string | 使用 mapping 中的字符串键 |
| `timestamp` | number | 毫秒时间戳 |
| `struct` | object | 不支持嵌套 struct |

---

## 5. 属性/遥测上报

### 5.1 基本规则

- 属性键名必须使用 TSL `properties[].id`。
- `struct` 属性使用嵌套 JSON 对象。
- 标量属性直接作为 JSON 字段上报。
- 支持只上报部分属性；未上报的属性不会更新。
- 建议每次上报完整的 `deviceInfo`、`locationInfo`、`realStateInfo`，避免平台侧状态不完整。

### 5.2 完整属性示例

```json
{
  "ts": 1793240957000,
  "values": {
    "deviceInfo": {
      "company": "三一重工",
      "name": "挖掘机-001",
      "model": "SY215C",
      "sn": "SN20220601001",
      "battery": 85,
      "updateTs": 1793240957000,
      "category": "挖掘机",
      "source": "自有",
      "managementNumber": "GL-2022-001",
      "onlineState": 2
    },
    "locationInfo": {
      "longitude": 113.264385,
      "latitude": 23.129163,
      "updateTs": 1793240957000,
      "address": "广东省广州市天河区XXX路",
      "altitude": 12.5
    },
    "realStateInfo": {
      "machineState": 0,
      "updateTs": 1793240957000,
      "speed": 15.5,
      "todayMileage": 12000,
      "isWork": true,
      "workTime": 28800,
      "idleTime": 3600,
      "fuelConsumptionVolume": 45.5
    },
    "imei": "860000000000001",
    "iccid": "89860000000000000001",
    "firmwareVersion": "1.0.0",
    "signalStrength": -65,
    "reportInterval": 60,
    "totalMileage": 123456
  }
}
```

### 5.3 属性说明

完整字段定义见 [附录 A](#附录-a4dzdcqua2d-物模型)。

| 属性 | 类型 | 说明 |
| --- | --- | --- |
| `deviceInfo` | struct | 设备基本信息 |
| `locationInfo` | struct | 位置信息 |
| `realStateInfo` | struct | 实时运行状态 |
| `imei` | string | 设备 IMEI |
| `iccid` | string | SIM 卡号 |
| `firmwareVersion` | string | 固件版本 |
| `signalStrength` | int | 信号强度，-120 ~ 0 dBm |
| `reportInterval` | int | 上报周期，10 ~ 3600 秒，读写 |
| `totalMileage` | int | 累计里程，米 |

---

## 6. 事件上报

### 6.1 基本规则

- 事件与属性使用同一个主题：`v1/devices/me/telemetry`。
- 事件放在 `events` 数组中。
- **同一包内 `event_id` 必须一致**；混合不同事件会被平台判为非法。
- 建议每包 1~4 个事件，避免 payload 超过 4 KB。
- 每个事件对象使用扁平字段，不要额外嵌套 `params` 对象。
- 事件参数与 `event_id`、`event_type`、`event_ts` 同级。
- `event_ts` 使用毫秒时间戳，建议同一包内每个事件使用不同的 `event_ts`。

### 6.2 事件数组示例：设备告警

```json
{
  "events": [
    {
      "event_id": "alarm",
      "event_type": "alert",
      "event_ts": 1793240957000,
      "address": "广东省广州市天河区XXX路",
      "longitude": 113.264385,
      "latitude": 23.129163,
      "alarmAt": 1793240957000,
      "alarmType": "oil_abnormal",
      "remark": "油量异常下降，疑似漏油"
    },
    {
      "event_id": "alarm",
      "event_type": "alert",
      "event_ts": 1793240957100,
      "address": "广东省广州市天河区XXX路",
      "longitude": 113.264385,
      "latitude": 23.129163,
      "alarmAt": 1793240957100,
      "alarmType": "low_power",
      "remark": "设备电量低"
    }
  ]
}
```

### 6.3 事件数组示例：加油记录

```json
{
  "events": [
    {
      "event_id": "refuelRecord",
      "event_type": "info",
      "event_ts": 1793240957000,
      "volume": 120.5,
      "percentage": 45.0,
      "startTs": 1793240557000,
      "endTs": 1793240957000,
      "tankId": 1
    }
  ]
}
```

### 6.4 单事件兼容格式

平台也支持一次只上报一个事件对象，此时不需要 `events` 数组：

```json
{
  "event_id": "refuelRecord",
  "event_type": "info",
  "event_ts": 1793240957000,
  "volume": 120.5,
  "percentage": 45.0,
  "startTs": 1793240557000,
  "endTs": 1793240957000,
  "tankId": 1
}
```

新接入设备建议统一使用 `events` 数组格式。

### 6.5 事件字段

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `event_id` | string | 是 | 事件标识，必须属于当前产品 TSL `events[].id` |
| `event_type` | string | 建议 | `info` / `alert` / `fault`；省略时使用 TSL 默认值 |
| `event_ts` | number | 建议 | 事件发生时间，毫秒；省略时使用平台接收时间 |
| 其他字段 | - | 否 | 事件参数，字段名和类型由 TSL 定义 |

### 6.6 事件类型与平台级别

消息中不传输 `level`，平台按下表在后续处理中映射：

| `event_type` | 平台级别 | 说明 |
| --- | --- | --- |
| `info` | `INFO` | 只进入事件时间线，不触发通知 |
| `alert` | `MAJOR` | 进入事件中心，按规则通知 |
| `fault` | `CRITICAL` | 进入事件中心，严重告警通知 |

### 6.7 事件参数

完整字段定义见 [附录 A](#附录-a4dzdcqua2d-物模型)。

| 事件 | 类型 | 参数 |
| --- | --- | --- |
| `refuelRecord` | info | `volume`、`percentage`、`startTs`、`endTs`、`tankId` |
| `workRecord` | info | `state`、`startTs`、`endTs`、`mileage` |
| `fuelRecord` | info | `volume`、`percentage`、`presentTs`、`pressure`、`tankId` |
| `alarm` | alert | `address`、`longitude`、`latitude`、`alarmAt`、`alarmType`、`remark` |

---

## 7. 行为调用与 RPC

### 7.1 RPC 主题

平台向设备下发：

```text
v1/devices/me/rpc/request/{requestId}
```

设备返回：

```text
v1/devices/me/rpc/response/{requestId}
```

`requestId` 由平台生成，设备返回时必须使用相同的 `requestId`。

### 7.2 请求与响应格式

平台请求：

```json
{
  "method": "setReportInterval",
  "params": {
    "interval": 60
  }
}
```

设备响应：

```json
{
  "success": true
}
```

### 7.3 行为列表

| 行为 | method | 请求参数 | 响应 |
| --- | --- | --- | --- |
| 重启设备 | `reboot` | 无 | `{"success": true}` |
| 设置上报周期 | `setReportInterval` | `{"interval": 10~3600}` | `{"success": true}` |
| 查询当前位置 | `queryLocation` | 无 | `{"longitude":..., "latitude":..., "address":"...", "updateTs":...}` |
| 锁车/解锁 | `lockMachine` | `{"locked": true/false}` | `{"success": true}` |

### 7.4 读写属性

`reportInterval` 是 `rw` 属性，平台也可能通过通用属性设置下发：

```json
{
  "method": "set",
  "params": {
    "reportInterval": 60
  }
}
```

设备应同时兼容 `set` 和 `setReportInterval` 两种方式。

### 7.5 超时

平台默认 RPC 超时为 10 秒。设备应在超时前返回 `rpc/response/{requestId}`。关键操作建议设备执行完成后，再通过属性/遥测上报实际状态，避免仅依赖 RPC 回执。

---

## 8. 平台校验与错误处理

### 8.1 平台校验内容

平台按产品 TSL 校验以下内容：

- 报文必须是 JSON 对象。
- 属性类型、范围、枚举、int32 限制。
- `struct` 属性必须是对象，且不允许嵌套 struct。
- 事件 `event_id` 必须属于当前产品 TSL。
- 事件 `event_type` 必须与 TSL 一致。
- 事件参数类型和范围。
- 同一包内 `events[].event_id` 必须一致。
- 单条 payload 小于 4 KB。
- 时间戳统一使用毫秒。

### 8.2 非法数据

平台不会针对每条 MQTT 消息返回业务错误码。设备收到 MQTT QoS 确认只代表消息已到达平台，不代表业务校验通过。

因此设备侧应：

- 按本文和产品 TSL 做本地校验；
- 记录本地发送日志；
- 对关键数据使用唯一 `event_ts`；
- 联调阶段通过平台日志或平台提供的排查工具确认数据是否入库。

### 8.3 幂等与重复上报

事件幂等键为：

```text
(device, event_id, event_ts)
```

相同设备、相同事件、相同 `event_ts` 的重复上报只保留一条。设备重传时应保持 `event_ts` 不变。

### 8.4 QoS 与断网补传

- 建议使用 QoS 1。
- 平台不保证缓存设备离线期间的消息。
- 设备应在本地缓存断网期间的数据，网络恢复后补传。
- 补传数据必须保留原始 `event_ts` / 属性时间戳。

### 8.5 时间戳

所有 `ts`、`event_ts`、`updateTs` 等时间字段统一使用毫秒。设备如果只有秒级时间，需要乘以 1000 后再上报。

---

## 9. 快速接入示例

以下示例中的 `<ACCESS_TOKEN>` 需要替换为平台发放的设备 Token。示例使用当前测试地址 `localhost:1883`。

### 9.1 mosquitto_pub 上报属性

```bash
mosquitto_pub \
  -h localhost -p 1883 \
  -u "<ACCESS_TOKEN>" -P "" \
  -t "v1/devices/me/telemetry" \
  -q 1 \
  -m '{
    "ts": 1793240957000,
    "values": {
      "deviceInfo": {
        "company": "三一重工",
        "name": "挖掘机-001",
        "model": "SY215C",
        "sn": "SN20220601001",
        "battery": 85,
        "updateTs": 1793240957000,
        "category": "挖掘机",
        "source": "自有",
        "managementNumber": "GL-2022-001",
        "onlineState": 2
      },
      "imei": "860000000000001",
      "signalStrength": -65,
      "totalMileage": 123456
    }
  }'
```

### 9.2 mosquitto_pub 上报事件数组

```bash
mosquitto_pub \
  -h localhost -p 1883 \
  -u "<ACCESS_TOKEN>" -P "" \
  -t "v1/devices/me/telemetry" \
  -q 1 \
  -m '{
    "events": [
      {
        "event_id": "alarm",
        "event_type": "alert",
        "event_ts": 1793240957000,
        "address": "广东省广州市天河区XXX路",
        "longitude": 113.264385,
        "latitude": 23.129163,
        "alarmAt": 1793240957000,
        "alarmType": "oil_abnormal",
        "remark": "油量异常下降，疑似漏油"
      }
    ]
  }'
```

### 9.3 Python paho-mqtt 示例

```python
import json

import paho.mqtt.client as mqtt

HOST = "localhost"
PORT = 1883
TOKEN = "<ACCESS_TOKEN>"
DEVICE_NAME = "4DZDCQUA2D-0001"


def on_connect(client, userdata, flags, reason_code, properties):
    print("connected:", reason_code)
    client.subscribe("v1/devices/me/rpc/request/+", qos=1)


def on_message(client, userdata, msg):
    request_id = msg.topic.rsplit("/", 1)[-1]
    request = json.loads(msg.payload.decode("utf-8"))
    method = request.get("method")
    print("rpc:", request_id, method, request.get("params"))

    if method == "reboot":
        response = {"success": True}
    elif method == "setReportInterval":
        response = {"success": True}
    elif method == "queryLocation":
        response = {
            "longitude": 113.264385,
            "latitude": 23.129163,
            "address": "广东省广州市天河区XXX路",
            "updateTs": 1793240957000,
        }
    elif method == "lockMachine":
        response = {"success": True}
    else:
        response = {"success": False, "error": "unsupported method"}

    client.publish(
        f"v1/devices/me/rpc/response/{request_id}",
        json.dumps(response),
        qos=1,
    )


client = mqtt.Client(
    mqtt.CallbackAPIVersion.VERSION2,
    client_id=DEVICE_NAME,
)
client.username_pw_set(TOKEN, "")
client.on_connect = on_connect
client.on_message = on_message
client.connect(HOST, PORT, keepalive=60)
client.loop_forever()
```

### 9.4 订阅下行 RPC

```bash
mosquitto_sub \
  -h localhost -p 1883 \
  -u "<ACCESS_TOKEN>" -P "" \
  -t "v1/devices/me/rpc/request/+" \
  -q 1
```

---

## 附录 A：4DZDCQUA2D 物模型

### A.1 产品信息

| 参数 | 值 |
| --- | --- |
| ProductId | `4DZDCQUA2D` |
| CategoryId | `1` |
| TSL 版本 | `1.1` |

### A.2 deviceInfo（设备信息）

| 字段 | 类型 | 说明 | 取值范围 |
| --- | --- | --- | --- |
| `company` | string | 设备厂商 | 建议 0~100 字符 |
| `name` | string | 设备名称 | 建议 0~50 字符 |
| `model` | string | 设备型号 | 建议 0~20 字符 |
| `sn` | string | 设备序列号 | 建议 0~20 字符 |
| `battery` | int | 设备电量 | 0~100，单位 % |
| `updateTs` | timestamp | 更新时间 | 毫秒时间戳 |
| `category` | string | 机械类型 | 建议 0~50 字符 |
| `source` | string | 机械来源 | 建议 0~50 字符 |
| `managementNumber` | string | 机械管理编号 | 建议 0~100 字符 |
| `onlineState` | enum | 物联网连接状态 | 0=未绑定，1=已绑定，2=已入网 |

### A.3 locationInfo（位置信息）

| 字段 | 类型 | 说明 | 取值范围 |
| --- | --- | --- | --- |
| `longitude` | float | 经度 | -180~180，单位 度 |
| `latitude` | float | 纬度 | -90~90，单位 度 |
| `updateTs` | timestamp | 位置更新时间 | 毫秒时间戳 |
| `address` | string | 当前地址描述 | - |
| `altitude` | float | 海拔 | 0~10000，单位 M |

### A.4 realStateInfo（实时状态）

| 字段 | 类型 | 说明 | 取值范围 |
| --- | --- | --- | --- |
| `machineState` | enum | 机械状态 | 0=working运行，1=motionless静止，2=idling怠速，4=offline离线 |
| `updateTs` | timestamp | 状态更新时间 | 毫秒时间戳 |
| `speed` | float | 当前速度 | 0~100，单位 km/h |
| `todayMileage` | int | 今日里程 | 0~100000000，单位 m |
| `isWork` | bool | 今日是否工作 | true/false（也兼容 0/1） |
| `workTime` | int | 今日运转时长 | 0~100000000，单位 S |
| `idleTime` | int | 今日怠速时长 | 0~100000000，单位 S |
| `fuelConsumptionVolume` | float | 今日油耗量 | 0~10000，单位 L |

### A.5 标量属性

| 属性 | 类型 | 说明 | 取值范围 |
| --- | --- | --- | --- |
| `imei` | string | 设备 IMEI | - |
| `iccid` | string | SIM 卡号 | - |
| `firmwareVersion` | string | 固件版本 | - |
| `signalStrength` | int | 信号强度 | -120~0，单位 dBm |
| `reportInterval` | int | 上报周期，读写 | 10~3600，单位 s |
| `totalMileage` | int | 累计里程 | 0~1000000000，单位 m |

### A.6 事件参数

#### refuelRecord（加油记录）

类型：`info`

| 字段 | 类型 | 说明 | 取值范围 |
| --- | --- | --- | --- |
| `volume` | float | 加油量 | 0~10000，单位 L |
| `percentage` | float | 加油百分比 | 0~100，单位 % |
| `startTs` | timestamp | 开始时间 | 毫秒时间戳 |
| `endTs` | timestamp | 结束时间 | 毫秒时间戳 |
| `tankId` | int | 油箱编号 | 0~10 |

#### workRecord（工作记录）

类型：`info`

| 字段 | 类型 | 说明 | 取值范围 |
| --- | --- | --- | --- |
| `state` | enum | 工作状态 | 0=working运行，1=motionless静止，2=idling怠速，4=offline离线 |
| `startTs` | timestamp | 开始时间 | 毫秒时间戳 |
| `endTs` | timestamp | 结束时间 | 毫秒时间戳 |
| `mileage` | int | 里程 | 0~100000000，单位 M |

#### fuelRecord（油量变化曲线）

类型：`info`

| 字段 | 类型 | 说明 | 取值范围 |
| --- | --- | --- | --- |
| `volume` | float | 油耗量 | 0~10000 |
| `percentage` | float | 油耗百分比 | 0~100，单位 % |
| `presentTs` | timestamp | 油耗产生时间 | 毫秒时间戳 |
| `pressure` | int | 油箱压力 | 0~10000 |
| `tankId` | int | 油箱编号 | 0~100 |

#### alarm（设备告警）

类型：`alert`

| 字段 | 类型 | 说明 | 取值范围 |
| --- | --- | --- | --- |
| `address` | string | 告警地点 | - |
| `longitude` | float | 经度 | -180~180，单位 度 |
| `latitude` | float | 纬度 | -90~90，单位 度 |
| `alarmAt` | timestamp | 告警时间 | 毫秒时间戳 |
| `alarmType` | enum_string | 告警类型 | 见下表 |
| `remark` | string | 告警详情 | - |

#### alarmType 枚举

| 值 | 说明 |
| --- | --- |
| `low_power` | 低电量报警 |
| `oil_abnormal` | 油量异常报警 |
| `equipment_dismantle` | 设备拆除报警 |
| `continuous_idling` | 持续怠速报警 |
| `oil_equipment_dismantle` | 油位监测仪破坏报警 |

### A.7 枚举汇总

#### onlineState

| 值 | 说明 |
| --- | --- |
| 0 | 未绑定 |
| 1 | 已绑定 |
| 2 | 已入网 |

#### machineState / workRecord.state

| 值 | 说明 |
| --- | --- |
| 0 | working 运行 |
| 1 | motionless 静止 |
| 2 | idling 怠速 |
| 4 | offline 离线 |

#### isWork

| 值 | 说明 |
| --- | --- |
| 0 / false | 未工作 |
| 1 / true | 工作 |

### A.8 行为

| 行为 | method | 输入 | 输出 |
| --- | --- | --- | --- |
| 重启设备 | `reboot` | 无 | `success: bool` |
| 设置上报周期 | `setReportInterval` | `interval: int，10~3600` | `success: bool` |
| 查询当前位置 | `queryLocation` | 无 | `longitude`、`latitude`、`address`、`updateTs` |
| 锁车/解锁 | `lockMachine` | `locked: bool` | `success: bool` |

---

## 附录 B：与参考文档的差异

本文参考了 `ztxk-distribute-service/docs/mqtt` 的物模型组织方式，但当前平台使用 ThingsBoard 原生 MQTT，差异如下：

| 参考文档 | 当前平台 |
| --- | --- |
| `propertyReport` 接口 | `v1/devices/me/telemetry` |
| `eventSinglePost` 接口 | `v1/devices/me/telemetry` 中的 `events` 数组 |
| `eventId` / `type` / `timestamp` / `params` | `event_id` / `event_type` / `event_ts` + 扁平参数 |
| 单条事件对象 | 推荐 `events` 数组；同一包内 `event_id` 一致 |
| 消息包含 `level` | 消息不包含 `level`，平台按 `event_type` 映射 |
| `mqtt-iot.crec.cn:1883` | 当前测试地址 `localhost:1883` |
| 参考文档中的 `FuelInfoProperty` | 当前 4DZDCQUA2D TSL 未定义，不纳入本文 |
| 参考文档中的 `MachineProperty` | 当前 4DZDCQUA2D TSL 未定义，不纳入本文 |

> 其他产品接入时，属性、事件、行为字段以该产品实际 TSL 为准；连接方式、主题、认证和事件数组规则保持不变。

---

## 12. 变更记录

| 版本 | 日期 | 说明 |
| --- | --- | --- |
| 1.0 | 2026-10-29 | 初始版本，基于 4DZDCQUA2D TSL 和 ThingsBoard MQTT 接入 |

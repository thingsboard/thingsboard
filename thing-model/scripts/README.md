# thing-model 数据模拟脚本

用于向 ThingsBoard 模拟上报物模型数据，验证 Device Profile、规则链和 Kafka 出口。

## 环境要求

- Python 3.9+
- thing-model 已启动
- ThingsBoard 已启动
- 产品已同步，产品 Asset 上存在 `tsl_schema`
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
  - 正常遥测
  - 事件
  - 非法数据
- 退出时删除临时设备

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

只打印消息，不创建设备、不发请求：

```bash
python thing-model/scripts/simulate_telemetry.py --dry-run --count 3
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
| `--device-name` | `sim-<product>-<timestamp>-<random>` | 测试设备名 |
| `--site-code` | `SIM` | 设备 `site_code` 属性 |
| `--mode` | `mixed` | `telemetry` / `event` / `invalid` / `mixed` |
| `--interval` | `5` | 消息间隔秒数 |
| `--count` | `0` | 消息数量，0 表示无限 |
| `--seed` | 空 | 随机种子 |
| `--dry-run` | 关闭 | 只打印消息 |
| `--keep-device` | 关闭 | 退出时保留设备 |
| `--timeout` | `10` | HTTP 超时秒数 |

## 数据生成规则

- `int`：在 `min/max` 范围内随机
- `float`：在 `min/max` 范围内随机
- `bool`：随机布尔值
- `string`：生成 `sim-<random>`，`bssid` 生成 MAC 地址
- `enum` / `enum_string`：从 mapping 中选择键
- `timestamp`：当前时间
- `struct`：默认跳过并打印 warning

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

## 注意

- 默认会创建并删除测试设备；使用 `--keep-device` 可保留。
- 脚本不会打印完整 Access Token。
- `--dry-run` 仍需要登录 ThingsBoard 以读取产品目录。
- 产品 Asset 上没有 `tsl_schema` 时，脚本会提示先执行产品同步。

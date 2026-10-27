# 腾讯 IoT 物模型迁移

`convert_tencent_tsl.py` 将腾讯 IoT 物模型转换为 `thing-model` 使用的 TSL。

## 用法

```powershell
# 单文件，输出到同目录 <name>.tsl.json
python thing-model/migrate/convert_tencent_tsl.py thing-model/migrate/4DZDCQUA2D.json

# 指定输出
python thing-model/migrate/convert_tencent_tsl.py thing-model/migrate/4DZDCQUA2D.json -o thing-model/migrate/4DZDCQUA2D.tsl.json

# 批量转换目录
python thing-model/migrate/convert_tencent_tsl.py thing-model/migrate --output-dir thing-model/migrate/converted
```

## 参数

| 参数 | 说明 |
| --- | --- |
| `input` | 输入 JSON 文件或目录 |
| `-o, --output` | 单文件输出路径 |
| `--output-dir` | 批量输出目录 |
| `--keep-string-ranges` | 保留字符串类型的 min/max/start/step |
| `--flatten-structs` | 将 struct 属性扁平化为 `parent_field` |
| `--product-id` | 覆盖 `profile.ProductId` |
| `--indent` | 输出缩进，默认 2 |

## 转换规则

- 数值字符串转数字：`"0"` → `0`，`"0.01"` → `0.01`
- `stringenum` → `enum_string`
- `struct.specs` 数组转对象，键为字段 id
- `dataType` → `type`
- 默认丢弃字符串类型的 `min`/`max`/`start`/`step`
- 保留 `enum` / `enum_string` 的 `mapping`
- 保留 struct 内 `bool` 的 `mapping`
- 事件参数 `define` 同步转换
- 默认不覆盖输入文件

`--flatten-structs` 使用下划线拼接字段 id，例如：

```text
deviceInfo.company → deviceInfo_company
```

不要使用点号，因为 TSL 标识符不允许 `.`。

## 测试

```powershell
python thing-model/migrate/test_convert_tencent_tsl.py
```

## 转换后校验

转换脚本只负责格式转换。建议通过运行中的 thing-model 校验：

```http
POST http://localhost:8090/api/products/4DZDCQUA2D/thing-model/validate
X-Authorization: Bearer <TENANT_ADMIN_JWT>
Content-Type: application/json

{"tsl": <converted-tsl>}
```

校验通过后再创建产品并同步 ThingsBoard。

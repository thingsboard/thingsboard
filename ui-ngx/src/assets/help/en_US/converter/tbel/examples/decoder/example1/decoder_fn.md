#### Decoder example JavaScript function

```javascript
{:code-style="max-height: 500px;"}
// decode payload to JSON

var data = decodeToJson(payload);

var result = [];

for (int i = 0; i < data.length; i++) {
  var report = data[i];
  var deviceName = report.serialNumber;
  var deviceType = metadata.deviceType;
  var raw = report.value;
  var decoded = hexToBytes(raw);
  // Result object with device attributes/telemetry data
  result.push({
    deviceName: deviceName,
    deviceType: deviceType,
    attributes: {model: metadata.model},
    telemetry: {
      ts: report.timestamp,
      values: {
        battery: parseBytesToInt(decoded, 0, 2) / 100.0,
        temperature: parseBytesToInt(decoded, 2, 2) / 100.0,
        rawData: JSON.stringify(report)
      }
    }
  });
}

return result;
{:copy-code}
```

<br>
<br>

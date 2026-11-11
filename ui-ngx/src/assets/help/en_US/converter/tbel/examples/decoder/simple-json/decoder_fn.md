#### Decoder example JavaScript function

```javascript
{:code-style="max-height: 500px;"}

// decode payload to JSON. See helper function below
var json = decodeToJson(payload);
// convert date to epoch in milliseconds
var timestamp = Date.parse(json.ts);
// Construct result object with time-series data
var result = {
    deviceName: json.serialNumber,
    deviceType: "Thermostat",
    deviceLabel: "Kitchen Thermostat",
    telemetry: {
        ts: timestamp,
        values: {
            temperature: json.t,
            humidity: json.h,
        }
    }
};

return result;
{:copy-code}
```

<br>
<br>

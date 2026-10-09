#### Decoder example JavaScript function

```javascript
{:code-style="max-height: 500px;"}

// decode payload to string. See helper function below
var str = decodeToString(payload);
// split the string to an array of strings using ',' delimiter
var csv = str.split(',');
// Construct result object with time-series data
var result = {
    deviceName: csv[0],
    deviceType: "Thermostat",
    deviceLabel: "Kitchen Thermostat",
    telemetry: {
        temperature: parseFloat(csv[1]),
        humidity: parseFloat(csv[2]),
    }
};

return result;
{:copy-code}
```

<br>
<br>

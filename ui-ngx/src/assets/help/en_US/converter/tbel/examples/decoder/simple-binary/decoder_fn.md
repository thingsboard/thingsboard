#### Decoder example JavaScript function

```javascript
{:code-style="max-height: 500px;"}
// Use first 4 bytes as device name 
var deviceName = "SN-" + parseBytesToInt(payload, 0, 4);

var result = {
  deviceName: deviceName,
  deviceType: "Thermometer",
  telemetry: {
    // Use 5th byte as a battery level
    battery: parseBytesToInt(payload, 4, 1),
    // Use bytes 6 and 7 as a temperature
    temperature: parseBytesToInt(payload, 5, 2) / 100.0,
    // Use 8th byte as a saturation level
    saturation: parseBytesToInt(payload, 7, 1)
  }
};

return result;
{:copy-code}
```

<br>
<br>

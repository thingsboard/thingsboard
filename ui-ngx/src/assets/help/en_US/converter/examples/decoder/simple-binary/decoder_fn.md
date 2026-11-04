#### Decoder example JavaScript function

```javascript
{:code-style="max-height: 500px;"}
// Use first 4 bytes as device name 
var deviceName = "SN-" + getInt(payload, 0, 4);

var result = {
    deviceName: deviceName,
    deviceType: "Thermometer",
    telemetry: {
        // Use 5th byte as a battery level
        battery: getInt(payload, 4, 5),
        // Use bytes 6 and 7 as a temperature
        temperature: getFloat(payload, 5, 7, 100),
        // Use 8th byte as a saturation level
        saturation: getInt(payload, 7, 8)
    }
};

/* Extracts integer value from the 'byteArray' using 'byteIdxFrom' and 'byteIdxTo'*/
function getInt(byteArray, byteIdxFrom, byteIdxTo) {
    var val = 0;
    for (var j = byteIdxFrom; j < byteIdxTo; j++) {
        val += byteArray[j];
        if (j < byteIdxTo - 1) {
            val = val << 8;
        }
    }
    return val;
}

/* Extracts float value from the 'byteArray' using 'byteIdxFrom' and 'byteIdxTo' and divides the value using 'divided' parameter*/
function getFloat(byteArray, byteIdxFrom, byteIdxTo, divided) {
    return getInt(byteArray, byteIdxFrom, byteIdxTo) / divided;
}


return result;
{:copy-code}
```

<br>
<br>

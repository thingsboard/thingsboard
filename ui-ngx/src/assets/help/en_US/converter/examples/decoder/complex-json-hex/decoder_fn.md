#### Decoder example JavaScript function

```javascript
{:code-style="max-height: 500px;"}
// decode payload to JSON
var data = decodeToJson(payload);

var result = [];

for (var i in data) {
    var report = data[i];
    var deviceName = report.serialNumber;
    var raw = report.value;
    var decoded = hexStringToByteArray(raw);
    // Result object with device attributes/telemetry data
    result.push({
        deviceName: deviceName,
        deviceType: "Thermometer",
        telemetry: {
            ts: Date.parse(report.timestamp),
            values: {
                batteryVoltage: getFloat(decoded, 0, 2, 100),
                temperature: getFloat(decoded, 2, 4, 100),
                rawData: report
            }
        }
    });
}

/** Helper functions **/

function decodeToString(payload) {
    return String.fromCharCode.apply(String, payload);
}

function decodeToJson(payload) {
    var str = decodeToString(payload);
    var data = JSON.parse(str);
    return data;
}

/* Converts hex string to byte array*/
function hexStringToByteArray(str) {
    if (!str) {
        return new Uint8Array();
    }
    var a = [];
    for (var i = 0, len = str.length; i < len; i += 2) {
        a.push(parseInt(str.substr(i, 2), 16));
    }
    return new Uint8Array(a);
}

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

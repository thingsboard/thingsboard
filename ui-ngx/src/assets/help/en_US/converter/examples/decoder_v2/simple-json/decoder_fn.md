#### Decoder example TBEL function

```javascript
{:code-style="max-height: 500px;"}
function decodePayload(input) {
  var result = { attributes: {}, telemetry: {}};
  var data = decodeToJson(input);
  var timestamp = metadata.ts;

  result.attributes.sn = data.sn;
  
  var values = {};
  values.battery = data.battery;
  values.temperature = data.temperature;
  values.saturation = data.saturation;

  result.telemetry = {
    ts: timestamp,
    values: values
  };

  return result;
}

var result = decodePayload(payload);
return result;

/** Helper function to decode raw payload bytes to string**/
function decodeToString(payload) {
  return String.fromCharCode.apply(String, payload);
}
/** Helper function to decode raw payload bytes to JSON object**/
function decodeToJson(payload) {
  return JSON.parse(decodeToString(payload));
}

{:copy-code}
```

<br>
<br>

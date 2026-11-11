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
{:copy-code}
```

<br>
<br>

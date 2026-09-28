#### Decoder example TBEL function

```javascript
{:code-style="max-height: 500px;"}
function decodePayload(input) {
  var result = { attributes: {}, telemetry: {}};
  
  result.attributes.sn = parseBytesToInt(input, 0, 4);
  
  var timestamp = metadata.ts; 
  
  var values = {};
  values.battery = parseBytesToInt(input, 4, 1);
  values.temperature = parseBytesToInt(input, 5, 2) / 100.0;
  values.saturation = parseBytesToInt(input, 7, 1);
  
  result.telemetry = {
    ts: timestamp,
    values: values
  };
  
  return result;
}

var result = decodePayload(payload);
return result;

function parseBytesToInt(input, offset, length) {
  var result = 0;
  for (var i = offset; i < offset + length; i++) {
    result = (result << 8) | (input[i] & 0xFF);
  }
  return result;
}
{:copy-code}
```

<br>
<br>

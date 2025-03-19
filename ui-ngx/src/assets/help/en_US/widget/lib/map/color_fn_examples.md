##### Examples

* Calculate color depending on `temperature` telemetry value for `thermostat` device type:

```javascript
var type = data.Type;
if (type == 'thermostat') {
  var temperature = data.temperature;
  if (typeof temperature !== undefined) {
    var percent = (temperature + 60)/120 * 100;
    return tinycolor.mix('blue', 'red', percent).toHexString();
  }
  return 'blue';
}
{:copy-code}
```

<br>
<br>

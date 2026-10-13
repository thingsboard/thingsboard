##### Examples

* Calculate color depending on `temperature` telemetry value for `thermostat` device type.<br>
  Ensure that the <code>Type</code> and <code>temperature</code> keys are included in the <b>additional data keys</b> configuration:

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

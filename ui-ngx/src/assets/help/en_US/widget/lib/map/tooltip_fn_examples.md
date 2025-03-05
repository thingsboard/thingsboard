##### Examples

* Display details with corresponding telemetry data for `energy meter` or `thermometer` device types:

```javascript
var deviceType = data.Type;
if (typeof deviceType !== undefined) {
  if (deviceType == "energy meter") {
    return '<b>${entityName}</b><br/><b>Energy:</b> ${energy:2} kWt<br/>';
  } else if (deviceType == "thermometer") {
    return '<b>${entityName}</b><br/><b>Temperature:</b> ${temperature:2} °C<br/>';
  }
}
return data.entityName;
{:copy-code}
```

<br>
<br>

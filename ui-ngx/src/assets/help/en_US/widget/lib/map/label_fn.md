#### Marker label function

<div class="divider"></div>
<br/>

*function (data, dsData, dsIndex): string*

A JavaScript function used to compute text or HTML code of the marker label.

**Parameters:**

<ul>
  {% include widget/lib/map/map_fn_args %}
</ul>

**Returns:**

Should return string value presenting text or HTML of the marker label.

<div class="divider"></div>

##### Examples

* Display styled label with corresponding latest telemetry data for `energy meter` or `thermometer` device types:

```javascript
var deviceType = data['Type'];
if (typeof deviceType !== undefined) {
  if (deviceType == "energy meter") {
    return '<span style="color:orange;">${entityName}, ${energy:2} kWt</span>';
  } else if (deviceType == "thermometer") {
    return '<span style="color:blue;">${entityName}, ${temperature:2} °C</span>';
  }
}
return data.entityName;
{:copy-code}
```

<br>
<br>

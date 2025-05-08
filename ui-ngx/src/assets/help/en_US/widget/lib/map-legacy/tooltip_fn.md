#### Marker tooltip function

<div class="divider"></div>
<br/>

*function (data, dsData, dsIndex): string*

A JavaScript function used to compute text or HTML code to be displayed in the marker, point or polygon tooltip.

**Parameters:**

<ul>
  {% include widget/lib/map-legacy/map_fn_args %}
</ul>

**Returns:**

Should return string value presenting text or HTML for the tooltip.

<div class="divider"></div>

##### Examples

* Display details with corresponding telemetry data for `thermostat` device type:

```javascript
var deviceType = data['Type'];
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

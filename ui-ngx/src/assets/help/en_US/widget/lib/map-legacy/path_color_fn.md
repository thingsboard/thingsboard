#### Path color function

<div class="divider"></div>
<br/>

*function (data, dsData, dsIndex): string*

A JavaScript function used to compute color of the trip path.

**Parameters:**

<ul>
  {% include widget/lib/map-legacy/map_fn_args %}
</ul>

**Returns:**

Should return string value presenting color of the trip path.

In case no data is returned, color value from **Path color** settings field will be used.

<div class="divider"></div>

##### Examples

* Calculate color depending on `temperature` telemetry value for `colorpin` device type:

```javascript
var type = data['Type'];
if (type == 'colorpin') {
  var temperature = data['temperature'];
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

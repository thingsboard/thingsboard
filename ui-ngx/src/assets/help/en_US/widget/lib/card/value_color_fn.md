#### Color function

<div class="divider"></div>
<br/>

*function (value): string*

A JavaScript function used to compute a color.

**Parameters:**

<ul>
  <li><b>value:</b> <code>primitive (number/string/boolean)</code> - A value of the current datapoint.
  </li>
</ul>

**Returns:**

Should return string value presenting color.

In case no data is returned, color value from **Color** settings field will be used.

<div class="divider"></div>

##### Examples

* Calculate color depending on `temperature` telemetry value:

```javascript
var temperature = value;
if (typeof temperature !== undefined) {
  var percent = (temperature + 60)/120 * 100;
  return tinycolor.mix('blue', 'red', percent).toHexString();
}
return 'blue';
{:copy-code}
```

<br>
<br>

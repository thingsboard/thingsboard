#### Tooltip value format function

<div class="divider"></div>
<br/>

*function (value): string*

A JavaScript function used to format datapoint value to be shown on the chart tooltip.

**Parameters:**

<ul>
  <li><b>value:</b> <code>primitive (number/string/boolean)</code> - A value of the datapoint that should be formatted.
  </li>
</ul>

**Returns:**

A string representing the formatted value.

<div class="divider"></div>

##### Examples

* Present the datapoint value in tooltip in Celsius (°C) units:

```javascript
return value + ' °C';
{:copy-code}
```

* Present the datapoint value in tooltip in Amperage (A) units and two decimal places:

```javascript
return value.toFixed(2) + ' A';
{:copy-code}
```

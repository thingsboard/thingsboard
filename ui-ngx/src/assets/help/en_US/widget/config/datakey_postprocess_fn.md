#### Data post-processing function

<div class="divider"></div>
<br/>

*function (time, value, prevValue, timePrev, prevOrigValue): any*

A JavaScript function doing post-processing on telemetry data.

**Parameters:**

<ul>
  <li><b>time:</b> <code>number</code> - timestamp in milliseconds of the current datapoint.
  </li>
  <li><b>value:</b> <code>primitive (number/string/boolean)</code> - A value of the current datapoint.
  </li>
  <li><b>prevValue:</b> <code>primitive (number/string/boolean)</code> - A value of the previous datapoint after applied post-processing.
  </li>
  <li><b>timePrev:</b> <code>number</code> - timestamp in milliseconds of the previous datapoint value.
  </li>
  <li><b>prevOrigValue:</b> <code>primitive (number/string/boolean)</code> - An original value of the previous datapoint.
  </li>
</ul>

**Returns:**

A primitive type (number, string or boolean) presenting the new datapoint value.

<div class="divider"></div>

##### Examples

* Multiply all datapoint values by 10:

```javascript
return value * 10;
{:copy-code}
```

* Round all datapoint values to whole numbers:

```javascript
return Math.round(value);
{:copy-code}
```
 
* Get relative difference between data points:

```javascript
if (prevOrigValue) {
    return (value - prevOrigValue) / prevOrigValue;
} else {
    return 0;
}
{:copy-code}
```
* Formatting data to time format

```javascript
if (value) {
  return moment(value).format("DD/MM/YYYY HH:mm:ss");
}
return '';
{:copy-code}
```

* Creates line-breaks for 0 values, when used in line chart

```javascript
if (value === 0) {
  return null;
} else {
  return value;
}
{:copy-code}
```

* Display data point of the HTML value card under the condition

```javascript
return value ? '<div class="info"><b>Temperature: </b>'+value+' °C</div>' : '';
{:copy-code}
```

<br>
<br>

#### Data post-processing function

<div class="divider"></div>
<br/>

*function (time, value): any*

[TBEL{:target="_blank"}](${siteBaseUrl}/docs${docPlatformPrefix}/user-guide/tbel/) function doing post-processing on telemetry data.

**Parameters:**

<ul>
  <li><b>time:</b> <code>number</code> - timestamp in milliseconds of the current datapoint.
  </li>
  <li><b>value:</b> <code>primitive (number/string/boolean)</code> - A value of the current datapoint.
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

* Render as HTML block under the condition

```javascript
return value ? '<div class="info"><b>Temperature: </b>'+value+' °C</div>' : '';
{:copy-code}
```

<br>
<br>

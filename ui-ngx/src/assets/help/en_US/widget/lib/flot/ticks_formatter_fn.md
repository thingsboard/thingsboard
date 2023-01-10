#### Ticks formatter function

<div class="divider"></div>
<br/>

*function (value): string*

A JavaScript function used to format Y axis ticks.

**Parameters:**

<ul>
  <li><b>value:</b> <code>number</code> - A tick value that should be formatted.
  </li>
</ul>

**Returns:**

A string presenting the formatted value to be displayed as Y axis tick.

<div class="divider"></div>

##### Examples

* Display ticks as is:

```javascript
return value;
{:copy-code}
```

* Present ticks in Amperage (A) units and two decimal places:

```javascript
return value.toFixed(2) + ' A';
{:copy-code}
```

* Disable ticks:

```javascript
return '';
{:copy-code}
```

* Present ticks in decimal format (1196 => 1,196.0):

```javascript
var value = value / 1;
function numberWithCommas(x) {
  return x.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ",");
}
return value ? numberWithCommas(value.toFixed(1)) : '';
{:copy-code}
```

<ul>
<li>
To present axis ticks for true / false or 1 / 0 data.<br> 
Display <code>On</code> when value > <code>0</code> and <= <code>1</code>,<br>
<code>Off</code> when value = <code>0</code>,<br>
disable for all other values.<br>
<strong>Note: </strong> To avoid duplicates among Y axis ticks it is recommended to set <strong><i>Steps size between ticks</i></strong> to <code>1</code>:
</li>
</ul>

```javascript
if (value > 0 && value <= 1) {
  return 'On';
} else if (value === 0) {
  return 'Off';
} else {
  return '';
}
{:copy-code}
```

<ul>
<li>
To present axis ticks for state or level data.<br> 
Display <code>High</code> when value >= <code>2</code>,<br>
<code>Medium</code> when value >= <code>1</code> and < <code>2</code>,<br>
<code>Low</code> when value >= <code>0</code> and < <code>1</code>,<br>
disable for all other values.<br>
<strong>Note: </strong> To avoid duplicates among Y axis ticks it is recommended to set <strong><i>Steps size between ticks</i></strong> to <code>1</code><br>
or other suitable value depending on your case:
</li>
</ul>

```javascript
if (value >= 2) {
  return 'High';
} else if (value >= 1) {
  return 'Medium';
} else if (value >= 0) {
  return 'Low';
} else {
  return '';
}
{:copy-code}
```

<br>
<br>

#### Data generation function

<div class="divider"></div>
<br/>

*function (time, prevValue): any*

A JavaScript function generating datapoint values.

**Parameters:**

<ul>
  <li><b>time:</b> <code>number</code> - timestamp in milliseconds of the current datapoint.
  </li>
  <li><b>prevValue:</b> <code>primitive (number/string/boolean)</code> - A previous datapoint value.
  </li>
</ul>

**Returns:**

A primitive type (number, string or boolean) presenting newly generated datapoint value.

<div class="divider"></div>

##### Examples

* Generate data with sine function:

```javascript
return Math.sin(time/5000);
{:copy-code}
```

* Generate true/false sequence:

```javascript
if (!prevValue) {
    return true;
} else {
    return false;
}
{:copy-code}
```

* Generate repeating sequence of predefined values (for ex. latitude):

```javascript
var lats = [37.7696499,
  37.7699074,
  37.7699536,
  37.7697242,
  37.7695189,
  37.7696889];

var index = Math.floor((time/3 % 14000) / 1000);

return lats[index];
{:copy-code}
```

<br>
<br>

#### Parse value function

<div class="divider"></div>
<br/>

*function (data): boolean*

A JavaScript function converting attribute/timeseries value or value of the RPC command response to boolean value.

**Parameters:**

<ul>
  <li><b>data:</b> <code>any</code> - attribute/timeseries value or value of the RPC command response.
   </li>
</ul>

**Returns:**

`true` if control widget should be switched on, `false` otherwise.

<div class="divider"></div>

##### Examples

* Switch on control widget for any positive value:

```javascript
return data ? true : false;
{:copy-code}
```

* Parse control widget state from json payload having `enabled` boolean property:

```javascript
var payload = typeof data === 'string' ? JSON.parse(data) : data;
return payload && payload.enabled ? true : false;
{:copy-code}
```

<br>
<br>

#### Convert value function

<div class="divider"></div>
<br/>

*function (value): any*

A JavaScript function converting target on/off state of the control widget to payload of the RPC set value command.

**Parameters:**

<ul>
  <li><b>value:</b> <code>boolean</code> - value indicating target on/off state of the control widget.
   </li>
</ul>

**Returns:**

Payload object or primitive to be used by the RPC set value command.

<div class="divider"></div>

##### Examples

* Use original target value as payload:

```javascript
return value;
{:copy-code}
```

* Create json payload with `enabled` boolean property:

```javascript
return { enabled: value };
{:copy-code}
```

<br>
<br>

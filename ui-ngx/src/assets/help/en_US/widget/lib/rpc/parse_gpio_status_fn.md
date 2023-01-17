#### Parse GPIO status function

<div class="divider"></div>
<br/>

*function (body, pin): boolean*

A JavaScript function evaluating enabled/disable state of GPIO pin from the response of GPIO status request.

**Parameters:**

<ul>
  <li><b>body:</b> <code>any</code> - response body of the GPIO status request.
   </li>
  <li><b>pin:</b> <code>number</code> - number of the GPIO pin.
   </li>
</ul>

**Returns:**

`true` if GPIO pin should be enabled, `false` otherwise.

<div class="divider"></div>

##### Examples

* Detect status of the pin assuming response body is array of boolean pins states:

```javascript
return body[pin] === true;
{:copy-code}
```

<br>
<br>

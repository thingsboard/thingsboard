#### Parse value function

<div class="divider"></div>
<br/>

*function (data): boolean*

A JavaScript function that converts the current dashboard state id into a boolean value.

**Parameters:**

<ul>
  <li><b>data:</b> <code> string </code> - the current dashboard state id.
   </li>
</ul>

**Returns:**

`true` if the widget should be in an activated state, `false` otherwise.

<div class="divider"></div>

##### Examples

* Check if the current dashboard state id is "default":

```javascript
return data === 'default' ? true : false;
{:copy-code}
```

<br>
<br>

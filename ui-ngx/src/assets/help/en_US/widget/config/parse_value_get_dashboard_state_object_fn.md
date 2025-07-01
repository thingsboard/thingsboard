#### Parse value function

<div class="divider"></div>
<br/>

*function (data): boolean*

A JavaScript function that converts the current dashboard state object into a boolean value.

**Parameters:**

<ul>
  <li><b>data:</b> <a href="https://github.com/thingsboard/thingsboard/blob/master/ui-ngx/src/app/core/api/widget-api.models.ts#L150" target="_blank">StateObject</a> - the current dashboard state object.
   </li>
</ul>

**Returns:**

`true` if the widget should be in an activated state, `false` otherwise.

<div class="divider"></div>

##### Examples

* Check if the current dashboard state id is "default":

```javascript
return data.id === 'default' ? true : false;
{:copy-code}
```

* Check if the current dashboard state parameters are empty:

```javascript
return Object.keys(data.params).length ? true : false;
{:copy-code}
```

<br>
<br>

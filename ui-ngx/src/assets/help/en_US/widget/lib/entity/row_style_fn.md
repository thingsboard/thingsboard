#### Row style function

<div class="divider"></div>
<br/>

*function (entity, ctx): {[key: string]: string}*

A JavaScript function used to compute entity row style depending on entity value.

**Parameters:**

<ul>
  <li><b>entity:</b> <code><a href="https://github.com/thingsboard/thingsboard/blob/e264f7b8ddff05bda85c4833bf497f47f447496e/ui-ngx/src/app/modules/home/components/widget/lib/table-widget.models.ts#L61" target="_blank">EntityData</a></code> - An 
            <a href="https://github.com/thingsboard/thingsboard/blob/e264f7b8ddff05bda85c4833bf497f47f447496e/ui-ngx/src/app/modules/home/components/widget/lib/table-widget.models.ts#L61" target="_blank">EntityData</a> object
            presenting basic entity properties (ex. <code>id</code>, <code>entityName</code>) and <br> provides access to other entity attributes/timeseries declared in widget datasource configuration.
  </li>
  <li><b>ctx:</b> <code><a href="https://github.com/thingsboard/thingsboard/blob/5bb6403407aa4898084832d6698aa9ea6d484889/ui-ngx/src/app/modules/home/models/widget-component.models.ts#L107" target="_blank">WidgetContext</a></code> - A reference to <a href="https://github.com/thingsboard/thingsboard/blob/5bb6403407aa4898084832d6698aa9ea6d484889/ui-ngx/src/app/modules/home/models/widget-component.models.ts#L107" target="_blank">WidgetContext</a> that has all necessary API 
     and data used by widget instance.
  </li>
</ul>

**Returns:**

Should return key/value object presenting style attributes.

<div class="divider"></div>

##### Examples

* Set color and font-weight table row:

```javascript
return {
  color:'rgb(0, 132, 214)',
  fontWeight: 600
}
{:copy-code}
```

* Set row background color depending on device type:

```javascript
var deviceType = entity.Type;
var color = '#fff';
switch (deviceType) {
  case 'thermostat':
    color = 'orange';
    break;
  case 'default':
    color = '#abab00';
    break;
}
return {
  backgroundColor: color
};
{:copy-code}
```

<br>
<br>

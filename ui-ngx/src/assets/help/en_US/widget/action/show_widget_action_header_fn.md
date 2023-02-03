#### Show widget header action function

<div class="divider"></div>
<br/>

*function (widgetContext, data): boolean*

A JavaScript function evaluating whether to display particular widget header action.  

**Parameters:**

<ul>
  <li><b>widgetContext:</b> <code><a href="https://github.com/thingsboard/thingsboard/blob/5bb6403407aa4898084832d6698aa9ea6d484889/ui-ngx/src/app/modules/home/models/widget-component.models.ts#L107" target="_blank">WidgetContext</a></code> - A reference to <a href="https://github.com/thingsboard/thingsboard/blob/5bb6403407aa4898084832d6698aa9ea6d484889/ui-ngx/src/app/modules/home/models/widget-component.models.ts#L107" target="_blank">WidgetContext</a> that has all necessary API 
     and data used by widget instance.
  </li>
  <li><b>data:</b> <code><a href="https://github.com/thingsboard/thingsboard/blob/5bb6403407aa4898084832d6698aa9ea6d484889/ui-ngx/src/app/modules/home/components/widget/lib/maps/map-models.ts#L108" target="_blank">FormattedData[]</a></code> - An array of <a href="https://github.com/thingsboard/thingsboard/blob/5bb6403407aa4898084832d6698aa9ea6d484889/ui-ngx/src/app/modules/home/components/widget/lib/maps/map-models.ts#L108" target="_blank">FormattedData</a> objects.<br/>
     Each object represents basic entity properties (ex. <code>entityId</code>, <code>entityName</code>)<br/>and provides access to other entity attributes/timeseries declared in widget datasource configuration.
  </li>
</ul>

**Returns:**

`true` if header action should be displayed, `false` otherwise. 

<div class="divider"></div>

##### Examples

* Display action only for customer users:

```javascript
return widgetContext.currentUser.authority === 'CUSTOMER_USER';
{:copy-code}
```

* Display action only if the first entity is device and has type `thermostat`:

```javascript
return data[0] && data[0].entityType === 'DEVICE' && data[0].Type === 'thermostat';
{:copy-code}
```

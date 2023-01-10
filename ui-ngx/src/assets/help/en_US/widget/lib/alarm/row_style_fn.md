#### Row style function

<div class="divider"></div>
<br/>

*function (alarm, ctx): {[key: string]: string}*

A JavaScript function used to compute alarm row style depending on alarm value.

**Parameters:**

<ul>
  <li><b>alarm:</b> <code><a href="https://github.com/thingsboard/thingsboard/blob/e264f7b8ddff05bda85c4833bf497f47f447496e/ui-ngx/src/app/shared/models/alarm.models.ts#L108" target="_blank">AlarmDataInfo</a></code> - An 
            <a href="https://github.com/thingsboard/thingsboard/blob/e264f7b8ddff05bda85c4833bf497f47f447496e/ui-ngx/src/app/shared/models/alarm.models.ts#L108" target="_blank">AlarmDataInfo</a> object
            presenting basic alarm properties (ex. <code>type</code>, <code>severity</code>, <code>originator</code>, etc.) and <br> provides access to other alarm or originator entity fields/attributes/timeseries declared in widget datasource configuration.
  </li>
  <li><b>ctx:</b> <code><a href="https://github.com/thingsboard/thingsboard/blob/5bb6403407aa4898084832d6698aa9ea6d484889/ui-ngx/src/app/modules/home/models/widget-component.models.ts#L107" target="_blank">WidgetContext</a></code> - A reference to <a href="https://github.com/thingsboard/thingsboard/blob/5bb6403407aa4898084832d6698aa9ea6d484889/ui-ngx/src/app/modules/home/models/widget-component.models.ts#L107" target="_blank">WidgetContext</a> that has all necessary API 
     and data used by widget instance.
  </li>
</ul>

**Returns:**

Should return key/value object presenting style attributes.

<div class="divider"></div>

##### Examples

* Set row background color depending on alarm severity:

```javascript
var severity = alarm.severity;
var color = '#fff';
switch (severity) {
  case 'CRITICAL':
    color = 'red';
    break;
  case 'MAJOR':
    color = 'orange';
    break;
  case 'MINOR':
    color = '#ffca3d';
    break;
  case 'WARNING':
    color = '#abab00';
    break;
  case 'INDETERMINATE':
    color = 'green';
    break;
}
return {
  backgroundColor: color
};
{:copy-code}
```

<br>
<br>

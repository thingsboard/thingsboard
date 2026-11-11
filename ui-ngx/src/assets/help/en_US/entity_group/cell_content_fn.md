#### Cell content function

<div class="divider"></div>
<br/>

*function (value, entity, datePipe): string*

A JavaScript function used to compute entity cell content HTML depending on entity field value.

**Parameters:**

<ul>
  <li><b>value:</b> <code>any</code> - An entity field value displayed in the cell.
  </li>
  <li><b>entity:</b> <code>object</code> - An 
            <span trigger-style="fontSize: 16px;" trigger-text="<b>ShortEntityView</b>" tb-help-popup="entity_group/short_entity_view_obj"></span> object
            presenting basic entity properties (ex. <code>id</code>, <code>name</code>) and provides access to entity<br>
            fields/attributes/timeseries configured in entity group <b>Columns</b> configuration.
  </li>
  <li><b>datePipe:</b> <code><a href="https://angular.io/api/common/DatePipe" target="_blank">DatePipe</a></code> - An angular pipe to format a date value according to locale rules.
             See <a href="https://angular.io/api/common/DatePipe" target="_blank">DatePipe</a> for API reference.
</li>
</ul>

**Returns:**

Should return string value presenting cell content HTML.

<div class="divider"></div>

##### Examples

* Format entity created time using date/time pattern:

```javascript
var createdTime = value;
return createdTime ? datePipe.transform(createdTime, 'yyyy-MM-dd HH:mm:ss') : '';
{:copy-code}
```

* Styled cell content for device profile field:

```javascript
var deviceType = value;
var color = '#fff';
switch (deviceType) {
  case 'thermostat':
    color = 'orange';
    break;
  case 'default':
    color = '#abab00';
    break;
}
return '<div style="border: 2px solid #0072ff; ' +
  'border-radius: 10px; padding: 5px; ' +
  'background-color: '+ color +'; ' +
  'text-align: center;">' + deviceType + '</div>';
{:copy-code}
```

<br>
<br>

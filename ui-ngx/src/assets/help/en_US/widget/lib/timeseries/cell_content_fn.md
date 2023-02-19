#### Cell content function

<div class="divider"></div>
<br/>

*function (value, rowData, ctx): string*

A JavaScript function used to compute timeseries cell content HTML depending on timeseries field value.

**Parameters:**

<ul>
  <li><b>value:</b> <code>any</code> - An entity field value displayed in the cell.
  </li>
  <li><b>rowData:</b> <code><a href="https://github.com/thingsboard/thingsboard/blob/e264f7b8ddff05bda85c4833bf497f47f447496e/ui-ngx/src/app/modules/home/components/widget/lib/timeseries-table-widget.component.ts#L80" target="_blank">TimeseriesRow</a></code> - A 
            <a href="https://github.com/thingsboard/thingsboard/blob/e264f7b8ddff05bda85c4833bf497f47f447496e/ui-ngx/src/app/modules/home/components/widget/lib/timeseries-table-widget.component.ts#L80" target="_blank">TimeseriesRow</a> object
            presenting <code>formattedTs</code> (a string value of formatted timestamp) and <br> timeseries values for each column declared in widget datasource configuration.
  </li>
  <li><b>ctx:</b> <code><a href="https://github.com/thingsboard/thingsboard/blob/5bb6403407aa4898084832d6698aa9ea6d484889/ui-ngx/src/app/modules/home/models/widget-component.models.ts#L107" target="_blank">WidgetContext</a></code> - A reference to <a href="https://github.com/thingsboard/thingsboard/blob/5bb6403407aa4898084832d6698aa9ea6d484889/ui-ngx/src/app/modules/home/models/widget-component.models.ts#L107" target="_blank">WidgetContext</a> that has all necessary API 
     and data used by widget instance.
  </li>
</ul>

**Returns:**

Should return string value presenting cell content HTML.

<div class="divider"></div>

##### Examples

* Styled cell content for temperature field:

```javascript
var temperature = value;
var color = '#fff';
if (temperature) {
  if (temperature > 25) {
    color = 'red';
  } else {
    color = 'green';
  }
}
return '<div style="border: 2px solid #0072ff; ' +
  'border-radius: 10px; padding: 5px; ' +
  'color: #fff; ' +
  'background-color: '+ color +'; ' +
  'text-align: center;">' + temperature + '</div>';
{:copy-code}
```

<br>
<br>

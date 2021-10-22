#### Row style function

<div class="divider"></div>
<br/>

*function (rowData, ctx): {[key: string]: string}*

A JavaScript function used to compute timeseries row style depending on row value.

**Parameters:**

<ul>
  <li><b>rowData:</b> <code><a href="https://github.com/thingsboard/thingsboard/blob/e264f7b8ddff05bda85c4833bf497f47f447496e/ui-ngx/src/app/modules/home/components/widget/lib/timeseries-table-widget.component.ts#L80" target="_blank">TimeseriesRow</a></code> - A 
            <a href="https://github.com/thingsboard/thingsboard/blob/e264f7b8ddff05bda85c4833bf497f47f447496e/ui-ngx/src/app/modules/home/components/widget/lib/timeseries-table-widget.component.ts#L80" target="_blank">TimeseriesRow</a> object
            presenting <code>formattedTs</code> (a string value of formatted timestamp) and <br> timeseries values for each column declared in widget datasource configuration.
  </li>
  <li><b>ctx:</b> <code><a href="https://github.com/thingsboard/thingsboard/blob/5bb6403407aa4898084832d6698aa9ea6d484889/ui-ngx/src/app/modules/home/models/widget-component.models.ts#L107" target="_blank">WidgetContext</a></code> - A reference to <a href="https://github.com/thingsboard/thingsboard/blob/5bb6403407aa4898084832d6698aa9ea6d484889/ui-ngx/src/app/modules/home/models/widget-component.models.ts#L107" target="_blank">WidgetContext</a> that has all necessary API 
     and data used by widget instance.
  </li>
</ul>

**Returns:**

Should return key/value object presenting style attributes.

<div class="divider"></div>

##### Examples

* Set row background color depending on temperature value:

```javascript
var temperature = rowData.temperature;
var color = '#fff';
if (temperature) {
  if (temperature > 25) {
    color = 'red';
  } else {
    color = 'green';
  }
}
return {
  backgroundColor: color
};
{:copy-code}
```

<br>
<br>

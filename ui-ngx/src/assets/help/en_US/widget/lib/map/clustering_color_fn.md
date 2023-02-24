#### Clustering marker function

<div class="divider"></div>
<br/>

*function (data, childCount): string*

A JavaScript function used to compute clustering marker color.

**Parameters:**

<ul>
  <li><b>data:</b> <code><a href="https://github.com/thingsboard/thingsboard/blob/5bb6403407aa4898084832d6698aa9ea6d484889/ui-ngx/src/app/modules/home/components/widget/lib/maps/map-models.ts#L108" target="_blank">FormattedData[]</a></code>
    - the array of total markers contained within each cluster.<br/>
     Represents basic entity properties (ex. <code>entityId</code>, <code>entityName</code>)<br/>and provides access to other entity attributes/timeseries declared in widget datasource configuration.
  </li>
  <li><b>childCount:</b> <code>number</code> - the total number of markers contained within that cluster
  </li>
</ul>

**Returns:**

Should return string value presenting color of the marker.

In case no data is returned, color value from **Color** settings field will be used.

<div class="divider"></div>

##### Examples

<ul>
<li>
Calculate color depending on temperature telemetry value:
</li>


```javascript
let customColor;
for (let markerData of data) {
  if (markerData.temperature > 40) {
    customColor = 'red'
  }
}
return customColor ? customColor : 'green';
{:copy-code}
```

<li>
Calculate color depending on childCount:
</li>

```javascript
if (childCount < 10) {
  return 'green';
} else if (childCount < 100) {
  return 'yellow';
} else {
  return 'red';
}
{:copy-code}
```

</ul>
<br>
<br>

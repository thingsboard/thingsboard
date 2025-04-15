#### Clustering marker function

<div class="divider"></div>
<br/>

*function (data, childCount): string*

A JavaScript function used to compute clustering marker color.

**Parameters:**

<ul>
  <li><b>data:</b> <code><a href="https://github.com/thingsboard/thingsboard/blob/b881f1c2985399f9665e033e2479549e97da1f36/ui-ngx/src/app/shared/models/widget.models.ts#L513" target="_blank">FormattedData[]</a></code>
    - the array of total markers contained within each cluster.<br/>
     Represents basic entity properties (ex. <code>entityId</code>, <code>entityName</code>)<br/>and provides access to other entity attributes/timeseries declared in datasource of the data layer configuration.
  </li>
  <li><b>childCount:</b> <code>number</code> - the total number of markers contained within that cluster
  </li>
</ul>

**Returns:**

Should return string value presenting color of the marker.

In case no data is returned, default colors will be used depending on number of markers within that cluster.

<div class="divider"></div>

##### Examples

<ul>
<li>
Calculate color depending on temperature telemetry value.<br/>
Ensure that the <code>temperature</code> key is included in the <b>additional data keys</b> configuration.
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

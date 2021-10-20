  <li><b>data:</b> <code><a href="https://github.com/thingsboard/thingsboard/blob/5bb6403407aa4898084832d6698aa9ea6d484889/ui-ngx/src/app/modules/home/components/widget/lib/maps/map-models.ts#L108" target="_blank">FormattedData</a></code> - A <a href="https://github.com/thingsboard/thingsboard/blob/5bb6403407aa4898084832d6698aa9ea6d484889/ui-ngx/src/app/modules/home/components/widget/lib/maps/map-models.ts#L108" target="_blank">FormattedData</a> object associated with the point of the trip.<br/>
     Represents basic entity properties (ex. <code>entityId</code>, <code>entityName</code>)<br/>and provides access to other entity attributes/timeseries declared in widget datasource configuration.
  </li>
  <li><b>dsData:</b> <code><a href="https://github.com/thingsboard/thingsboard/blob/5bb6403407aa4898084832d6698aa9ea6d484889/ui-ngx/src/app/modules/home/components/widget/lib/maps/map-models.ts#L108" target="_blank">FormattedData[][]</a></code> - two-dimensional array of <a href="https://github.com/thingsboard/thingsboard/blob/5bb6403407aa4898084832d6698aa9ea6d484889/ui-ngx/src/app/modules/home/components/widget/lib/maps/map-models.ts#L108" target="_blank">FormattedData</a> objects presenting<br/>
     array of trips (entities with timeseries data) resolved from configured datasources.<br/>
     Each element of array is particular entity trip data presented as array of <b>FormattedData</b> object (trip point).<br/>
     Each object represents basic entity properties (ex. <code>entityId</code>, <code>entityName</code>)<br/>
     and provides access to other entity attributes/timeseries declared in widget datasource configuration.
  </li>
  <li><b>dsIndex</b> <code>number</code> - index of the current trip data in <code>dsData</code> array.
  </li>

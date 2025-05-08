  <li><b>data:</b> <code><a href="https://github.com/thingsboard/thingsboard/blob/b881f1c2985399f9665e033e2479549e97da1f36/ui-ngx/src/app/shared/models/widget.models.ts#L513" target="_blank">FormattedData</a></code> object associated with data layer (markers/polygons/circles) or data point of the route (trips data layer).<br/>
     Represents basic entity properties (ex. <code>entityId</code>, <code>entityName</code>)<br/>and provides access to other entity attributes/timeseries declared in datasource of the data layer configuration.
  </li>
  <li><b>dsData:</b> <code><a href="https://github.com/thingsboard/thingsboard/blob/b881f1c2985399f9665e033e2479549e97da1f36/ui-ngx/src/app/shared/models/widget.models.ts#L513" target="_blank">FormattedData[]</a></code> - All available data associated with data layers including additional datasources as array of <a href="https://github.com/thingsboard/thingsboard/blob/b881f1c2985399f9665e033e2479549e97da1f36/ui-ngx/src/app/shared/models/widget.models.ts#L513" target="_blank">FormattedData</a> objects<br/>
     resolved from configured datasources. Each object represents basic entity properties (ex. <code>entityId</code>, <code>entityName</code>)<br/>
     and provides access to other entity attributes/timeseries declared in datasources of data layers configuration including additional datasources of the map configuration.
  </li>
  

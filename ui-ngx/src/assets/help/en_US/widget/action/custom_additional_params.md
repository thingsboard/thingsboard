#### Additional params object

<div class="divider"></div>
<br/>

<b>additionalParams:</b> <code>{[key: string]: any}</code>

An optional key/value object holding additional entity parameters depending on widget type and action source:

<ul>
  <li>Entities table widget (<i>On row click</i> or <i>Action cell button</i>) - <b>additionalParams:</b> <code>{ entity: <a href="https://github.com/thingsboard/thingsboard/blob/e264f7b8ddff05bda85c4833bf497f47f447496e/ui-ngx/src/app/modules/home/components/widget/lib/table-widget.models.ts#L61" target="_blank">EntityData</a> }</code>:
    <ul>
      <li><b>entity:</b> <code><a href="https://github.com/thingsboard/thingsboard/blob/e264f7b8ddff05bda85c4833bf497f47f447496e/ui-ngx/src/app/modules/home/components/widget/lib/table-widget.models.ts#L61" target="_blank">EntityData</a></code> - An 
            <a href="https://github.com/thingsboard/thingsboard/blob/e264f7b8ddff05bda85c4833bf497f47f447496e/ui-ngx/src/app/modules/home/components/widget/lib/table-widget.models.ts#L61" target="_blank">EntityData</a> object
            presenting basic entity properties (ex. <code>id</code>, <code>entityName</code>) and <br> provides access to other entity attributes/timeseries declared in widget datasource configuration.
      </li>
    </ul>
  </li>        
  <li>Alarms table widget (<i>On row click</i> or <i>Action cell button</i>) - <b>additionalParams:</b> <code>{ alarm: <a href="https://github.com/thingsboard/thingsboard/blob/7049f564b4f2a1f49f730c72a1c62f9f24aeb7cc/ui-ngx/src/app/shared/models/alarm.models.ts#L147" target="_blank">AlarmDataInfo</a> }</code>:
    <ul>
      <li><b>alarm:</b> <code><a href="https://github.com/thingsboard/thingsboard/blob/7049f564b4f2a1f49f730c72a1c62f9f24aeb7cc/ui-ngx/src/app/shared/models/alarm.models.ts#L147" target="_blank">AlarmDataInfo</a></code> - An 
            <a href="https://github.com/thingsboard/thingsboard/blob/7049f564b4f2a1f49f730c72a1c62f9f24aeb7cc/ui-ngx/src/app/shared/models/alarm.models.ts#L147" target="_blank">AlarmDataInfo</a> object
            presenting basic alarm properties (ex. <code>type</code>, <code>severity</code>, <code>originator</code>, etc.) and <br> provides access to other alarm or originator entity fields/attributes/timeseries declared in widget datasource configuration.
      </li>
    </ul>
  </li>        
  <li>Timeseries table widget (<i>On row click</i> or <i>Action cell button</i>) - <b>additionalParams:</b> <code><a href="https://github.com/thingsboard/thingsboard/blob/e264f7b8ddff05bda85c4833bf497f47f447496e/ui-ngx/src/app/modules/home/components/widget/lib/timeseries-table-widget.component.ts#L80" target="_blank">TimeseriesRow</a></code>:
    <ul>
      <li><b>additionalParams:</b> <code><a href="https://github.com/thingsboard/thingsboard/blob/e264f7b8ddff05bda85c4833bf497f47f447496e/ui-ngx/src/app/modules/home/components/widget/lib/timeseries-table-widget.component.ts#L80" target="_blank">TimeseriesRow</a></code> - A 
            <a href="https://github.com/thingsboard/thingsboard/blob/e264f7b8ddff05bda85c4833bf497f47f447496e/ui-ngx/src/app/modules/home/components/widget/lib/timeseries-table-widget.component.ts#L80" target="_blank">TimeseriesRow</a> object
            presenting <code>formattedTs</code> (a string value of formatted timestamp) and <br> timeseries values for each column declared in widget datasource configuration.
      </li>
    </ul>
  </li>
  <li>Map widgets (<i>On marker/polygon/circle click</i> or <i>Tag action</i>) - <b>additionalParams</b>: <code><a href="https://github.com/thingsboard/thingsboard/blob/b881f1c2985399f9665e033e2479549e97da1f36/ui-ngx/src/app/shared/models/widget.models.ts#L513" target="_blank">FormattedData</a></code>:
    <ul>
      <li><b>additionalParams:</b> <code><a href="https://github.com/thingsboard/thingsboard/blob/b881f1c2985399f9665e033e2479549e97da1f36/ui-ngx/src/app/shared/models/widget.models.ts#L513" target="_blank">FormattedData</a></code> - An object associated with a data layer (markers, polygons, circles) or with a specific data point of a route (for trips data layers).<br/>
          It contains basic entity properties (ex. <code>entityId</code>, <code>entityName</code>) and provides access to additional attributes and timeseries defined in datasource of the data layer configuration.
      </li>
    </ul>
  </li>
  <li>Entities hierarchy widget (<i>On node selected</i>) - <b>additionalParams:</b> <code>{ nodeCtx: <a href="https://github.com/thingsboard/thingsboard/blob/e264f7b8ddff05bda85c4833bf497f47f447496e/ui-ngx/src/app/modules/home/components/widget/lib/entities-hierarchy-widget.models.ts#L35" target="_blank">HierarchyNodeContext</a> }</code>:
    <ul>
      <li><b>nodeCtx:</b> <code><a href="https://github.com/thingsboard/thingsboard/blob/e264f7b8ddff05bda85c4833bf497f47f447496e/ui-ngx/src/app/modules/home/components/widget/lib/entities-hierarchy-widget.models.ts#L35" target="_blank">HierarchyNodeContext</a></code> - An 
            <a href="https://github.com/thingsboard/thingsboard/blob/e264f7b8ddff05bda85c4833bf497f47f447496e/ui-ngx/src/app/modules/home/components/widget/lib/entities-hierarchy-widget.models.ts#L35" target="_blank">HierarchyNodeContext</a> object
            containing <code>entity</code> field holding basic entity properties <br> (ex. <code>id</code>, <code>name</code>, <code>label</code>) and <code>data</code> field holding other entity attributes/timeseries declared in widget datasource configuration.
      </li>
    </ul>
  </li>
  <li>Pie (<i>On slice click</i>) - <b>additionalParams:</b> <code>LatestChartDataItem</code>:
    <ul>
      <li><b>additionalParams:</b> <code>LatestChartDataItem</code> - A 
            data object of clicked pie slice 
            containing <code>dataKey</code>, <code>datasource</code> and <code>value</code> fields.
      </li>
    </ul>
  </li>
  <li>Pie - Flot widget (deprecated) (<i>On slice click</i>) - <b>additionalParams:</b> <code><a href="https://github.com/thingsboard/thingsboard/blob/e264f7b8ddff05bda85c4833bf497f47f447496e/ui-ngx/src/app/modules/home/components/widget/lib/flot-widget.models.ts#L62" target="_blank">TbFlotPlotItem</a></code>:
    <ul>
      <li><b>additionalParams:</b> <code><a href="https://github.com/thingsboard/thingsboard/blob/e264f7b8ddff05bda85c4833bf497f47f447496e/ui-ngx/src/app/modules/home/components/widget/lib/flot-widget.models.ts#L62" target="_blank">TbFlotPlotItem</a></code> - A 
            <a href="https://github.com/thingsboard/thingsboard/blob/e264f7b8ddff05bda85c4833bf497f47f447496e/ui-ngx/src/app/modules/home/components/widget/lib/flot-widget.models.ts#L62" target="_blank">TbFlotPlotItem</a> object
            containing <code>series</code> field with information about datasource and <br> data key of clicked pie slice.
      </li>
    </ul>
  </li>
  <li><i>All other widgets</i> - does not provide <b>additionalParams</b> value.
  </li>
</ul> 

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
  <li>Alarms table widget (<i>On row click</i> or <i>Action cell button</i>) - <b>additionalParams:</b> <code>{ alarm: <a href="https://github.com/thingsboard/thingsboard/blob/e264f7b8ddff05bda85c4833bf497f47f447496e/ui-ngx/src/app/shared/models/alarm.models.ts#L108" target="_blank">AlarmDataInfo</a> }</code>:
    <ul>
      <li><b>alarm:</b> <code><a href="https://github.com/thingsboard/thingsboard/blob/e264f7b8ddff05bda85c4833bf497f47f447496e/ui-ngx/src/app/shared/models/alarm.models.ts#L108" target="_blank">AlarmDataInfo</a></code> - An 
            <a href="https://github.com/thingsboard/thingsboard/blob/e264f7b8ddff05bda85c4833bf497f47f447496e/ui-ngx/src/app/shared/models/alarm.models.ts#L108" target="_blank">AlarmDataInfo</a> object
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
  <li>Entities hierarchy widget (<i>On node selected</i>) - <b>additionalParams:</b> <code>{ nodeCtx: <a href="https://github.com/thingsboard/thingsboard/blob/e264f7b8ddff05bda85c4833bf497f47f447496e/ui-ngx/src/app/modules/home/components/widget/lib/entities-hierarchy-widget.models.ts#L35" target="_blank">HierarchyNodeContext</a> }</code>:
    <ul>
      <li><b>nodeCtx:</b> <code><a href="https://github.com/thingsboard/thingsboard/blob/e264f7b8ddff05bda85c4833bf497f47f447496e/ui-ngx/src/app/modules/home/components/widget/lib/entities-hierarchy-widget.models.ts#L35" target="_blank">HierarchyNodeContext</a></code> - An 
            <a href="https://github.com/thingsboard/thingsboard/blob/e264f7b8ddff05bda85c4833bf497f47f447496e/ui-ngx/src/app/modules/home/components/widget/lib/entities-hierarchy-widget.models.ts#L35" target="_blank">HierarchyNodeContext</a> object
            containing <code>entity</code> field holding basic entity properties <br> (ex. <code>id</code>, <code>name</code>, <code>label</code>) and <code>data</code> field holding other entity attributes/timeseries declared in widget datasource configuration.
      </li>
    </ul>
  </li>    
  <li>Pie - Flot widget (<i>On slice click</i>) - <b>additionalParams:</b> <code><a href="https://github.com/thingsboard/thingsboard/blob/e264f7b8ddff05bda85c4833bf497f47f447496e/ui-ngx/src/app/modules/home/components/widget/lib/flot-widget.models.ts#L62" target="_blank">TbFlotPlotItem</a></code>:
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

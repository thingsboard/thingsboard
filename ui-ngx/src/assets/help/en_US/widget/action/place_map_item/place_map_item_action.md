#### Place map item function

<div class="divider"></div>
<br/>

*function ($event, widgetContext, entityId, entityName, htmlTemplate, additionalParams, entityLabel): void*

A JavaScript function triggered after a map item is placed. Optionally uses an HTML template to render dialog.

**Parameters:**

<ul style="width: 700px">
  <li><b>$event:</b> <code>{shape: <a href="https://github.com/geoman-io/leaflet-geoman/blob/6335a8c6cbebfcd06707d3c5da9d3d393cd2d942/leaflet-geoman.d.ts#L829" target="_blank">PM.SUPPORTED_SHAPES</a>; layer: <a href="https://leafletjs.com/reference.html#layer" target="_blank">L.Layer</a>}</code> - Event payload containing the created shape type and its associated map layer.
  </li>
  <li><b>widgetContext:</b> <code><a href="https://github.com/thingsboard/thingsboard/blob/5bb6403407aa4898084832d6698aa9ea6d484889/ui-ngx/src/app/modules/home/models/widget-component.models.ts#L107" target="_blank">WidgetContext</a></code> - A reference to <a href="https://github.com/thingsboard/thingsboard/blob/5bb6403407aa4898084832d6698aa9ea6d484889/ui-ngx/src/app/modules/home/models/widget-component.models.ts#L107" target="_blank">WidgetContext</a> that has all necessary API 
     and data used by widget instance.
  </li>
  <li><b>entityId:</b> <code>string</code> - An optional string id of the target entity.
  </li>
  <li><b>entityName:</b> <code>string</code> - An optional string name of the target entity.
  </li>
  <li><b>htmlTemplate:</b> <code>string</code> - An optional HTML template string defined in <b>HTML</b> tab.<br/> Used to render custom dialog (see <b>Examples</b> for more details).
  </li>
  <li><b>additionalParams</b>: <code>{coordinates: Coordinates; layer: <a href="https://leafletjs.com/reference.html#layer" target="_blank">L.Layer</a>}</code>:
    <ul>
      <li><b>coordinates:</b> <code>Coordinates</code> - Represents geographical coordinates of the placed map item. The actual format of this parameter depends on the type of the selected map item:
        <ul>
          <li><b>Marker:</b> <code>{x: number; y: number}</code>, where <code>x</code> represents latitude, and <code>y</code> represents longitude.</li>
          <li><b>Polygon, Rectangle:</b> <code><a href="https://github.com/thingsboard/thingsboard/blob/e8b2066fb6f38bcc5775ea5b51a0755dfb763df8/ui-ngx/src/app/shared/models/widget/maps/map.models.ts#L1326" target="_blank">TbPolygonRawCoordinates</a></code> contains an array of points defining the shape boundaries.</li>
          <li><b>Circle:</b> <code><a href="https://github.com/thingsboard/thingsboard/blob/e8b2066fb6f38bcc5775ea5b51a0755dfb763df8/ui-ngx/src/app/shared/models/widget/maps/map.models.ts#L1338" target="_blank">TbCircleData</a></code> contains center coordinates and radius information.</li>
          <li><b>Polyline:</b> <code><a href="https://github.com/thingsboard/thingsboard/blob/e8b2066fb6f38bcc5775ea5b51a0755dfb763df8/ui-ngx/src/app/shared/models/widget/maps/map.models.ts#L1333" target="_blank">TbPolylineRawCoordinates</a></code> contains an array of points defining the polyline.</li>
        </ul>
        Note: The coordinates will be automatically converted according to the selected map type.
      </li>
      <li><b>layer:</b> <code><a href="https://leafletjs.com/reference.html#layer" target="_blank">L.Layer</a></code> - The Leaflet map layer instance (e.g., marker, polygon, circle) associated with the placed map item. This object provides access to layer properties and methods defined in Leaflet's API.
      </li>
    </ul>
  </li>
  <li><b>entityLabel:</b> <code>string</code> - An optional string label of the target entity.
  </li>
</ul>

<div class="divider"></div>

##### Examples

###### Display dialog to create a device or an asset

<br>

<div style="padding-left: 64px;"
     tb-help-popup="widget/action/place_map_item/create_dialog_js"
     tb-help-popup-placement="top"
     [tb-help-popup-style]="{maxHeight: '50vh', maxWidth: '50vw'}"
     trigger-style="font-size: 16px;"
     trigger-text="JavaScript function">
</div>

<br>

<div style="padding-left: 64px;"
     tb-help-popup="widget/action/place_map_item/create_dialog_html"
     tb-help-popup-placement="top"
     [tb-help-popup-style]="{maxHeight: '50vh', maxWidth: '50vw'}"
     trigger-style="font-size: 16px;"
     trigger-text="HTML code">
</div>

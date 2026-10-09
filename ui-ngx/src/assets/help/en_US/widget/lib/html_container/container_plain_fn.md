#### Container function

<div class="divider"></div>
<br/>

*function (ctx, container): void*

A JavaScript function used to initialize libraries, build or update the DOM inside the widget content element, wire up event handlers and drive the widget through the widget context.

**Parameters:**

<ul>
  <li><b>ctx:</b> <code><a href="https://github.com/thingsboard/thingsboard/blob/5bb6403407aa4898084832d6698aa9ea6d484889/ui-ngx/src/app/modules/home/models/widget-component.models.ts#L107" target="_blank">WidgetContext</a></code> - A reference to <a href="https://github.com/thingsboard/thingsboard/blob/5bb6403407aa4898084832d6698aa9ea6d484889/ui-ngx/src/app/modules/home/models/widget-component.models.ts#L107" target="_blank">WidgetContext</a> that has all necessary API
     and data used by widget instance.<br/>
     Provides access to send RPC, switch dashboard states, create data subscriptions, read settings and access platform services.
  </li>
  <li><b>container:</b> <code>HTMLElement</code> - The widget's content element, a native DOM node (<code>&lt;div class="tb-absolute-fill"&gt;</code>) wrapping the rendered template.<br/>
     Query it with <code>container.querySelector(...)</code>.
  </li>
</ul>

**Returns:**

This function does not return any value.

<div class="divider"></div>

##### Notes

<ul>
  <li>This widget does <b>not</b> bind widget datasources automatically. There is no <code>ctx.data</code> populated from the widget configuration. To read live data create an explicit subscription via <code>ctx.subscriptionApi.createSubscription(...)</code>.</li>
  <li>Release every subscription, timer and global event listener you create from <code>ctx.registerDestroyCallback(...)</code> so the widget cleans up correctly.</li>
</ul>

<div class="divider"></div>

##### Examples

###### Kanban board with a CDN drag-and-drop library

<br>

<div style="padding-left: 64px;"
     tb-help-popup="widget/lib/html_container/examples/plain_kanban_resources"
     tb-help-popup-placement="top"
     [tb-help-popup-style]="{maxHeight: '50vh', maxWidth: '50vw'}"
     trigger-style="font-size: 16px;"
     trigger-text="Resources">
</div>

<br>

<div style="padding-left: 64px;"
     tb-help-popup="widget/lib/html_container/examples/plain_kanban_html"
     tb-help-popup-placement="top"
     [tb-help-popup-style]="{maxHeight: '50vh', maxWidth: '50vw'}"
     trigger-style="font-size: 16px;"
     trigger-text="HTML code">
</div>

<br>

<div style="padding-left: 64px;"
     tb-help-popup="widget/lib/html_container/examples/plain_kanban_css"
     tb-help-popup-placement="top"
     [tb-help-popup-style]="{maxHeight: '50vh', maxWidth: '50vw'}"
     trigger-style="font-size: 16px;"
     trigger-text="CSS">
</div>

<br>

<div style="padding-left: 64px;"
     tb-help-popup="widget/lib/html_container/examples/plain_kanban_js"
     tb-help-popup-placement="top"
     [tb-help-popup-style]="{maxHeight: '50vh', maxWidth: '50vw'}"
     trigger-style="font-size: 16px;"
     trigger-text="JavaScript function">
</div>

###### Visitor analytics with a custom range and working hours

<br>

<div style="padding-left: 64px;"
     tb-help-popup="widget/lib/html_container/examples/plain_analytics_resources"
     tb-help-popup-placement="top"
     [tb-help-popup-style]="{maxHeight: '50vh', maxWidth: '50vw'}"
     trigger-style="font-size: 16px;"
     trigger-text="Resources">
</div>

<br>

<div style="padding-left: 64px;"
     tb-help-popup="widget/lib/html_container/examples/plain_analytics_html"
     tb-help-popup-placement="top"
     [tb-help-popup-style]="{maxHeight: '50vh', maxWidth: '50vw'}"
     trigger-style="font-size: 16px;"
     trigger-text="HTML code">
</div>

<br>

<div style="padding-left: 64px;"
     tb-help-popup="widget/lib/html_container/examples/plain_analytics_css"
     tb-help-popup-placement="top"
     [tb-help-popup-style]="{maxHeight: '50vh', maxWidth: '50vw'}"
     trigger-style="font-size: 16px;"
     trigger-text="CSS">
</div>

<br>

<div style="padding-left: 64px;"
     tb-help-popup="widget/lib/html_container/examples/plain_analytics_js"
     tb-help-popup-placement="top"
     [tb-help-popup-style]="{maxHeight: '50vh', maxWidth: '50vw'}"
     trigger-style="font-size: 16px;"
     trigger-text="JavaScript function">
</div>

<br>
<br>

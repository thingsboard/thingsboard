#### Container function

<div class="divider"></div>
<br/>

*function (ctx): void*

A JavaScript function used to expose data and handlers to the Angular template by assigning them to <code>this</code>. Reference them in the template without the <code>this</code> prefix.

**Parameters:**

<ul>
  <li><b>ctx:</b> <code><a href="https://github.com/thingsboard/thingsboard/blob/5bb6403407aa4898084832d6698aa9ea6d484889/ui-ngx/src/app/modules/home/models/widget-component.models.ts#L107" target="_blank">WidgetContext</a></code> - A reference to <a href="https://github.com/thingsboard/thingsboard/blob/5bb6403407aa4898084832d6698aa9ea6d484889/ui-ngx/src/app/modules/home/models/widget-component.models.ts#L107" target="_blank">WidgetContext</a> that has all necessary API
     and data used by widget instance.<br/>
     Provides access to send RPC, switch dashboard states, create data subscriptions, read settings and access platform services.
  </li>
</ul>

**Returns:**

This function does not return any value. Expose values and handlers to the template by assigning them to <code>this</code>.

<div class="divider"></div>

##### Notes

<ul>
  <li>There is no <code>container</code> parameter in Angular mode. Access the root element via <code>ctx.$container[0]</code> or <code>event.currentTarget.closest(...)</code> from a handler.</li>
  <li>Define helper functions as arrow functions to preserve <code>this</code>.</li>
  <li>Call <code>ctx.detectChanges()</code> after changing template-bound values so the template re-renders.</li>
  <li>This widget does <b>not</b> bind widget datasources automatically. There is no <code>ctx.data</code> populated from the widget configuration. To read live data create an explicit subscription via <code>ctx.subscriptionApi.createSubscription(...)</code> and release it from <code>ctx.registerDestroyCallback(...)</code>.</li>
</ul>

<div class="divider"></div>

##### Examples

###### Resizable split master–detail with embedded dashboard state

<br>

<div style="padding-left: 64px;"
     tb-help-popup="widget/lib/html_container/examples/angular_split_html"
     tb-help-popup-placement="top"
     [tb-help-popup-style]="{maxHeight: '50vh', maxWidth: '50vw'}"
     trigger-style="font-size: 16px;"
     trigger-text="HTML template">
</div>

<br>

<div style="padding-left: 64px;"
     tb-help-popup="widget/lib/html_container/examples/angular_split_css"
     tb-help-popup-placement="top"
     [tb-help-popup-style]="{maxHeight: '50vh', maxWidth: '50vw'}"
     trigger-style="font-size: 16px;"
     trigger-text="CSS">
</div>

<br>

<div style="padding-left: 64px;"
     tb-help-popup="widget/lib/html_container/examples/angular_split_js"
     tb-help-popup-placement="top"
     [tb-help-popup-style]="{maxHeight: '50vh', maxWidth: '50vw'}"
     trigger-style="font-size: 16px;"
     trigger-text="JavaScript function">
</div>

###### Slide-over device detail with two-way RPC

<br>

<div style="padding-left: 64px;"
     tb-help-popup="widget/lib/html_container/examples/angular_drawer_html"
     tb-help-popup-placement="top"
     [tb-help-popup-style]="{maxHeight: '50vh', maxWidth: '50vw'}"
     trigger-style="font-size: 16px;"
     trigger-text="HTML template">
</div>

<br>

<div style="padding-left: 64px;"
     tb-help-popup="widget/lib/html_container/examples/angular_drawer_css"
     tb-help-popup-placement="top"
     [tb-help-popup-style]="{maxHeight: '50vh', maxWidth: '50vw'}"
     trigger-style="font-size: 16px;"
     trigger-text="CSS">
</div>

<br>

<div style="padding-left: 64px;"
     tb-help-popup="widget/lib/html_container/examples/angular_drawer_js"
     tb-help-popup-placement="top"
     [tb-help-popup-style]="{maxHeight: '50vh', maxWidth: '50vw'}"
     trigger-style="font-size: 16px;"
     trigger-text="JavaScript function">
</div>

###### Composite dashboard with tab navigation

<br>

<div style="padding-left: 64px;"
     tb-help-popup="widget/lib/html_container/examples/angular_tabs_html"
     tb-help-popup-placement="top"
     [tb-help-popup-style]="{maxHeight: '50vh', maxWidth: '50vw'}"
     trigger-style="font-size: 16px;"
     trigger-text="HTML template">
</div>

<br>

<div style="padding-left: 64px;"
     tb-help-popup="widget/lib/html_container/examples/angular_tabs_css"
     tb-help-popup-placement="top"
     [tb-help-popup-style]="{maxHeight: '50vh', maxWidth: '50vw'}"
     trigger-style="font-size: 16px;"
     trigger-text="CSS">
</div>

<br>

<div style="padding-left: 64px;"
     tb-help-popup="widget/lib/html_container/examples/angular_tabs_js"
     tb-help-popup-placement="top"
     [tb-help-popup-style]="{maxHeight: '50vh', maxWidth: '50vw'}"
     trigger-style="font-size: 16px;"
     trigger-text="JavaScript function">
</div>

<br>
<br>

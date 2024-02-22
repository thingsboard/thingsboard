#### Widget type JavaScript code

<div class="divider"></div>
<br/>

All widget related JavaScript code according to the [Widget API{:target="_blank"}](${siteBaseUrl}/docs/user-guide/contribution/widgets-development/#basic-widget-api).
The built-in variable **self** is a reference to the widget instance.<br>
Each widget function should be defined as a property of the **self** variable.
**self** variable has property **ctx** of type [WidgetContext{:target="_blank"}](https://github.com/thingsboard/thingsboard/blob/5bb6403407aa4898084832d6698aa9ea6d484889/ui-ngx/src/app/modules/home/models/widget-component.models.ts#L107) - a reference to widget context that has all necessary API and data used by widget instance.

In order to implement a new widget, the following JavaScript functions should be defined *(Note: each function is optional and can be implemented according to  widget specific behaviour):*

|{:auto} **Function**                       | **Description**                                                                        |
|------------------------------------|----------------------------------------------------------------------------------------|
| ``` onInit() ```                   | The first function which is called when widget is ready for initialization. Should be used to prepare widget DOM, process widget settings and initial subscription information. |
| ``` onDataUpdated() ```            | Called when the new data is available from the widget subscription. Latest data can be accessed from the <span trigger-style="fontSize: 16px;" trigger-text="<b>defaultSubscription</b>" tb-help-popup="widget/editor/widget_js_subscription_object"></span> object of widget context (**ctx**). |
| ``` onResize() ```                 | Called when widget container is resized. Latest width and height can be obtained from widget context (**ctx**).             |
| ``` onEditModeChanged() ```        | Called when dashboard editing mode is changed. Latest mode is handled by isEdit property of **ctx**.             |
| ``` onMobileModeChanged() ```      | Called when dashboard view width crosses mobile breakpoint. Latest state is handled by isMobile property of **ctx**.                 |
| ``` onDestroy() ```                | Called when widget element is destroyed. Should be used to cleanup all resources if necessary.            |
| ``` getSettingsSchema() ```        | Optional function returning widget settings schema json as alternative to **Settings schema** of settings section.             |
| ``` getDataKeySettingsSchema() ``` | Optional function returning particular data key settings schema json as alternative to **Data key settings schema** tab of settings section.                   |
| ``` typeParameters() ```           | Returns [WidgetTypeParameters{:target="_blank"}](https://github.com/thingsboard/thingsboard/blob/2627fe51d491055d4140f16617ed543f7f5bd8f6/ui-ngx/src/app/shared/models/widget.models.ts#L151) object describing widget datasource parameters. See <span trigger-style="fontSize: 16px;" trigger-text="<b>Type parameters object</b>" tb-help-popup="widget/editor/widget_js_type_parameters_object"></span> |            |
| ``` actionSources() ```            | Returns map describing available widget action sources ([WidgetActionSource{:target="_blank"}](https://github.com/thingsboard/thingsboard/blob/2627fe51d491055d4140f16617ed543f7f5bd8f6/ui-ngx/src/app/shared/models/widget.models.ts#L121)) used to define user actions. See <span trigger-style="fontSize: 16px;" trigger-text="<b>Action sources object</b>" tb-help-popup="widget/editor/widget_js_action_sources_object"></span> |

<div class="divider"></div>

##### Creating simple widgets 

The tutorials below show how to create minimal widgets of each type.
In order to minimize the amount of code, the Angular framework will be used, on which ThingsBoard UI is actually based.
By the way, you can always use pure JavaScript or jQuery API in your widget code.

<br>

<div style="padding-left: 64px;"
     tb-help-popup="widget/editor/examples/latest_values_widget"
     tb-help-popup-placement="top"
     trigger-style="font-size: 16px;"
     trigger-text="Sample Latest Values widget">
</div>

<br>

<div style="padding-left: 64px;"
     tb-help-popup="widget/editor/examples/timeseries_widget"
     tb-help-popup-placement="top"
     trigger-style="font-size: 16px;"
     trigger-text="Sample Time-Series widget">
</div>

<br>

<div style="padding-left: 64px;"
     tb-help-popup="widget/editor/examples/rpc_widget"
     tb-help-popup-placement="top"
     trigger-style="font-size: 16px;"
     trigger-text="Sample RPC (Control) widget">
</div>

<br>

<div style="padding-left: 64px;"
     tb-help-popup="widget/editor/examples/alarm_widget"
     tb-help-popup-placement="top"
     trigger-style="font-size: 16px;"
     trigger-text="Sample Alarm widget">
</div>

<br>

<div style="padding-left: 64px;"
     tb-help-popup="widget/editor/examples/static_widget"
     tb-help-popup-placement="top"
     trigger-style="font-size: 16px;"
     trigger-text="Sample Static widget">
</div>

<div class="divider"></div>

##### Integrating existing code to create widget definition

Below are some examples demonstrating how external JavaScript libraries or existing code can be reused/integrated to create new widgets.

###### Using external JavaScript library

<br>

<div style="padding-left: 64px;"
     tb-help-popup="widget/editor/examples/ext_latest_values_example"
     tb-help-popup-placement="top"
     trigger-style="font-size: 16px;"
     trigger-text="Latest Values widget Example with gauge.js library">
</div>

<br>

<div style="padding-left: 64px;"
     tb-help-popup="widget/editor/examples/ext_timeseries_example"
     tb-help-popup-placement="top"
     trigger-style="font-size: 16px;"
     trigger-text="Time-Series widget Example with Chart.js library">
</div>

###### Using existing JavaScript/Typescript code

<br>

<div style="padding-left: 64px;"
     tb-help-popup="widget/editor/widget_js_existing_code"
     tb-help-popup-placement="top"
     trigger-style="font-size: 16px;"
     trigger-text="Read more">
</div>

<div class="divider"></div>

##### Widget code debugging tips

The most simple method of debugging is Web console output.
Just place [**console.log(...)**{:target="_blank"}](https://developer.mozilla.org/en-US/docs/Web/API/Console/log) function inside any part of widget JavaScript code.
Then click **Run** button to restart widget code and observe debug information in the Web console.

Another and most effective method of debugging is to invoke browser debugger.
Put [**debugger;**{:target="_blank"}](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Statements/debugger) statement into the place of widget code you are interested in and then click **Run** button to restart widget code.
Browser debugger (if enabled) will automatically pause code execution at the debugger statement and you will be able to analyze script execution using browser debugging tools.

<div class="divider"></div>

##### Further reading

For more information read  [Widgets Development Guide{:target="_blank"}](${siteBaseUrl}/docs/user-guide/contribution/widgets-development).

<br>
<br>

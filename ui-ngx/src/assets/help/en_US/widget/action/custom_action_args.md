  <li><b>$event:</b> <code><a href="https://developer.mozilla.org/en-US/docs/Web/API/MouseEvent" target="_blank">MouseEvent</a></code> - The <a href="https://developer.mozilla.org/en-US/docs/Web/API/MouseEvent" target="_blank">MouseEvent</a> object. Usually a result of a mouse click event.
  </li>
  <li><b>widgetContext:</b> <code><a href="https://github.com/thingsboard/thingsboard/blob/5bb6403407aa4898084832d6698aa9ea6d484889/ui-ngx/src/app/modules/home/models/widget-component.models.ts#L107" target="_blank">WidgetContext</a></code> - A reference to <a href="https://github.com/thingsboard/thingsboard/blob/5bb6403407aa4898084832d6698aa9ea6d484889/ui-ngx/src/app/modules/home/models/widget-component.models.ts#L107" target="_blank">WidgetContext</a> that has all necessary API 
     and data used by widget instance.
  </li>
  <li><b>entityId:</b> <code>string</code> - An optional string id of the target entity.
  </li>
  <li><b>entityName:</b> <code>string</code> - An optional string name of the target entity.
  </li>
  <li><b>additionalParams:</b> <code>{[key: string]: any}</code> - An optional key/value object holding additional entity parameters.
        <span style="padding-left: 4px;"
             tb-help-popup="widget/action/custom_additional_params"
             tb-help-popup-placement="top"
             [tb-help-popup-style]="{maxHeight: '50vh', maxWidth: '50vw'}"
             trigger-text="Read more">
        </span>
  </li>
  <li><b>entityLabel:</b> <code>string</code> - An optional string label of the target entity.
  </li>

#### Custom action function

<div class="divider"></div>
<br/>

*function ($event, widgetContext, entityId, entityName, additionalParams, entityLabel): void*

A JavaScript function performing custom action.

**Parameters:**

<ul>
  <li><b>$event:</b> <code><a href="https://developer.mozilla.org/en-US/docs/Web/API/MouseEvent" target="_blank">MouseEvent</a></code> - The <a href="https://developer.mozilla.org/en-US/docs/Web/API/MouseEvent" target="_blank">MouseEvent</a> object. Usually a result of a mouse click event.
  </li>
  <li><b>widgetContext:</b> <code><a href="https://github.com/thingsboard/thingsboard/blob/5bb6403407aa4898084832d6698aa9ea6d484889/ui-ngx/src/app/modules/home/models/widget-component.models.ts#L107" target="_blank">WidgetContext</a></code> - A reference to <a href="https://github.com/thingsboard/thingsboard/blob/5bb6403407aa4898084832d6698aa9ea6d484889/ui-ngx/src/app/modules/home/models/widget-component.models.ts#L107" target="_blank">WidgetContext</a> that has all necessary API 
     and data used by widget instance.
  </li>
  <li><b>entityId:</b> <code>string</code> - An optional string id of the target entity.
  </li>
  <li><b>entityName:</b> <code>string</code> - An optional string name of the target entity.
  </li>
  {% include widget/action/custom_additional_params %}
  <li><b>entityLabel:</b> <code>string</code> - An optional string label of the target entity.
  </li>
</ul>

<div class="divider"></div>

##### Examples

* Display alert dialog with entity information:

```javascript
var title;
var content;
if (entityName) {
    title = entityName + ' details';
    content = '<b>Entity name</b>: ' + entityName;
    if (additionalParams && additionalParams.entity) {
        var entity = additionalParams.entity;
        if (entity.id) {
            content += '<br><b>Entity type</b>: ' + entity.id.entityType;
        }
        if (!isNaN(entity.temperature) && entity.temperature !== '') {
            content += '<br><b>Temperature</b>: ' + entity.temperature + ' °C';
        }
    }
} else {
    title = 'No entity information available';
    content = '<b>No entity information available</b>';
}

showAlertDialog(title, content);
 
function showAlertDialog(title, content) {
    setTimeout(function() {
      widgetContext.dialogs.alert(title, content).subscribe();
    }, 100);
}
{:copy-code}
```

* Delete device after confirmation:

```javascript
var $injector = widgetContext.$scope.$injector;
var dialogs = $injector.get(widgetContext.servicesMap.get('dialogs'));
var deviceService = $injector.get(widgetContext.servicesMap.get('deviceService'));

openDeleteDeviceDialog();

function openDeleteDeviceDialog() {
  var title = 'Are you sure you want to delete the device ' + entityName + '?';
  var content = 'Be careful, after the confirmation, the device and all related data will become unrecoverable!';
  dialogs.confirm(title, content, 'Cancel', 'Delete').subscribe(
    function(result) {
      if (result) {
        deleteDevice();
      }
    }
  );
}

function deleteDevice() {
  deviceService.deleteDevice(entityId.id).subscribe(
    function() {
      widgetContext.updateAliases();
    }
  );
}
{:copy-code}
```

#### Custom action function

<div class="divider"></div>
<br/>

*function ($event, widgetContext, entityId, entityName, additionalParams, entityLabel): void*

A JavaScript function performing custom action.

**Parameters:**

<ul>
  {% include widget/action/custom_action_args %}
</ul>

<div class="divider"></div>

##### Examples

* Display alert dialog with entity information:

```javascript
{:code-style="max-height: 300px;"}
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
{:code-style="max-height: 300px;"}
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

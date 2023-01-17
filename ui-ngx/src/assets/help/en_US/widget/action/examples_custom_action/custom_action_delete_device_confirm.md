#### Function delete device after confirmation

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

<br>
<br>

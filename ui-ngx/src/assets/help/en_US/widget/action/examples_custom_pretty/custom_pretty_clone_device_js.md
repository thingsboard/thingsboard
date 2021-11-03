#### Function displaying dialog to clone device

```javascript
{:code-style="max-height: 400px;"}
const $injector = widgetContext.$scope.$injector;
const customDialog = $injector.get(widgetContext.servicesMap.get('customDialog'));
const attributeService = $injector.get(widgetContext.servicesMap.get('attributeService'));
const deviceService = $injector.get(widgetContext.servicesMap.get('deviceService'));
const rxjs = widgetContext.rxjs;

openCloneDeviceDialog();

function openCloneDeviceDialog() {
    customDialog.customDialog(htmlTemplate, CloneDeviceDialogController).subscribe();
}

function CloneDeviceDialogController(instance) {
    let vm = instance;
    vm.deviceName = entityName;

    vm.cloneDeviceFormGroup = vm.fb.group({
        cloneName: ['', [vm.validators.required]]
    });

    vm.save = function() {
        deviceService.getDevice(entityId.id).pipe(
            rxjs.mergeMap((origDevice) => {
                let cloneDevice = {
                    name: vm.cloneDeviceFormGroup.get('cloneName').value,
                    type: origDevice.type
                };
                return deviceService.saveDevice(cloneDevice).pipe(
                    rxjs.mergeMap((newDevice) => {
                        return attributeService.getEntityAttributes(origDevice.id, 'SERVER_SCOPE').pipe(
                            rxjs.mergeMap((origAttributes) => {
                                return attributeService.saveEntityAttributes(newDevice.id, 'SERVER_SCOPE', origAttributes);
                            })
                        );
                    })
                );
            })
        ).subscribe(() => {
            widgetContext.updateAliases();
            vm.dialogRef.close(null);
        });
    };

    vm.cancel = function() {
        vm.dialogRef.close(null);
    };
}
{:copy-code}
```

<br>
<br>

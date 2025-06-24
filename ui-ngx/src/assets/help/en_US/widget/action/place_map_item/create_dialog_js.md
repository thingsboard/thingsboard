#### Function displaying dialog to create a device or an asset

```javascript
{:code-style="max-height: 400px;"}
let $injector = widgetContext.$scope.$injector;
let customDialog = $injector.get(widgetContext.servicesMap.get('customDialog'));
let assetService = $injector.get(widgetContext.servicesMap.get('assetService'));
let deviceService = $injector.get(widgetContext.servicesMap.get('deviceService'));
let attributeService = $injector.get(widgetContext.servicesMap.get('attributeService'));

openAddEntityDialog();

function openAddEntityDialog() {
  customDialog.customDialog(htmlTemplate, AddEntityDialogController).subscribe();
}

function AddEntityDialogController(instance) {
  let vm = instance;

  vm.allowedEntityTypes = ['ASSET', 'DEVICE'];

  vm.addEntityFormGroup = vm.fb.group({
    entityName: ['', [vm.validators.required]],
    entityType: ['DEVICE'],
    entityLabel: [null],
    type: ['', [vm.validators.required]],
    attributes: vm.fb.group({
      address: [null],
      owner: [null]
    })
  });

  vm.cancel = function() {
    vm.dialogRef.close(null);
  };

  vm.save = function() {
    vm.addEntityFormGroup.markAsPristine();
    saveEntityObservable().pipe(
      widgetContext.rxjs.switchMap((entity) => saveAttributes(entity.id))
    ).subscribe(() => {
      widgetContext.updateAliases();
      vm.dialogRef.close(null);
    });
  };

  function saveEntityObservable() {
    const formValues = vm.addEntityFormGroup.value;
    let entity = {
      name: formValues.entityName,
      type: formValues.type,
      label: formValues.entityLabel
    };
    if (formValues.entityType == 'ASSET') {
      return assetService.saveAsset(entity);
    } else if (formValues.entityType == 'DEVICE') {
      return deviceService.saveDevice(entity);
    }
  }

  function saveAttributes(entityId) {
    let attributes = vm.addEntityFormGroup.get('attributes').value;
    let attributesArray = getMapItemLocationAttributes();
    for (let key in attributes) {
      if(attributes[key] !== null) {
        attributesArray.push({key: key, value: attributes[key]});
      }
    }
    if (attributesArray.length > 0) {
      return attributeService.saveEntityAttributes(entityId, "SERVER_SCOPE", attributesArray);
    }
    return widgetContext.rxjs.of([]);
  }

  function getMapItemLocationAttributes() {
    const attributes = [];
    const mapItemType = $event.shape;
    if (mapItemType === 'Marker') {
      const mapType = widgetContext.mapInstance.type();
      attributes.push({key: mapType === 'image' ? 'xPos' : 'latitude', value: additionalParams.coordinates.x});
      attributes.push({key: mapType === 'image' ? 'yPos' : 'longitude', value: additionalParams.coordinates.y});
    } else if (mapItemType === 'Rectangle' || mapItemType === 'Polygon') {
      attributes.push({key: 'perimeter', value: additionalParams.coordinates});
    } else if (mapItemType === 'Circle') {
      attributes.push({key: 'circle', value: additionalParams.coordinates});
    }
    return attributes;
  }
}
{:copy-code}
```

<br>
<br>

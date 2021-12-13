#### Function displaying dialog to create a device or an asset

```javascript
{:code-style="max-height: 400px;"}
let $injector = widgetContext.$scope.$injector;
let customDialog = $injector.get(widgetContext.servicesMap.get('customDialog'));
let assetService = $injector.get(widgetContext.servicesMap.get('assetService'));
let deviceService = $injector.get(widgetContext.servicesMap.get('deviceService'));
let attributeService = $injector.get(widgetContext.servicesMap.get('attributeService'));
let entityRelationService = $injector.get(widgetContext.servicesMap.get('entityRelationService'));

openAddEntityDialog();

function openAddEntityDialog() {
  customDialog.customDialog(htmlTemplate, AddEntityDialogController).subscribe();
}

function AddEntityDialogController(instance) {
  let vm = instance;

  vm.allowedEntityTypes = ['ASSET', 'DEVICE'];
  vm.entitySearchDirection = {
    from: "FROM",
    to: "TO"
  }

  vm.addEntityFormGroup = vm.fb.group({
    entityName: ['', [vm.validators.required]],
    entityType: ['DEVICE'],
    entityLabel: [null],
    type: ['', [vm.validators.required]],
    attributes: vm.fb.group({
      latitude: [null],
      longitude: [null],
      address: [null],
      owner: [null],
      number: [null, [vm.validators.pattern(/^-?[0-9]+$/)]],
      booleanValue: [null]
    }),
    relations: vm.fb.array([])
  });

  vm.cancel = function () {
    vm.dialogRef.close(null);
  };

  vm.relations = function () {
    return vm.addEntityFormGroup.get('relations');
  };

  vm.addRelation = function () {
    vm.relations().push(vm.fb.group({
      relatedEntity: [null, [vm.validators.required]],
      relationType: [null, [vm.validators.required]],
      direction: [null, [vm.validators.required]]
    }));
  };

  vm.removeRelation = function (index) {
    vm.relations().removeAt(index);
    vm.relations().markAsDirty();
  };

  vm.save = function () {
    vm.addEntityFormGroup.markAsPristine();
    saveEntityObservable().subscribe(
      function (entity) {
        widgetContext.rxjs.forkJoin([
          saveAttributes(entity.id),
          saveRelations(entity.id)
        ]).subscribe(
          function () {
            widgetContext.updateAliases();
            vm.dialogRef.close(null);
          }
        );
      }
    );
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
    let attributesArray = [];
    for (let key in attributes) {
      if (attributes[key] !== null) {
        attributesArray.push({key: key, value: attributes[key]});
      }
    }
    if (attributesArray.length > 0) {
      return attributeService.saveEntityAttributes(entityId, "SERVER_SCOPE", attributesArray);
    }
    return widgetContext.rxjs.of([]);
  }

  function saveRelations(entityId) {
    let relations = vm.addEntityFormGroup.get('relations').value;
    let tasks = [];
    for (let i = 0; i < relations.length; i++) {
      let relation = {
        type: relations[i].relationType,
        typeGroup: 'COMMON'
      };
      if (relations[i].direction == 'FROM') {
        relation.to = relations[i].relatedEntity;
        relation.from = entityId;
      } else {
        relation.to = entityId;
        relation.from = relations[i].relatedEntity;
      }
      tasks.push(entityRelationService.saveRelation(relation));
    }
    if (tasks.length > 0) {
      return widgetContext.rxjs.forkJoin(tasks);
    }
    return widgetContext.rxjs.of([]);
  }
}
{:copy-code}
```

<br>
<br>

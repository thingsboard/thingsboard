#### Function displaying dialog to edit a device or an asset

```javascript
{:code-style="max-height: 400px;"}
let $injector = widgetContext.$scope.$injector;
let customDialog = $injector.get(widgetContext.servicesMap.get('customDialog'));
let entityService = $injector.get(widgetContext.servicesMap.get('entityService'));
let assetService = $injector.get(widgetContext.servicesMap.get('assetService'));
let deviceService = $injector.get(widgetContext.servicesMap.get('deviceService'));
let attributeService = $injector.get(widgetContext.servicesMap.get('attributeService'));
let entityRelationService = $injector.get(widgetContext.servicesMap.get('entityRelationService'));

openEditEntityDialog();

function openEditEntityDialog() {
  customDialog.customDialog(htmlTemplate, EditEntityDialogController).subscribe();
}

function EditEntityDialogController(instance) {
  let vm = instance;

  vm.entityName = entityName;
  vm.entityType = entityId.entityType;
  vm.entitySearchDirection = {
    from: "FROM",
    to: "TO"
  };
  vm.attributes = {};
  vm.oldRelationsData = [];
  vm.relationsToDelete = [];
  vm.entity = {};

  vm.editEntityFormGroup = vm.fb.group({
    entityName: ['', [vm.validators.required]],
    entityType: [null],
    entityLabel: [null],
    type: ['', [vm.validators.required]],
    attributes: vm.fb.group({
      latitude: [null],
      longitude: [null],
      address: [null],
      owner: [null],
      number: [null, [vm.validators.pattern(/^-?[0-9]+$/)]],
      booleanValue: [false]
    }),
    oldRelations: vm.fb.array([]),
    relations: vm.fb.array([])
  });

  getEntityInfo();

  vm.cancel = function() {
    vm.dialogRef.close(null);
  };

  vm.relations = function() {
    return vm.editEntityFormGroup.get('relations');
  };

  vm.oldRelations = function() {
    return vm.editEntityFormGroup.get('oldRelations');
  };

  vm.addRelation = function() {
    vm.relations().push(vm.fb.group({
      relatedEntity: [null, [vm.validators.required]],
      relationType: [null, [vm.validators.required]],
      direction: [null, [vm.validators.required]]
    }));
  };

  function addOldRelation() {
    vm.oldRelations().push(vm.fb.group({
      relatedEntity: [{value: null, disabled: true}, [vm.validators.required]],
      relationType: [{value: null, disabled: true}, [vm.validators.required]],
      direction: [{value: null, disabled: true}, [vm.validators.required]]
    }));
  }

  vm.removeRelation = function(index) {
    vm.relations().removeAt(index);
    vm.relations().markAsDirty();
  };

  vm.removeOldRelation = function(index) {
    vm.oldRelations().removeAt(index);
    vm.relationsToDelete.push(vm.oldRelationsData[index]);
    vm.oldRelations().markAsDirty();
  };

  vm.save = function() {
    vm.editEntityFormGroup.markAsPristine();
    widgetContext.rxjs.forkJoin([
      saveAttributes(entityId),
      saveRelations(entityId),
      saveEntity()
    ]).subscribe(
      function () {
        widgetContext.updateAliases();
        vm.dialogRef.close(null);
      }
    );
  };

  function getEntityAttributes(attributes) {
    for (var i = 0; i < attributes.length; i++) {
      vm.attributes[attributes[i].key] = attributes[i].value;
    }
  }

  function getEntityRelations(relations) {
    let relationsFrom = relations[0];
    let relationsTo = relations[1];
    for (let i=0; i < relationsFrom.length; i++) {
      let relation = {
        direction: 'FROM',
        relationType: relationsFrom[i].type,
        relatedEntity: relationsFrom[i].to
      };
      vm.oldRelationsData.push(relation);
      addOldRelation();
    }
    for (let i=0; i < relationsTo.length; i++) {
      let relation = {
        direction: 'TO',
        relationType: relationsTo[i].type,
        relatedEntity: relationsTo[i].from
      };
      vm.oldRelationsData.push(relation);
      addOldRelation();
    }
  }

  function getEntityInfo() {
    widgetContext.rxjs.forkJoin([
      entityRelationService.findInfoByFrom(entityId),
      entityRelationService.findInfoByTo(entityId),
      attributeService.getEntityAttributes(entityId, 'SERVER_SCOPE'),
      entityService.getEntity(entityId.entityType, entityId.id)
    ]).subscribe(
      function (data) {
        getEntityRelations(data.slice(0,2));
        getEntityAttributes(data[2]);
        vm.entity = data[3];
        vm.editEntityFormGroup.patchValue({
          entityName: vm.entity.name,
          entityType: vm.entityType,
          entityLabel: vm.entity.label,
          type: vm.entity.type,
          attributes: vm.attributes,
          oldRelations: vm.oldRelationsData
        }, {emitEvent: false});
      }
    );
  }

  function saveEntity() {
    const formValues = vm.editEntityFormGroup.value;
    if (vm.entity.label !== formValues.entityLabel){
      vm.entity.label = formValues.entityLabel;
      if (formValues.entityType == 'ASSET') {
        return assetService.saveAsset(vm.entity);
      } else if (formValues.entityType == 'DEVICE') {
        return deviceService.saveDevice(vm.entity);
      }
    }
    return widgetContext.rxjs.of([]);
  }

  function saveAttributes(entityId) {
    let attributes = vm.editEntityFormGroup.get('attributes').value;
    let attributesArray = [];
    for (let key in attributes) {
      if (attributes[key] !== vm.attributes[key]) {
        attributesArray.push({key: key, value: attributes[key]});
      }
    }
    if (attributesArray.length > 0) {
      return attributeService.saveEntityAttributes(entityId, "SERVER_SCOPE", attributesArray);
    }
    return widgetContext.rxjs.of([]);
  }

  function saveRelations(entityId) {
    let relations = vm.editEntityFormGroup.get('relations').value;
    let tasks = [];
    for(let i=0; i < relations.length; i++) {
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
    for (let i=0; i < vm.relationsToDelete.length; i++) {
      let relation = {
        type: vm.relationsToDelete[i].relationType
      };
      if (vm.relationsToDelete[i].direction == 'FROM') {
        relation.to = vm.relationsToDelete[i].relatedEntity;
        relation.from = entityId;
      } else {
        relation.to = entityId;
        relation.from = vm.relationsToDelete[i].relatedEntity;
      }
      tasks.push(entityRelationService.deleteRelation(relation.from, relation.type, relation.to));
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

#### Function displaying dialog to add/edit image in entity attribute

```javascript
{:code-style="max-height: 400px;"}
let $injector = widgetContext.$scope.$injector;
let customDialog = $injector.get(widgetContext.servicesMap.get('customDialog'));
let assetService = $injector.get(widgetContext.servicesMap.get('assetService'));
let attributeService = $injector.get(widgetContext.servicesMap.get('attributeService'));
let entityService = $injector.get(widgetContext.servicesMap.get('entityService'));

openAddEntityDialog();

function openAddEntityDialog() {
  customDialog.customDialog(htmlTemplate, AddEntityDialogController).subscribe(() => {});
}

function AddEntityDialogController(instance) {
  let vm = instance;

  vm.entityName = entityName;

  vm.attributes = {};

  vm.editEntity = vm.fb.group({
    attributes: vm.fb.group({
      image: [null]
    })
  });

  getEntityInfo();

  vm.cancel = function() {
    vm.dialogRef.close(null);
  };

  vm.save = function() {
    vm.loading = true;
    saveAttributes(entityId).subscribe(
      () => {
        vm.dialogRef.close(null);
      }, () =>{
        vm.loading = false;
      }
    );
  };

  function getEntityAttributes(attributes) {
    for (var i = 0; i < attributes.length; i++) {
      vm.attributes[attributes[i].key] = attributes[i].value;
    }
  }

  function getEntityInfo() {
    vm.loading = true;
    attributeService.getEntityAttributes(entityId, 'SERVER_SCOPE').subscribe(
      function (data) {
        getEntityAttributes(data);

        vm.editEntity.patchValue({
          attributes: vm.attributes
        }, {emitEvent: false});
        vm.loading = false;
      }
    );
  }

  function saveAttributes(entityId) {
    let attributes = vm.editEntity.get('attributes').value;
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
}
{:copy-code}
```

<br>
<br>

#### Function delete Entity after confirmation

```javascript
var dialogs = $injector.get(servicesMap.get('dialogs'));

openDeleteEntityDialog();

function openDeleteEntityDialog() {
  var title = 'Are you sure you want to delete entity ' + entityName + '?';
  var content = 'Be careful, after the confirmation, entity and all related data will become unrecoverable!';
  dialogs.confirm(title, content, 'Cancel', 'Delete').subscribe(
    function(result) {
      if (result) {
        deleteEntity();
      }
    }
  );
}

function deleteEntity() {
  tableConfig.entitiesTableConfig.deleteEntity(entityId).subscribe(
    function() {
      tableConfig.updateData();
    }
  );
}
{:copy-code}
```

<br>
<br>

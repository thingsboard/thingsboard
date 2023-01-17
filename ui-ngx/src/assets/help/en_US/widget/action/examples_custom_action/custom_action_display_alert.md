#### Function display alert dialog with entity information

```javascript
{:code-style="max-height: 400px;"}
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

<br>
<br>

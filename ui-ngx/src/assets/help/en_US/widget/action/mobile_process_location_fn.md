#### Process location function

<div class="divider"></div>
<br/>

*function processLocation(latitude, longitude, $event, widgetContext, entityId, entityName, additionalParams, entityLabel): void*

A JavaScript function to process current location of the phone.

**Parameters:**

<ul>
  <li><b>latitude:</b> <code>number</code> - phone location latitude.
  </li>
  <li><b>longitude:</b> <code>number</code> - phone location longitude.
  </li>
  {% include widget/action/custom_action_args %}
</ul>

<div class="divider"></div>

##### Examples

* Display alert dialog with location data:

```javascript
showLocationDialog('Location', latitude, longitude);

function showLocationDialog(title, latitude, longitude) {
  setTimeout(function() {
    widgetContext.dialogs.alert(title, 'Latitude: '+latitude+'<br>Longitude: ' + longitude).subscribe();
  }, 100);
}
{:copy-code}
```

* Store phone location to entity attributes:

```javascript
saveEntityLocationAttributes('latitude', 'longitude', latitude, longitude);

function saveEntityLocationAttributes(latitudeAttributeName, longitudeAttributeName, latitude, longitude) {
  if (entityId) {
    let attributes = [
      { key: latitudeAttributeName, value: latitude },
      { key: longitudeAttributeName, value: longitude }
    ];
    widgetContext.attributeService.saveEntityAttributes(entityId, "SERVER_SCOPE", attributes).subscribe(
      function() {
        widgetContext.showSuccessToast('Location attributes saved!');
      },
      function(error) {
        widgetContext.dialogs.alert('Location attributes save failed', JSON.stringify(error));
      }
    );
  }
}
{:copy-code}
```

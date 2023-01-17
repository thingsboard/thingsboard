#### Get location function

<div class="divider"></div>
<br/>

*function getLocation($event, widgetContext, entityId, entityName, additionalParams, entityLabel): [number, number] | Observable<[number, number]>*

A JavaScript function that should return location as array of two numbers (latitude, longitude) for further processing by mobile action.<br>
Usually location can be obtained from entity attributes/telemetry.

**Parameters:**

<ul>
  {% include widget/action/custom_action_args %}
</ul>

**Returns:**

Latitude and longitude as array of two numbers or Observable of array of two numbers. For example ```[37.689, -122.433]```.

<div class="divider"></div>

##### Examples

* Return location from entity attributes:

```javascript
return getLocationFromEntityAttributes();

function getLocationFromEntityAttributes() {
  if (entityId) {
    return widgetContext.attributeService.getEntityAttributes(entityId, 'SERVER_SCOPE', ['latitude', 'longitude'])
      .pipe(widgetContext.rxjs
        .map(function(attributeData) {
                var res = [0,0];
                if (attributeData && attributeData.length === 2) {
                  res[0] = attributeData.filter(function (data) { return data.key === 'latitude'})[0].value;
                  res[1] = attributeData.filter(function (data) { return data.key === 'longitude'})[0].value;
                }
                return res;
            }
         )
      );
  } else {
    return [0,0];
  }
}
{:copy-code}
```

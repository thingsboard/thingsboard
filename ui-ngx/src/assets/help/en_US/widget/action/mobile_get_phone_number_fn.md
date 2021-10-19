#### Get phone number function

<div class="divider"></div>
<br/>

*function getPhoneNumber($event, widgetContext, entityId, entityName, additionalParams, entityLabel): number | string | Observable&lt;number&gt; | Observable&lt;string&gt;*

A JavaScript function that should return phone number for further processing by mobile action.<br>
Usually phone number can be obtained from entity attributes/telemetry.

**Parameters:**

<ul>
  {% include widget/action/custom_action_args %}
</ul>

**Returns:**

String or numeric value of phone number or Observable of string or numeric value. For example ```123456789```.

<div class="divider"></div>

##### Examples

* Return phone number from entity attributes:

```javascript
return getPhoneNumberFromEntityAttributes();

function getPhoneNumberFromEntityAttributes() {
  if (entityId) {
    return widgetContext.attributeService.getEntityAttributes(entityId, 'SERVER_SCOPE', ['phone'])
      .pipe(widgetContext.rxjs
        .map(function(attributeData) {
                var res = 0;
                if (attributeData && attributeData.length === 1) {
                  res = attributeData[0].value;
                }
                return res;
            }
         )
      );
  } else {
    return 0;
  }
}
{:copy-code}
```

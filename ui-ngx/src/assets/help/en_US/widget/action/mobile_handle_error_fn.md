#### Handle error function

<div class="divider"></div>
<br/>

*function handleError(error, $event, widgetContext, entityId, entityName, additionalParams, entityLabel): void*

An optional JavaScript function to handle error occurred while mobile action execution.

**Parameters:**

<ul>
  <li><b>error:</b> <code>string</code> - error message.
  </li>
  {% include widget/action/custom_action_args %}
</ul>

<div class="divider"></div>

##### Examples

* Display alert dialog with error message:

```javascript
showErrorDialog('Failed to perform action', error);

function showErrorDialog(title, error) {
  setTimeout(function() {
    widgetContext.dialogs.alert(title, error).subscribe();
  }, 100);
}
{:copy-code}
```

#### Handle empty result function

<div class="divider"></div>
<br/>

*function handleEmptyResult($event, widgetContext, entityId, entityName, additionalParams, entityLabel): void*

An optional JavaScript function to handle empty result.<br>Usually this happens when user cancels the action (for ex. by pressing phone back button).

**Parameters:**

<ul>
  {% include widget/action/custom_action_args %}
</ul>

<div class="divider"></div>

##### Examples

* Display alert dialog with canceled action message:

```javascript
showEmptyResultDialog('Action was canceled!');

function showEmptyResultDialog(message) {
    setTimeout(function() {
        widgetContext.dialogs.alert('Empty result', message).subscribe();
    }, 100);
}
{:copy-code}
```

#### Process launch result function

<div class="divider"></div>
<br/>

*function processLaunchResult(launched, $event, widgetContext, entityId, entityName, additionalParams, entityLabel): void*

An optional JavaScript function to process result of attempt to launch external mobile application (for ex. map application or phone call application).

**Parameters:**

<ul>
  <li><b>launched:</b> <code>boolean</code> - boolean value indicating if the external application was successfully launched.
  {% include widget/action/custom_action_args %}
</ul>

<div class="divider"></div>

##### Examples

* Display alert dialog with external application launch status:

```javascript
showLaunchStatusDialog('Application', launched);

function showLaunchStatusDialog(title, status) {
  setTimeout(function() {
    widgetContext.dialogs.alert(title, status ? 'Successfully launched' : 'Failed to launch').subscribe();
  }, 100);
}

{:copy-code}
```

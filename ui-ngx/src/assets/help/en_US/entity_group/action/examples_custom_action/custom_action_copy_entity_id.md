#### Function copy Entity Id to buffer

```javascript
{:code-style="max-height: 400px;"}
var ActionNotificationShow = servicesMap.get('actionNotificationShow');
var store = $injector.get(servicesMap.get('store'));

if (copyToClipboard(entityId.id)) {
  store.dispatch(new ActionNotificationShow({
    message: 'Entity Id has been copied',
    type: 'success',
    duration: 750,
    verticalPosition: 'top',
    horizontalPosition: 'left'
  }));
}

function copyToClipboard(text) {
  if (window.clipboardData && window.clipboardData.setData) {
    return window.clipboardData.setData("Text", text);
  }
  else if (document.queryCommandSupported && document.queryCommandSupported("copy")) {
    var textarea = document.createElement("textarea");
    textarea.textContent = text;
    textarea.style.position = "fixed";
    document.body.appendChild(textarea);
    textarea.select();
    try {
      return document.execCommand("copy");
    }
    catch (ex) {
      console.warn("Copy to clipboard failed.", ex);
      return false;
    }
    document.body.removeChild(textarea);
  }
}
{:copy-code}
```

<br>
<br>

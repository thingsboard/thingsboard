#### Function copy device access token to buffer

```javascript
{:code-style="max-height: 400px;"}
var $injector = widgetContext.$scope.$injector;
var deviceService = $injector.get(widgetContext.servicesMap.get('deviceService'));
var $translate = $injector.get(widgetContext.servicesMap.get('translate'));
var $scope = widgetContext.$scope;
if (entityId.id && entityId.entityType === 'DEVICE') {
  deviceService.getDeviceCredentials(entityId.id, true).subscribe(
    (deviceCredentials) => {
      var credentialsId = deviceCredentials.credentialsId;
      if (copyToClipboard(credentialsId)) {
        $scope.showSuccessToast($translate.instant('device.accessTokenCopiedMessage'), 750, "top", "left");
      }
    }
  );
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

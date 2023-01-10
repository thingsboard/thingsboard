#### Process QR code function

<div class="divider"></div>
<br/>

*function processQrCode(code, format, $event, widgetContext, entityId, entityName, additionalParams, entityLabel): void*

A JavaScript function to process result of barcode scanning.

**Parameters:**

<ul>
  <li><b>code:</b> <code>string</code> - A string value of scanned barcode.
  </li>
  <li><b>format:</b> <code>string</code> - barcode format. See <a href="https://github.com/juliuscanute/qr_code_scanner/blob/c89f1eaddb94cca705d7e602a0c326e271680bf4/lib/src/types/barcode_format.dart#L1" target="_blank">BarcodeFormat</a> enum for possible values.
  </li>
  {% include widget/action/custom_action_args %}
</ul>

<div class="divider"></div>

##### Examples

* Display alert dialog with scanned barcode:

```javascript
showQrCodeDialog('Bar Code', code, format);

function showQrCodeDialog(title, code, format) {
  setTimeout(function() {
    widgetContext.dialogs.alert(title, 'Code: ['+code+']<br>Format: ' + format).subscribe();
  }, 100);
}
{:copy-code}
```

* Parse code as a device claiming info (in this case ```{deviceName: string, secretKey: string}```)<br>and then claim device (see [Claiming devices{:target="_blank"}](${siteBaseUrl}/docs/user-guide/claiming-devices/) for details):

```javascript
var $scope = widgetContext.$scope;
var $injector = $scope.$injector;
var $translate = $injector.get(widgetContext.servicesMap.get('translate'));
var deviceService = $injector.get(widgetContext.servicesMap.get('deviceService'));
var deviceNotFound = $translate.instant('widgets.input-widgets.claim-not-found');
var failedClaimDevice = $translate.instant('widgets.input-widgets.claim-failed');
var claimDeviceInfo = JSON.parse(code);
var deviceName = claimDeviceInfo.deviceName;
var secretKey = claimDeviceInfo.secretKey;
var claimRequest = {
  secretKey: secretKey
};
deviceService.claimDevice(deviceName, claimRequest, { ignoreErrors: true }).subscribe(
  function (data) {
    widgetContext.showSuccessToast('Device \'' + deviceName + '\' successfully claimed!');
    widgetContext.updateAliases();
  },
  function (error) {
    if(error.status == 404) {
      widgetContext.showErrorToast(deviceNotFound);
    } else {
      if (error.status !== 400 && error.error && error.error.message) {
        showDialog('Failed to claim device', error.error.message);
      } else {
        widgetContext.showErrorToast(failedClaimDevice);
      }
    }
  }
);

function showDialog(title, error) {
  setTimeout(function() {
    widgetContext.dialogs.alert(title, error).subscribe();
  }, 100);
}
{:copy-code}
```

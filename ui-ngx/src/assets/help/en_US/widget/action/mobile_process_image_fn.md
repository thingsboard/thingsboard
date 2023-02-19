#### Process image function

<div class="divider"></div>
<br/>

*function processImage(imageUrl, $event, widgetContext, entityId, entityName, additionalParams, entityLabel): void*

A JavaScript function to process image obtained as a result of mobile action (take photo, take image from gallery, etc.).

**Parameters:**

<ul>
  <li><b>imageUrl:</b> <code>string</code> - An image URL in base64 data format.
  </li>
  {% include widget/action/custom_action_args %}
</ul>

<div class="divider"></div>

##### Examples

* Store image url data to entity attribute:

```javascript
saveEntityImageAttribute('image', imageUrl);

function saveEntityImageAttribute(attributeName, imageUrl) {
  if (entityId) {
    let attributes = [{
      key: attributeName, value: imageUrl
    }];
    widgetContext.attributeService.saveEntityAttributes(entityId, "SERVER_SCOPE", attributes).subscribe(
      function() {
        widgetContext.showSuccessToast('Image attribute saved!');
      },
      function(error) {
        widgetContext.dialogs.alert('Image attribute save failed', JSON.stringify(error));
      }
    );
  }
}
{:copy-code}
```

* Display dialog with obtained image:

```javascript
showImageDialog('Image', imageUrl);

function showImageDialog(title, imageUrl) {
  setTimeout(function() {
    widgetContext.customDialog.customDialog(imageDialogTemplate, ImageDialogController, {imageUrl: imageUrl, title: title}).subscribe();
  }, 100);
}

var imageDialogTemplate =
    '<div aria-label="Image">' +
    '<form #theForm="ngForm">' +
    '<mat-toolbar fxLayout="row" color="primary">' +
    '<h2>{{title}}</h2>' +
    '<span fxFlex></span>' +
    '<button mat-icon-button (click)="close()">' +
    '<mat-icon>close</mat-icon>' +
    '</button>' +
    '</mat-toolbar>' +
    '<div mat-dialog-content>' +
    '<div class="mat-content mat-padding">' +
    '<div fxLayout="column" fxFlex>' +
    '<div style="padding-top: 20px;">' +
    '<img [src]="imageUrl" style="height: 300px;"/>' +
    '</div>' +
    '</div>' +
    '</div>' +
    '</div>' +
    '<div mat-dialog-actions fxLayout="row">' +
    '<span fxFlex></span>' +
    '<button mat-button (click)="close()" style="margin-right:20px;">Close</button>' +
    '</div>' +
    '</form>' +
    '</div>';

function ImageDialogController(instance) {
  let vm = instance;
  vm.title = vm.data.title;
  vm.imageUrl = vm.data.imageUrl;
  vm.close = function ()
  {
    vm.dialogRef.close(null);
  }
}
{:copy-code}
```

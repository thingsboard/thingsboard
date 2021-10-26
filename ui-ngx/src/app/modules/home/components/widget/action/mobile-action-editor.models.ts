///
/// Copyright © 2016-2021 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import { WidgetMobileActionType } from '@shared/models/widget.models';

const processImageFunctionTemplate =
  '// Function body to process image obtained as a result of mobile action (take photo, take image from gallery, etc.). \n' +
  '// - imageUrl - image URL in base64 data format\n\n' +
  'showImageDialog(\'--TITLE--\', imageUrl);\n' +
  '//saveEntityImageAttribute(\'image\', imageUrl);\n' +
  '\n' +
  'function showImageDialog(title, imageUrl) {\n' +
  '    setTimeout(function() {\n' +
  '        widgetContext.customDialog.customDialog(imageDialogTemplate, ImageDialogController, {imageUrl: imageUrl, title: title}).subscribe();\n' +
  '    }, 100);\n' +
  '}\n' +
  '\n' +
  'function saveEntityImageAttribute(attributeName, imageUrl) {\n' +
  '    if (entityId) {\n' +
  '        let attributes = [{\n' +
  '            key: attributeName, value: imageUrl\n' +
  '        }];\n' +
  '        widgetContext.attributeService.saveEntityAttributes(entityId, "SERVER_SCOPE", attributes).subscribe(\n' +
  '           function() {\n' +
  '               widgetContext.showSuccessToast(\'Image attribute saved!\');\n' +
  '           },\n' +
  '           function(error) {\n' +
  '               widgetContext.dialogs.alert(\'Image attribute save failed\', JSON.stringify(error));\n' +
  '           }\n' +
  '        );\n' +
  '    }\n' +
  '}\n' +
  '\n' +
  'var\n' +
  '  imageDialogTemplate =\n' +
  '    \'<div aria-label="Image">\' +\n' +
  '    \'<form #theForm="ngForm">\' +\n' +
  '    \'<mat-toolbar fxLayout="row" color="primary">\' +\n' +
  '    \'<h2>{{title}}</h2>\' +\n' +
  '    \'<span fxFlex></span>\' +\n' +
  '    \'<button mat-icon-button (click)="close()">\' +\n' +
  '    \'<mat-icon>close</mat-icon>\' +\n' +
  '    \'</button>\' +\n' +
  '    \'</mat-toolbar>\' +\n' +
  '    \'<div mat-dialog-content>\' +\n' +
  '    \'<div class="mat-content mat-padding">\' +\n' +
  '    \'<div fxLayout="column" fxFlex>\' +\n' +
  '    \'<div style="padding-top: 20px;">\' +\n' +
  '    \'<img [src]="imageUrl" style="height: 300px;"/>\' +\n' +
  '    \'</div>\' +\n' +
  '    \'</div>\' +\n' +
  '    \'</div>\' +\n' +
  '    \'</div>\' +\n' +
  '    \'<div mat-dialog-actions fxLayout="row">\' +\n' +
  '    \'<span fxFlex></span>\' +\n' +
  '    \'<button mat-button (click)="close()" style="margin-right:20px;">Close</button>\' +\n' +
  '    \'</div>\' +\n' +
  '    \'</form>\' +\n' +
  '    \'</div>\';\n' +
  '\n' +
  'function ImageDialogController(instance) {\n' +
  '  let vm = instance;\n' +
  '  vm.title = vm.data.title;\n' +
  '  vm.imageUrl = vm.data.imageUrl;\n' +
  '  vm.close = function ()\n' +
  '  {\n' +
  '    vm.dialogRef.close(null);\n' +
  '  }\n' +
  '}\n';

const processLaunchResultFunctionTemplate =
  '// Optional function body to process result of attempt to launch external mobile application (for ex. map application or phone call application). \n' +
  '// - launched - boolean value indicating if the external application was successfully launched.\n\n' +
  'showLaunchStatusDialog(\'--TITLE--\', launched);\n' +
  '\n' +
  'function showLaunchStatusDialog(title, status) {\n' +
  '    setTimeout(function() {\n' +
  '        widgetContext.dialogs.alert(title, status ? \'Successfully launched\' : \'Failed to launch\').subscribe();\n' +
  '    }, 100);\n' +
  '}\n';

const processQrCodeFunction =
  '// Function body to process result of QR code scanning. \n' +
  '// - code - scanned QR code\n' +
  '// - format - scanned QR code format\n\n' +
  'showQrCodeDialog(\'QR Code\', code, format);\n' +
  '\n' +
  'function showQrCodeDialog(title, code, format) {\n' +
  '    setTimeout(function() {\n' +
  '        widgetContext.dialogs.alert(title, \'Code: [\'+code+\']<br>Format: \' + format).subscribe();\n' +
  '    }, 100);\n' +
  '}\n';

const processLocationFunction =
  '// Function body to process current location of the phone. \n' +
  '// - latitude - phone location latitude\n' +
  '// - longitude - phone location longitude\n\n' +
  'showLocationDialog(\'Location\', latitude, longitude);\n' +
  '// saveEntityLocationAttributes(\'latitude\', \'longitude\', latitude, longitude);\n' +
  '\n' +
  'function saveEntityLocationAttributes(latitudeAttributeName, longitudeAttributeName, latitude, longitude) {\n' +
  '    if (entityId) {\n' +
  '        let attributes = [\n' +
  '            { key: latitudeAttributeName, value: latitude },\n' +
  '            { key: longitudeAttributeName, value: longitude }\n' +
  '        ];\n' +
  '        widgetContext.attributeService.saveEntityAttributes(entityId, "SERVER_SCOPE", attributes).subscribe(\n' +
  '           function() {\n' +
  '               widgetContext.showSuccessToast(\'Location attributes saved!\');\n' +
  '           },\n' +
  '           function(error) {\n' +
  '               widgetContext.dialogs.alert(\'Location attributes save failed\', JSON.stringify(error));\n' +
  '           }\n' +
  '        );\n' +
  '    }\n' +
  '}\n' +
  '\n' +
  '\n' +
  'function showLocationDialog(title, latitude, longitude) {\n' +
  '    setTimeout(function() {\n' +
  '        widgetContext.dialogs.alert(title, \'Latitude: \'+latitude+\'<br>Longitude: \' + longitude).subscribe();\n' +
  '    }, 100);\n' +
  '}';

const handleEmptyResultFunctionTemplate =
  '// Optional function body to handle empty result. \n' +
  '// Usually this happens when user cancels the action (for ex. by pressing phone back button). \n\n' +
  'showEmptyResultDialog(\'--MESSAGE--\');\n' +
  '\n' +
  'function showEmptyResultDialog(message) {\n' +
  '    setTimeout(function() {\n' +
  '        widgetContext.dialogs.alert(\'Empty result\', message).subscribe();\n' +
  '    }, 100);\n' +
  '}\n';

const handleErrorFunctionTemplate =
  '// Optional function body to handle error occurred while mobile action execution \n' +
  '// - error - Error message\n\n' +
  'showErrorDialog(\'--TITLE--\', error);\n' +
  '\n' +
  'function showErrorDialog(title, error) {\n' +
  '    setTimeout(function() {\n' +
  '        widgetContext.dialogs.alert(title, error).subscribe();\n' +
  '    }, 100);\n' +
  '}\n';

const getLocationFunctionTemplate =
  '// Function body that should return location as array of two numbers (latitude, longitude) for further processing by mobile action.\n' +
  '// Usually location can be obtained from entity attributes/telemetry. \n\n' +
  'return getLocationFromEntityAttributes();\n' +
  '//return [30, 30]; // TEST LOCATION\n' +
  '\n' +
  '\n' +
  'function getLocationFromEntityAttributes() {\n' +
  '    if (entityId) {\n' +
  '        return widgetContext.attributeService.getEntityAttributes(entityId, \'SERVER_SCOPE\', [\'latitude\', \'longitude\']).pipe(widgetContext.rxjs.map(function(attributeData) {\n' +
  '            var res = [0,0];\n' +
  '            if (attributeData && attributeData.length === 2) {\n' +
  '                res[0] = attributeData.filter(function (data) { return data.key === \'latitude\'})[0].value;\n' +
  '                res[1] = attributeData.filter(function (data) { return data.key === \'longitude\'})[0].value;\n' +
  '            }\n' +
  '            return res;\n' +
  '        }));\n' +
  '    } else {\n' +
  '        return [0,0];\n' +
  '    }\n' +
  '}\n';

const getPhoneNumberFunctionTemplate =
  '// Function body that should return phone number for further processing by mobile action.\n' +
  '// Usually phone number can be obtained from entity attributes/telemetry. \n\n' +
  'return getPhoneNumberFromEntityAttributes();\n' +
  '//return 123456789; // TEST PHONE NUMBER\n' +
  '\n' +
  '\n' +
  'function getPhoneNumberFromEntityAttributes() {\n' +
  '    if (entityId) {\n' +
  '        return widgetContext.attributeService.getEntityAttributes(entityId, \'SERVER_SCOPE\', [\'phone\']).pipe(widgetContext.rxjs.map(function(attributeData) {\n' +
  '            var res = 0;\n' +
  '            if (attributeData && attributeData.length === 1) {\n' +
  '                res = attributeData[0].value;\n' +
  '            }\n' +
  '            return res;\n' +
  '        }));\n' +
  '    } else {\n' +
  '        return 0;\n' +
  '    }\n' +
  '}\n';

export function getDefaultProcessImageFunction(type: WidgetMobileActionType): string {
  let title;
  switch (type) {
    case WidgetMobileActionType.takePictureFromGallery:
      title = 'Gallery picture';
      break;
    case WidgetMobileActionType.takePhoto:
      title = 'Photo';
      break;
    case WidgetMobileActionType.takeScreenshot:
      title = 'Screenshot';
      break;
  }
  return processImageFunctionTemplate.replace('--TITLE--', title);
}

export function getDefaultProcessLaunchResultFunction(type: WidgetMobileActionType): string {
  let title;
  switch (type) {
    case WidgetMobileActionType.mapLocation:
      title = 'Map location';
      break;
    case WidgetMobileActionType.mapDirection:
      title = 'Map direction';
      break;
    case WidgetMobileActionType.makePhoneCall:
      title = 'Phone call';
      break;
  }
  return processLaunchResultFunctionTemplate.replace('--TITLE--', title);
}

export function getDefaultProcessQrCodeFunction() {
  return processQrCodeFunction;
}

export function getDefaultProcessLocationFunction() {
  return processLocationFunction;
}

export function getDefaultGetLocationFunction() {
  return getLocationFunctionTemplate;
}

export function getDefaultGetPhoneNumberFunction() {
  return getPhoneNumberFunctionTemplate;
}

export function getDefaultHandleEmptyResultFunction(type: WidgetMobileActionType): string {
  let message = 'Mobile action was cancelled!';
  switch (type) {
    case WidgetMobileActionType.takePictureFromGallery:
      message = 'Take picture from gallery action was cancelled!';
      break;
    case WidgetMobileActionType.takePhoto:
      message = 'Take photo action was cancelled!';
      break;
    case WidgetMobileActionType.mapDirection:
      message = 'Open map directions was not invoked!';
      break;
    case WidgetMobileActionType.mapLocation:
      message = 'Open location on map was not invoked!';
      break;
    case WidgetMobileActionType.scanQrCode:
      message = 'Scan QR code action was canceled!';
      break;
    case WidgetMobileActionType.makePhoneCall:
      message = 'Phone call was not invoked!';
      break;
    case WidgetMobileActionType.getLocation:
      message = 'Get location action was canceled!';
      break;
    case WidgetMobileActionType.takeScreenshot:
      message = 'Take screenshot action was cancelled!';
      break;
  }
  return handleEmptyResultFunctionTemplate.replace('--MESSAGE--', message);
}

export function getDefaultHandleErrorFunction(type: WidgetMobileActionType): string {
  let title = 'Mobile action failed';
  switch (type) {
    case WidgetMobileActionType.takePictureFromGallery:
      title = 'Failed to take picture from gallery';
      break;
    case WidgetMobileActionType.takePhoto:
      title = 'Failed to take photo';
      break;
    case WidgetMobileActionType.mapDirection:
      title = 'Failed to open map directions';
      break;
    case WidgetMobileActionType.mapLocation:
      title = 'Failed to open map location';
      break;
    case WidgetMobileActionType.scanQrCode:
      title = 'Failed to scan QR code';
      break;
    case WidgetMobileActionType.makePhoneCall:
      title = 'Failed to make phone call';
      break;
    case WidgetMobileActionType.getLocation:
      title = 'Failed to get phone location';
      break;
    case WidgetMobileActionType.takeScreenshot:
      title = 'Failed to take screenshot';
      break;
  }
  return handleErrorFunctionTemplate.replace('--TITLE--', title);
}

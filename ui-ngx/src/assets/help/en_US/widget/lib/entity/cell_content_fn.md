#### Cell content function

<div class="divider"></div>
<br/>

*function (value, entity, ctx): string*

A JavaScript function used to compute entity cell content HTML depending on entity field value.

**Parameters:**

<ul>
  <li><b>value:</b> <code>any</code> - An entity field value displayed in the cell.
  </li>
  <li><b>entity:</b> <code><a href="https://github.com/thingsboard/thingsboard/blob/e264f7b8ddff05bda85c4833bf497f47f447496e/ui-ngx/src/app/modules/home/components/widget/lib/table-widget.models.ts#L61" target="_blank">EntityData</a></code> - An 
            <a href="https://github.com/thingsboard/thingsboard/blob/e264f7b8ddff05bda85c4833bf497f47f447496e/ui-ngx/src/app/modules/home/components/widget/lib/table-widget.models.ts#L61" target="_blank">EntityData</a> object
            presenting basic entity properties (ex. <code>id</code>, <code>entityName</code>) and <br> provides access to other entity attributes/timeseries declared in widget datasource configuration.
  </li>
  <li><b>ctx:</b> <code><a href="https://github.com/thingsboard/thingsboard/blob/5bb6403407aa4898084832d6698aa9ea6d484889/ui-ngx/src/app/modules/home/models/widget-component.models.ts#L107" target="_blank">WidgetContext</a></code> - A reference to <a href="https://github.com/thingsboard/thingsboard/blob/5bb6403407aa4898084832d6698aa9ea6d484889/ui-ngx/src/app/modules/home/models/widget-component.models.ts#L107" target="_blank">WidgetContext</a> that has all necessary API 
     and data used by widget instance.
  </li>
</ul>

**Returns:**

Should return string value presenting cell content HTML.

<div class="divider"></div>

##### Examples

* Format entity created time using date/time pattern:

```javascript
var createdTime = value;
return createdTime ? ctx.date.transform(createdTime, 'yyyy-MM-dd HH:mm:ss') : '';
{:copy-code}
```

* Styled cell content for device type field:

```javascript
var deviceType = value;
var color = '#fff';
switch (deviceType) {
  case 'thermostat':
    color = 'orange';
    break;
  case 'default':
    color = '#abab00';
    break;
}
return '<div style="border: 2px solid #0072ff; ' +
  'border-radius: 10px; padding: 5px; ' +
  'background-color: '+ color +'; ' +
  'text-align: center;">' + deviceType + '</div>';
{:copy-code}
```

* Colored circles instead of boolean value:

```javascript
var color;
var active = value;
if (active == 'true') { // all key values here are strings
  color = '#27AE60';
} else {
  color = '#EB5757';
}
return '<span style="font-size: 18px; color: ' + color + '">&#11044;</span>';
{:copy-code}
```

* Decimal value format (1196 => 1,196.0):

```javascript
var value = value / 1;
function numberWithCommas(x) {
  return x.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ",");
}
return value ? numberWithCommas(value.toFixed(1)) : '';
{:copy-code}
```

* Show device status and icon this :

```javascript
{:code-style="max-height: 200px; max-width: 850px;"}
function getIcon(value) {
  if (value == 'QUEUED') {
    return '<mat-icon class="mat-icon material-icons mat-icon-no-color" data-mat-icon-type="font" style="color: #000;">' + 
      '<svg style="width:24px;height:24px" viewBox="0 0 24 24">' + 
      '<path fill="currentColor" d="M6,2V8H6V8L10,12L6,16V16H6V22H18V16H18V16L14,12L18,8V8H18V2H6M16,16.5V20H8V16.5L12,12.5L16,16.5M12,11.5L8,7.5V4H16V7.5L12,11.5Z" />' + 
      '</svg>' + 
    '</mat-icon>';
  }
  if (value == 'UPDATED' ) {
    return '<mat-icon class="mat-icon notranslate material-icons mat-icon-no-color" data-mat-icon-type="font" style="color: #000">' +
      'update' + 
    '</mat-icon>';
  }
  return '';
}
function capitalize (s) {
  if (typeof s !== 'string') return '';
  return s.charAt(0).toUpperCase() + s.slice(1).toLowerCase();
}
var status = value;
return getIcon(status) + '<span style="vertical-align: super;padding-left: 8px;">' + capitalize(status) + '</span>';
{:copy-code}
```

* Display device attribute value on progress bar:

```javascript
{:code-style="max-height: 200px; max-width: 850px;"}
var progress = value;
if (value !== '') {
  return `<mat-progress-bar style="height: 8px; padding-right: 30px" ` + 
            `role="progressbar" aria-valuemin="0" aria-valuemax="100" ` + 
            `tabindex="-1" mode="determinate" value="${progress}" ` + 
            `class="mat-progress-bar mat-primary" aria-valuenow="${progress}">` + 
        `<div aria-hidden="true">` + 
            `<svg width="100%" height="8" focusable="false" ` + 
               `class="mat-progress-bar-background mat-progress-bar-element">` +
            `<defs>` + 
            `<pattern x="4" y="0" width="8" height="4" patternUnits="userSpaceOnUse" id="mat-progress-bar-0">` + 
            `<circle cx="2" cy="2" r="2"></circle>` + 
            `</pattern>` + 
            `</defs>` + 
            `<rect width="100%" height="100%" fill="url("/components/progress-bar/overview#mat-progress-bar-0")"></rect>` + 
            `</svg>` + 
            `<div class="mat-progress-bar-buffer mat-progress-bar-element"></div>` + 
            `<div class="mat-progress-bar-primary mat-progress-bar-fill mat-progress-bar-element" style="transform: scale3d(${progress / 100}, 1, 1);"></div>` + 
            `<div class="mat-progress-bar-secondary mat-progress-bar-fill mat-progress-bar-element"></div>` + 
        `</div>` + 
    `</mat-progress-bar>`;
}
{:copy-code}
```

<br>
<br>

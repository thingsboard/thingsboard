### JavaScript Resource Module

<div class="divider"></div>
<br/>

A JavaScript module is a self-contained piece of code that encapsulates a specific functionality or set of related functionalities. Typically used in ThingsBoard for wrapping <a href="https://thingsboard.io/docs/user-guide/contribution/widgets-development/#thingsboard-extensions" target="_blank">custom extensions</a>.

#### Encapsulated JavaScript Functionality

A resource module can include any JavaScript code, making it easier to reuse specific logic. This can include variables or functions that are exported for use in other parts of the application.
##### Examples

You can declare variables:
```javascript
export const circle = '&#11044';
{:copy-code}
```
```javascript
export const integerRegex = /^[-+]?\d+$/;
{:copy-code}
```
Or functions:
```javascript
export const getStatusStyles = (value) => {
  let color;
  if (value) {
    color = 'rgb(39, 134, 34)';
  } else {
    color = 'rgb(255, 0, 0)';
  }
  return {
    color: color,
    fontSize: '18px'
  };
};
{:copy-code}
```
```javascript
export const formatDateToString = (date) => {
  const options = { hour: '2-digit', minute: '2-digit', second: '2-digit' };
  return date.toLocaleTimeString('en-US', options);
};
{:copy-code}
```
After import, these are reusable components that can be utilized in different parts of your application.
#### JavaScript Custom Extension Module
Alternatively, a JavaScript module  can be a <a href="https://github.com/thingsboard/thingsboard-extensions" target="_blank">ThingsBoard extension</a> or any other pre-built JavaScript module.
##### Where to start?
To create a custom extension module, you can start by defining the specific functionality you want to encapsulate. This could involve:
- Creating new widgets or dashboards with custom components.
- Implementing custom logic for data processing.
- Implementing custom logic for cell or markdown visualization.
- Integrating third-party libraries.
- Extending existing ThingsBoard features.

Once your module is developed, you can package it as a ThingsBoard extension, allowing it to be easily imported and used in your widgets and dashboards.
<br>
*If you plan to reuse existing ThingsBoard logic, ensure you follow the <a href="https://github.com/thingsboard/thingsboard-extensions/blob/master/README.md" target="_blank">guidelines</a>  provided.*
<br>

### JavaScript Resource Module

<div class="divider"></div>
<br/>

A JavaScript module is a self-contained piece of code that encapsulates a specific functionality or set of related functionalities.

#### Use Cases
JavaScript resource modules are advantageous for reusing custom logic. They can be utilized in:
- Widget controller script.
- Data post-processing functions.
- Markdown/HTML value functions.
- Cell style functions.
- Cell content functions.
- Custom actions.

These modules can contain any JavaScript code, facilitating the reuse of specific logic. This includes variables or functions that are exported for use in other parts of the application.
##### Examples

You can declare variables:
```javascript
export const circle = '&#11044;';
{:copy-code}
```
```javascript
export const integerRegex = /^[-+]?\d+$/;
{:copy-code}
```
Or define functions such as:
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
**Hint**: *Remember to use the `export` keyword to make variables and functions accessible for use outside the module.*

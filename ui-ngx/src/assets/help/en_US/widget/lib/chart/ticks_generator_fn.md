#### Ticks generator function

<div class="divider"></div>
<br/>

*function (extent): {value: number}[]*

A JavaScript function used to generate Y axis ticks.

**Parameters:**

<ul>
  <li><b>extent:</b> <code>number[]</code> - An array of two numbers holding axis min and max values <b>[axisMin, axisMax]</b>.
  </li>
</ul>

**Returns:**

An array of tick values with the following structure:

```typescript
{
    value: number
}
```

<div class="divider"></div>

##### Examples

* Always display only one tick in the middle:

```javascript
return extent ? [{ value: (extent[0] + extent[1]) / 2}] : [];
{:copy-code}
```

* Display only min and max ticks:

```javascript
if (extent) {
  return [ {value: extent[0]}, {value: extent[1]} ];
} else {
  return [];
}
{:copy-code}
```

* Disable ticks:

```javascript
return [];
{:copy-code}
```

* Constant ticks (1,2,3):

```javascript
return [ {value: 1}, {value: 2}, {value: 3} ];
{:copy-code}
```

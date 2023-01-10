#### Position conversion function

<div class="divider"></div>
<br/>

*function (origXPos, origYPos): {x: number, y: number}*

A JavaScript function used to convert original relative x, y coordinates of the marker.

**Parameters:**

- **origXPos:** <code>number</code> - original relative x coordinate as double from 0 to 1;
- **origYPos:** <code>number</code> - original relative y coordinate as double from 0 to 1;

**Returns:**

Should return position data having the following structure:

```typescript
{ 
   x: number,
   y: number
}
```

- *x* - new relative x coordinate as double from 0 to 1;
- *y* - new relative y coordinate as double from 0 to 1;

<div class="divider"></div>

##### Examples

* Scale the coordinates to half the original:

```javascript
return {x: origXPos / 2, y: origYPos / 2};
{:copy-code}
```

<br>
<br>

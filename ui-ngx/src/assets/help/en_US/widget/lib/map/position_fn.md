#### Position conversion function

<div class="divider"></div>
<br/>

*function (origXPos, origYPos, data, dsData, dsIndex, aspect): {x: number, y: number}*

A JavaScript function used to convert original relative x, y coordinates of the marker.

**Parameters:**

<ul>
  <li><b>origXPos:</b> <code>number</code> - original relative x coordinate as double from 0 to 1.</li>
  <li><b>origYPos:</b> <code>number</code> - original relative y coordinate as double from 0 to 1.</li>
  {% include widget/lib/map/map_fn_args %}
  <li><b>aspect:</b> <code>number</code> - image map aspect ratio.</li>
</ul>

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

* Detect markers with same positions and place them with minimum overlap:

```javascript
var xPos = data.xPos;
var yPos = data.yPos;
var locationGroup = dsData.filter((item) => item.xPos === xPos && item.yPos === yPos);
if (locationGroup.length > 1) {
  const count = locationGroup.length;
  const index = locationGroup.indexOf(data);
  const radius = 0.035;
  const angle = (360 / count) * index - 45;
  const x = xPos + radius * Math.sin(angle*Math.PI/180) / aspect;
  const y = yPos + radius * Math.cos(angle*Math.PI/180);
  return {x: x, y: y};
} else {
  return {x: xPos, y: yPos};
}
{:copy-code}
```

<br>
<br>

#### Point shape draw function

<div class="divider"></div>
<br/>

*function (ctx, x, y, radius, shadow): void*

A JavaScript function used to draw custom shapes for chart points when `Custom function` for point shape is selected.

**Parameters:**

<ul>
  <li>
    <b>ctx:</b> <code><a target="_blank" href="https://developer.mozilla.org/en-US/docs/Web/API/CanvasRenderingContext2D">CanvasRenderingContext2D</a></code> - A canvas drawing context.
  </li>
  <li>
    <b>x</b> <code>number</code> - point center X coordinate.
  </li>
  <li>
    <b>y</b> <code>number</code> - point center Y coordinate.
  </li>
  <li>
    <b>radius</b> <code>number</code> - point radius.
  </li>
  <li>
    <b>shadow</b> <code>boolean</code> - whether to draw shadow.
  </li>
</ul>

<div class="divider"></div>

##### Examples

* Draw square:

```javascript
var size = radius * Math.sqrt(Math.PI) / 2;
ctx.rect(x - size, y - size, size + size, size + size);
{:copy-code}
```

* Draw circle:

```javascript
ctx.moveTo(x + radius, y);
ctx.arc(x, y, radius, 0, shadow ? Math.PI : Math.PI * 2, false);
{:copy-code}
```

* Draw diamond:

```javascript
var size = radius * Math.sqrt(Math.PI / 2);
ctx.moveTo(x - size, y);
ctx.lineTo(x, y - size);
ctx.lineTo(x + size, y);
ctx.lineTo(x, y + size);
ctx.lineTo(x - size, y);
ctx.lineTo(x, y - size);
{:copy-code}
```

* Draw triangle:

```javascript
var size = radius * Math.sqrt(2 * Math.PI / Math.sin(Math.PI / 3));
var height = size * Math.sin(Math.PI / 3);
ctx.moveTo(x - size / 2, y + height / 2);
ctx.lineTo(x + size / 2, y + height / 2);
if (!shadow) {
    ctx.lineTo(x, y - height / 2);
    ctx.lineTo(x - size / 2, y + height / 2);
    ctx.lineTo(x + size / 2, y + height / 2);
}
{:copy-code}
```

* Draw cross:

```javascript
var size = radius * Math.sqrt(Math.PI) / 2;
ctx.moveTo(x - size, y - size);
ctx.lineTo(x + size, y + size);
ctx.moveTo(x - size, y + size);
ctx.lineTo(x + size, y - size);
{:copy-code}
```

* Draw ellipse:

```javascript
if (!shadow) {
    ctx.moveTo(x + radius, y);
    ctx.arc(x, y, radius, 0, Math.PI * 2, false);
}
{:copy-code}
```

* Draw plus:

```javascript
var size = radius * Math.sqrt(Math.PI / 2);
ctx.moveTo(x - size, y);
ctx.lineTo(x + size, y);
ctx.moveTo(x, y + size);
ctx.lineTo(x, y - size);
{:copy-code}
```

<br>
<br>

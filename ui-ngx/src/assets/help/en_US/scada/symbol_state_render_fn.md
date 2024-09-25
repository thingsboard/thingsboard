#### Symbol state render function

<div class="divider"></div>
<br/>

*function (ctx, svg): void*

A JavaScript function used to render SCADA symbol state.

**Parameters:**

<ul>
  <li><b>ctx:</b> <code>ScadaSymbolContext</code> - <a href="${siteBaseUrl}/docs${docPlatformPrefix}/user-guide/scada/scada-symbols-dev-guide/#scadasymbolcontext" target="_blank">Context</a> of the SCADA symbol.
  </li>
  <li><b>svg:</b> <code><a href="https://svgjs.dev/docs/3.2/container-elements/#svg-svg">SVG.Svg</a></code> - A root svg node. Instance of <a href="https://svgjs.dev/docs/3.2/container-elements/#svg-svg" target="_blank">SVG.Svg</a>.
  </li>
</ul>

<div class="divider"></div>

##### Examples

* Change colors for many tags on the value of the “active, value, minValue, maxValue”

```javascript
var levelUpButton = ctx.tags.levelUpButton;
var levelDownButton = ctx.tags.levelDownButton;
var levelArrowUp = ctx.tags.levelArrowUp;
var levelArrowDown = ctx.tags.levelArrowDown;

var active = ctx.values.active;
var value = ctx.values.value;
var minValue = ctx.properties.minValue;
var maxValue = ctx.properties.maxValue;

var levelUpEnabled = active && value < maxValue;
var levelDownEnabled = active && value > minValue;

if (levelUpEnabled) {
  ctx.api.enable(levelUpButton);
  levelArrowUp[0].attr({fill: '#647484'});
} else {
  ctx.api.disable(levelUpButton);
  levelArrowUp[0].attr({fill: '#777'});
}

if (levelDownEnabled) {
  ctx.api.enable(levelDownButton);
  levelArrowDown[0].attr({fill: '#647484'});
} else {
  ctx.api.disable(levelDownButton);
  levelArrowDown[0].attr({fill: '#777'});
}
{:copy-code}
```

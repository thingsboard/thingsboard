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

This JavaScript snippet manages the enablement and appearance of SVG buttons used to control volume settings based on device connectivity and value thresholds.
The 'Up' button is disabled when the volume reaches its maximum allowable value (`maxValue`), and the 'Down' button is disabled when the volume is at its minimum allowable value (`minValue`). 
Both buttons are disabled when the device is offline. The visual cues for enabled/disabled states are represented by color changes.

```javascript

var levelUpButton = ctx.tags.levelUpButton; // Button for increasing volume
var levelDownButton = ctx.tags.levelDownButton; // Button for decreasing volume
var levelArrowUp = ctx.tags.levelArrowUp; // Visual arrow for 'Up' button
var levelArrowDown = ctx.tags.levelArrowDown; // Visual arrow for 'Down' button

var active = ctx.values.active; // Device connectivity status
var value = ctx.values.value; // Current volume level
var minValue = ctx.properties.minValue; // Minimum volume level
var maxValue = ctx.properties.maxValue; // Maximum volume level

var enabledColor = '#4CAF50'; // Color for enabled state
var disabledColor = '#A1ADB1'; // Color for disabled state

// Determine if the 'Up' button should be enabled
var levelUpEnabled = active && value < maxValue;
if (levelUpEnabled) {
  ctx.api.enable(levelUpButton); // Enable 'Up' button
  levelArrowUp[0].attr({fill: enabledColor}); // Set arrow color to indicate enabled state
} else {
  ctx.api.disable(levelUpButton); // Disable 'Up' button
  levelArrowUp[0].attr({fill: disabledColor}); // Set arrow color to indicate disabled state
}

// Determine if the 'Down' button should be enabled
var levelDownEnabled = active && value > minValue;
if (levelDownEnabled) {
  ctx.api.enable(levelDownButton); // Enable 'Down' button
  levelArrowDown[0].attr({fill: enabledColor}); // Set arrow color to indicate enabled state
} else {
  ctx.api.disable(levelDownButton); // Disable 'Down' button
  levelArrowDown[0].attr({fill: disabledColor}); // Set arrow color to indicate disabled state
}

{:copy-code}
```

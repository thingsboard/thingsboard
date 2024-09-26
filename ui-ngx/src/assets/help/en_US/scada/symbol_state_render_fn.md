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

**Handle volume buttons state and appearance**

This JavaScript snippet manages the enablement and appearance of SVG buttons used to control volume settings based on device connectivity and value thresholds.
The 'Up' button is disabled when the volume reaches its maximum allowable value `maxValue`, and the 'Down' button is disabled when the volume is at its minimum allowable value `minValue`. 
Both buttons are disabled when the device is offline. The visual cues for enabled/disabled states are represented by color changes.

<br>

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

<br>

**Manage icon and label visibility and styling**

This JavaScript code snippet dynamically handles the visibility and styling of an icon and label within a SCADA symbol.
It checks the `showIcon` and `showLabel` properties from the context to determine whether to display the icon and/or label. 
If the icon or label is shown, the script sets their respective properties like size, color, and position. 
The script utilizes the <a href="${siteBaseUrl}/docs${docPlatformPrefix}/user-guide/scada/scada-symbols-dev-guide/#scadasymbolapi" target="_blank">ScadaSymbolApi</a> to update the visual appearance of these elements based on the context properties.
<br>
This approach ensures that both the icon and label elements are dynamically shown or hidden based on context properties, with appropriate styling applied.

<br>

```javascript
// Retrieve the first icon and label elements from the context tags
var iconTag = ctx.tags.icon[0];
var labelTag = ctx.tags.label[0];

// Check whether the icon should be displayed, based on the context property
var showIcon = ctx.properties.showIcon;
var showLabel = ctx.properties.label;

if (showIcon) {
  // Show the icon if the 'showIcon' property is true
  iconTag.show();
  // Set icon attributes such as icon type, size, and color
  var icon = ctx.properties.icon;
  var iconSize = ctx.properties.iconSize;
  var iconColor = ctx.properties.iconColor;

  // Use the ScadaSymbolApi to apply the icon, size, and color to the iconTag
  ctx.api.icon(iconTag, icon, iconSize, iconColor, true);
  
  // If the label is not shown, adjust the icon's position
  if (!showLabel) {
    iconTag.transform({translateX: 83, translateY: 137});
  }
} else {
  // Hide the icon if 'showIcon' is false
  iconTag.hide();
}

if (showLabel) {
  // Show the label if the 'showLabel' property is true
  var labelTextFont = ctx.properties.labelTextFont;
  var labelTextColor = ctx.properties.labelTextColor;

  // Apply font and color to the label using the ScadaSymbolApi
  ctx.api.font(labelTag, labelTextFont, labelTextColor);

  // Set the text content of the label
  ctx.api.text(labelTag, ctx.properties.labelText);

  // If the icon is not shown, adjust the label's position
  if (!showIcon) {
    labelTag.transform({translateX: 10});
  }

  // Show the label
  labelTag.show();
} else {
  // Hide the label if 'showLabel' is false
  labelTag.hide();
}
{:copy-code}
```

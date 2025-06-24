#### Symbol tag state render function

<div class="divider"></div>
<br/>

*function (ctx, element): void*

A JavaScript function used to render SCADA symbol element with specific tag.

**Parameters:**

<ul>
  <li><b>ctx:</b> <code>ScadaSymbolContext</code> - <a href="${siteBaseUrl}/docs${docPlatformPrefix}/user-guide/scada/scada-symbols-dev-guide/#scadasymbolcontext" target="_blank">Context</a> of the SCADA symbol.
  </li>
  <li><b>element:</b> <code>Element</code> - <a href="https://svgjs.dev/docs/3.2/getting-started/" target="_blank">SVG.js</a> element.<br>
        See the examples below to learn how to <a href="https://svgjs.dev/docs/3.2/manipulating/" target="_blank">manipulate</a> and <a href="${siteBaseUrl}/docs${docPlatformPrefix}/user-guide/scada/scada-symbols-dev-guide/#scadasymbolanimation" target="_blank">animate</a> elements.<br>
  </li>
</ul>

<div class="divider"></div>

##### Examples

**Update element state based on device activity**

This JavaScript snippet demonstrates how to dynamically change the background color of an SVG element based on the `active` status from the context. 
Additionally, it enables or disables the element’s click functionality depending on the `active` status. 
If the device is active, the element is given the activeColor and click actions are allowed. Otherwise, it is assigned the inactiveColor and the click action is disabled.

<br>

```javascript
// Context values
var active = ctx.values.active; // Device connectivity status
// Colors from context properties
var activeColor = ctx.properties.activeColor || '#4CAF50'; // Color for enabled state
var inactiveColor = ctx.properties.inactiveColor || '#A1ADB1'; // Color for disabled state
// Check if the device is active
if (active) {
  // Set the background color to activeColor if active
  element.attr({fill: activeColor});
  // Enable the element to allow click actions
  ctx.api.enable(element);
} else {
  // Set the background color to inactiveColor if not active
  element.attr({fill: inactiveColor});
  // Disable the element to forbid click actions
  ctx.api.disable(element);
}
{:copy-code}
```

<br>

**Smooth rotation based on activity and speed**

This JavaScript snippet creates a smooth, infinite rotation animation for an element based on the `active` status and adjusts the animation speed dynamically according to the `speed` value.
If the element is active, the animation starts or continues rotating with a speed proportional to the speed value. If inactive, the animation pauses.

<br>

```javascript
// Get the 'active' status and the current speed
var on = ctx.values.active;
var animation = ctx.api.cssAnimation(element); // Retrieve any existing animation on the element
var speed = ctx.values.speed ? ctx.values.speed / 60 : 1; // Calculate speed, default to 1 if not provided
var animationDuration = 2000; // Duration for one full rotation (optional, can be adjusted)
var rotationAngle = 360; // Full rotation in degrees

if (on) {
  // If active, either create a new rotation animation or adjust the existing one
  if (!animation) {
    animation = ctx.api.cssAnimate(element) // Create new animation if not already active
      .rotate(rotationAngle) // Set rotation angle to 360 degrees
      .loop() // Loop the animation infinitely
      .speed(speed); // Adjust speed based on the 'speed' value from the context
  } else {
    animation.speed(speed).play(); // If animation exists, adjust its speed and continue playing
  }
} else {
  // If inactive, pause the animation
  if (animation) {
    animation.pause();
  }
}
{:copy-code}
```

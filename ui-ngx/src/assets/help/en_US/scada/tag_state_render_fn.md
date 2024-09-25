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

*  Change the background of the element based on the value of the “active”

```javascript
if(ctx.values.active){
  element.attr({fill: ctx.properties.activeColor});
} else {
  element.attr({fill: ctx.properties.inactiveColor});
}
{:copy-code}
```

* Enable and disable the “On” button based on the state of the "active" (avoid or prevent click action)

```javascript
if (ctx.values.active) {
  ctx.api.disable(element);
} else {
  ctx.api.enable(element);
}
{:copy-code}
```

* Smooth infinite rotation animation based on the value of the “active” with speed based on the value of the “speed”

```javascript
var on = ctx.values.active;
var speed = ctx.values.speed ? ctx.values.speed / 60 : 1;
var animation = ctx.api.cssAnimation(element);

if (on) {
  if (!animation) {
    animation = ctx.api.cssAnimate(element, 2000)
      .rotate(360).loop().speed(speed);
  } else {
    animation.speed(speed).play();
  }
} else {
  if (animation) {
    animation.pause();
  }
}
{:copy-code}
```

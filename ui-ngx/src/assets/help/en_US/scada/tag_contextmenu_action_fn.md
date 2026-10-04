#### Symbol tag right click action function

<div class="divider"></div>
<br/>

*function (ctx, element, event): void*

A JavaScript function invoked when user right-clicks on SVG element with specific tag.

**Parameters:**

<ul>
  <li><b>ctx:</b> <code>ScadaSymbolContext</code> - <a href="${siteBaseUrl}/docs${docPlatformPrefix}/user-guide/scada-symbol-dev/#scadasymbolcontext" target="_blank">Context</a> of the SCADA symbol.
  </li>
  <li><b>element:</b> <code>Element</code> - <a href="https://svgjs.dev/docs/3.2/getting-started/" target="_blank">SVG.js</a> element.<br>
        See the examples below to learn how to <a href="https://svgjs.dev/docs/3.2/manipulating/" target="_blank">manipulate</a> and <a href="${siteBaseUrl}/docs${docPlatformPrefix}/user-guide/scada-symbol-dev/#scadasymbolanimation" target="_blank">animate</a> elements.<br>
  </li>
  <li><b>event:</b> <code>Event</code> - DOM event.
  </li>
</ul>

<div class="divider"></div>

The browser context menu is automatically suppressed on the element, so the configured action is executed instead.
Note that on touch devices the right click action is typically triggered by a long press.

##### Example

**Invoke widget right click action**

<br>

This JavaScript snippet demonstrates triggering a <a href="${siteBaseUrl}/docs${docPlatformPrefix}/user-guide/actions/#action-types" target="_blank">widget action</a> using the <a href="${siteBaseUrl}/docs${docPlatformPrefix}/user-guide/scada-symbol-dev/#scadasymbolcontext" target="_blank">ScadaSymbolContext API</a> when the right click event occurs. The widget action will be linked to the behaviorId 'rightClick', which defines the action that will be executed upon the event.
The behavior of this action depends on the type of widget action configured in the ThingsBoard platform (e.g., navigating to a dashboard state, updating the current state, opening a URL, etc.).

<br>

```javascript
ctx.api.callAction(event, 'rightClick');  // Trigger widget action 'rightClick' on event
{:copy-code}
```

<br>

*Example Use Case*

- **Open details popup:** If the 'rightClick' action is configured to open a popup with device details, right-clicking the element shows contextual information, while the left click keeps its primary action.
- **Custom context menu:** A custom-action widget action can render your own context menu for the element in place of the browser one.

<br>

You can also execute an action behavior directly and pass a value, for example:

```javascript
var running = ctx.values.running; // Current running status from context

// Request device shutdown with observer callbacks for error handling
ctx.api.callAction(event, 'shutdown', undefined, {
  error: () => {
    // Action failed; keep the current state
    ctx.api.setValue('running', running);
  }
});
{:copy-code}
```

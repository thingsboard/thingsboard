#### Symbol tag double click action function

<div class="divider"></div>
<br/>

*function (ctx, element, event): void*

A JavaScript function invoked when user double-clicks on SVG element with specific tag.

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

If the same tag also has an *on click* action configured, the click action is automatically deferred for a short interval
and cancelled when a double click is detected — only the double click action is executed.

##### Example

**Invoke widget double click action**

<br>

This JavaScript snippet demonstrates triggering a <a href="${siteBaseUrl}/docs${docPlatformPrefix}/user-guide/actions/#action-types" target="_blank">widget action</a> using the <a href="${siteBaseUrl}/docs${docPlatformPrefix}/user-guide/scada-symbol-dev/#scadasymbolcontext" target="_blank">ScadaSymbolContext API</a> when the double click event occurs. The widget action will be linked to the behaviorId 'doubleClick', which defines the action that will be executed upon the event.
The behavior of this action depends on the type of widget action configured in the ThingsBoard platform (e.g., navigating to a dashboard state, updating the current state, opening a URL, etc.).

<br>

```javascript
ctx.api.callAction(event, 'doubleClick');  // Trigger widget action 'doubleClick' on event
{:copy-code}
```

<br>

*Example Use Case*

- **Navigate to Dashboard State:** If the 'doubleClick' action is configured to navigate to a detailed dashboard state, the user will be redirected to that state when double-clicking on the element, while a single click may be reserved for a quick toggle action.
- **Open URL:** If configured to open a URL, double-clicking on the element will take the user to a specified web resource.

<br>

You can also execute an action behavior directly and pass a value, for example:

```javascript
var running = ctx.values.running; // Current running status from context

// Toggle the device state with observer callbacks for next and error handling
ctx.api.callAction(event, 'switchState', !running, {
  error: () => {
    // Action failed; keep the current state
    ctx.api.setValue('running', running);
  }
});
{:copy-code}
```

#### Symbol tag click action function

<div class="divider"></div>
<br/>

*function (ctx, element, event): void*

A JavaScript function invoked when user clicks on SVG element with specific tag.

**Parameters:**

<ul>
  <li><b>ctx:</b> <code>ScadaSymbolContext</code> - <a href="${siteBaseUrl}/docs${docPlatformPrefix}/user-guide/scada/scada-symbols-dev-guide/#scadasymbolcontext" target="_blank">Context</a> of the SCADA symbol.
  </li>
  <li><b>element:</b> <code>Element</code> - <a href="https://svgjs.dev/docs/3.2/getting-started/" target="_blank">SVG.js</a> element.<br>
        See the examples below to learn how to <a href="https://svgjs.dev/docs/3.2/manipulating/" target="_blank">manipulate</a> and <a href="${siteBaseUrl}/docs${docPlatformPrefix}/user-guide/scada/scada-symbols-dev-guide/#scadasymbolanimation" target="_blank">animate</a> elements.<br>
  </li>
  <li><b>event:</b> <code>Event</code> - DOM event.
  </li>
</ul>

<div class="divider"></div>

##### Examples

The example demonstrates how to dynamically call the 'turnOn' or 'turnOff' actions based on the 'active' status from the context. The actions are implemented using the following methods from the <a href="${siteBaseUrl}/docs${docPlatformPrefix}/user-guide/scada/scada-symbols-dev-guide/#scadasymbolapi" target="_blank">Scada Symbol API</a>:

- **callAction**: *(event: Event, behaviorId: string, value?: any, observer?: Partial\<Observer\<void\>\>): void* - Triggers a specific behavior action identified by its ID, allowing for the optional passing of values and observer callbacks.

- **setValue**: *(valueId: string, value: any): void* - Updates a specific value within the `ctx.values` object and initiates all related rendering functions.

For more detailed guidelines on device interaction, consider reviewing the <a href="${siteBaseUrl}/docs${docPlatformPrefix}/user-guide/scada/scada-symbols-dev-guide/#best-practices" target="_blank">best practices</a>.

```javascript
var active = ctx.values.active; // Current active status from context
var action = active ? 'turnOn' : 'turnOff'; // Determine action based on active status
var parameter = "any object or primitive"; // Parameter to pass with the action

// Call the action with observer callbacks for next and error handling
ctx.api.callAction(event, action, parameter, {
  next: () => {
    // Action succeeded; toggle the 'activate' status for debugging
    ctx.api.setValue('activate', !active);
  },
  error: () => {
    // Action failed; reset the 'activate' status for debugging
    ctx.api.setValue('activate', active);
  }
});
```

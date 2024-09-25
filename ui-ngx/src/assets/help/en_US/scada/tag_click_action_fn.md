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

* Set new value action

*callAction: (event: Event, behaviorId: string, value?: any, observer?: Partial\<Observer\<void\>\>): void*

*setValue: (valueId: string, value: any): void*

Avoid manually setting behavior values, as shown in the example, see <a href="${siteBaseUrl}/docs${docPlatformPrefix}/user-guide/scada/scada-symbols-dev-guide/#best-practices" target="_blank">best practice</a> for Device Interaction

```javascript
var active = ctx.values.active;
var action = active ? 'turnOn' : 'turnOff';

ctx.api.callAction(event, action, active, {
  next: () => {
    // To simplify debugging in preview mode
    ctx.api.setValue('activate', !active);
  },
  error: () => {
    // To simplify debugging in preview mode
    ctx.api.setValue('activate', active);
  }
});
{:copy-code}
```

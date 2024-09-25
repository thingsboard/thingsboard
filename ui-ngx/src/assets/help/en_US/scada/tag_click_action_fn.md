#### Symbol tag click action function

<div class="divider"></div>
<br/>

*function (ctx, element, event): void*

A JavaScript function invoked when user clicks on SVG element with specific tag.

**Parameters:**

<ul>
  <li><b>ctx:</b> <code>ScadaSymbolContext</code> - <a href="${siteBaseUrl}/docs${docPlatformPrefix}/user-guide/scada/scada-symbols-dev-guide/#scadasymbolcontext" target="_blank">Context</a> of the SCADA symbol.
  </li>
  <li><b>element:</b> <code>Element</code> - SVG element.<br>
        See <a href="https://svgjs.dev/docs/3.2/manipulating/" target="_blank">Manipulating</a> section to manipulate the element.<br>
        See <a href="https://svgjs.dev/docs/3.2/animating/" target="_blank">Animating</a> section to animate the element.
  </li>
  <li><b>event:</b> <code>Event</code> - DOM event.
  </li>
</ul>

<div class="divider"></div>

##### Examples

* Set new value action

```javascript
var active = ctx.values.active;
var action = active ? 'inactive' : 'active';

ctx.api.callAction(event, action, undefined, {
  next: () => {
    ctx.api.setValue('activate', !active);
  },
  error: () => {
    ctx.api.setValue('activate', active);
  }
});
{:copy-code}
```

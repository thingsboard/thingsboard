#### Using existing JavaScript/Typescript code

<div class="divider"></div>
<br/>

Another approach of creating widgets is to use existing bundled JavaScript/Typescript code.

In this case, you can create own TypeScript class or Angular component and bundle it into the ThingsBoard UI code.

In order to make this code accessible within the widget, you need to register corresponding Angular module or inject TypeScript class to a global variable (for ex. window object).

Some ThingsBoard widgets already use this approach. Take a look at the [widget-component-service.ts{:target="_blank"}](https://github.com/thingsboard/thingsboard/blob/2627fe51d491055d4140f16617ed543f7f5bd8f6/ui-ngx/src/app/modules/home/components/widget/widget-component.service.ts#L140)
or [widget-components.module.ts{:target="_blank"}](https://github.com/thingsboard/thingsboard/blob/2627fe51d491055d4140f16617ed543f7f5bd8f6/ui-ngx/src/app/modules/home/components/widget/widget-components.module.ts#L50). <br>
Here you can find how some bundled classes or components are registered for later use in ThingsBoard widgets.

For example "Timeseries - Flot" widget (from "Charts" Widgets Bundle) uses [**TbFlot**{:target="_blank"}](https://github.com/thingsboard/thingsboard/blob/2627fe51d491055d4140f16617ed543f7f5bd8f6/ui-ngx/src/app/modules/home/components/widget/lib/flot-widget.ts#L73) TypeScript class which is injected as window property inside **widget-component-service.ts**:

```typescript
...

const widgetModulesTasks: Observable<any>[] = [];
...

widgetModulesTasks.push(from(import('@home/components/widget/lib/flot-widget')).pipe(
  tap((mod) => {
    (window as any).TbFlot = mod.TbFlot;
  }))
);
...

```

Another example is "Timeseries table" widget (from "Cards" Widgets Bundle) that uses Angular component [**tb-timeseries-table-widget**{:target="_blank"}](https://github.com/thingsboard/thingsboard/blob/2627fe51d491055d4140f16617ed543f7f5bd8f6/ui-ngx/src/app/modules/home/components/widget/lib/timeseries-table-widget.component.ts#L107)<br>which is registered as dependency of **WidgetComponentsModule** Angular module inside **widget-components.module.ts**.
Thereby this component becomes available for use inside the widget template HTML.

```typescript
...

import { TimeseriesTableWidgetComponent } from '@home/components/widget/lib/timeseries-table-widget.component';

...

@NgModule({
  declarations:
    [
...
      TimeseriesTableWidgetComponent,
...
    ],
...
  exports: [
...
      TimeseriesTableWidgetComponent,
...
  ],
...    
})
export class WidgetComponentsModule { }
```

<br>
<br>

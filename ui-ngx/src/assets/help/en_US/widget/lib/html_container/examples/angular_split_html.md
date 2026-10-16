#### Angular template of resizable split master–detail

```html
{:code-style="max-height: 400px;"}
<div class="split">
  <aside class="split__list" [style.width.px]="listWidth">
    <h3 class="split__title">Devices</h3>
    <ul class="split__items">
      <li *ngFor="let d of devices"
          class="split__item"
          [class.is-active]="d.entityId === selectedId"
          (click)="select(d)">
        <span class="split__name">{{ d.name }}</span>
        <span class="split__value">{{ d.lastValue }}</span>
      </li>
    </ul>
  </aside>

  <div class="split__divider" (mousedown)="startDrag($event)"></div>

  <section class="split__detail">
    <ng-container *ngIf="selected; else empty">
      <header>
        <div class="split__eyebrow">Device</div>
        <h3 class="split__detail-title">{{ selected.name }}</h3>
      </header>
      <div class="split__state">
        <tb-dashboard-state [ctx]="ctx" stateId="deviceCharts" [syncParentStateParams]="true"></tb-dashboard-state>
      </div>
    </ng-container>
    <ng-template #empty><div class="split__empty">Select a device</div></ng-template>
  </section>
</div>
{:copy-code}
```

<br>
<br>

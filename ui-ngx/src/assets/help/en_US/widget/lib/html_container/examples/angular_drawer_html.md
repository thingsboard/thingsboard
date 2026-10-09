#### Angular template of slide-over device detail

```html
{:code-style="max-height: 400px;"}
<mat-drawer-container class="dd">
  <mat-drawer-content class="dd__main">
    <h3 class="dd__title">Devices</h3>
    <div class="dd__card">
      <mat-table [dataSource]="devices" class="dd__table">
        <ng-container matColumnDef="name">
          <mat-header-cell *matHeaderCellDef>Device</mat-header-cell>
          <mat-cell *matCellDef="let d">{{ d.name }}</mat-cell>
        </ng-container>
        <ng-container matColumnDef="people">
          <mat-header-cell *matHeaderCellDef>People</mat-header-cell>
          <mat-cell *matCellDef="let d">{{ d.lastValue }}</mat-cell>
        </ng-container>
        <mat-header-row *matHeaderRowDef="columns"></mat-header-row>
        <mat-row *matRowDef="let d; columns: columns" [class.is-active]="d.entityId === selectedId" (click)="openDevice(d)"></mat-row>
      </mat-table>
    </div>
  </mat-drawer-content>

  <mat-drawer class="dd__drawer" mode="over" position="end" [opened]="drawerOpen" (openedChange)="drawerOpen = $event">
    <div class="dd__panel" *ngIf="selected">
      <header class="dd__panel-bar">
        <div>
          <div class="dd__panel-eyebrow">Device</div>
          <h3 class="dd__panel-title">{{ selected.name }}</h3>
        </div>
        <button class="dd__close" (click)="drawerOpen = false">✕</button>
      </header>

      <div class="dd__cards">
        <div class="card"><span class="card__label">People</span><span class="card__value">{{ selected.lastValue }}</span></div>
        <div class="card"><span class="card__label">Entity type</span><span class="card__value">{{ selected.entityType }}</span></div>
        <div class="card"><span class="card__label">Last update</span><span class="card__value">{{ selected.lastSeen }}</span></div>
      </div>

      <div class="dd__actions">
        <div class="dd__action">
          <button class="dd__btn" [disabled]="rebooting" (click)="reboot()">Reboot</button>
          <mat-progress-bar *ngIf="rebooting" mode="indeterminate"></mat-progress-bar>
        </div>
        <div class="dd__action">
          <button class="dd__btn dd__btn--warn" [disabled]="resetting" (click)="resetCounter()">Reset counter</button>
          <mat-progress-bar *ngIf="resetting" mode="indeterminate"></mat-progress-bar>
        </div>
      </div>

      <section class="dd__section">
        <h4 class="dd__section-title">Alarms</h4>
        <div class="dd__state"><tb-dashboard-state [ctx]="ctx" stateId="deviceAlarms" [syncParentStateParams]="true"></tb-dashboard-state></div>
      </section>
      <section class="dd__section">
        <h4 class="dd__section-title">Charts</h4>
        <div class="dd__state"><tb-dashboard-state [ctx]="ctx" stateId="deviceCharts" [syncParentStateParams]="true"></tb-dashboard-state></div>
      </section>
    </div>
  </mat-drawer>
</mat-drawer-container>
{:copy-code}
```

<br>
<br>

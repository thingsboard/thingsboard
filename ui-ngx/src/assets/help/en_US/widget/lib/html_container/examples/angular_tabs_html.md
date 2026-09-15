#### Angular template of composite dashboard with tab navigation

```html
{:code-style="max-height: 400px;"}
<div class="composite">
  <nav class="tabs">
    <button *ngFor="let tab of tabs"
            class="tab"
            [class.is-active]="tab.id === activeTab"
            (click)="selectTab(tab.id)">
      {{ tab.label }}
    </button>
  </nav>
  <div tb-toast toastTarget="layout" class="container">
    <tb-dashboard-state class="state" [ctx]="ctx" [stateId]="stateId" [syncParentStateParams]="true"></tb-dashboard-state>
  </div>
</div>
{:copy-code}
```

<br>
<br>

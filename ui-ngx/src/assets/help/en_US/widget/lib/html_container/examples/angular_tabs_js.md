#### JavaScript function of composite dashboard with tab navigation

```javascript
{:code-style="max-height: 400px;"}
this.ctx = ctx;

this.tabs = [
  { id: 'assets', label: 'Assets', stateId: 'assetsState' },
  { id: 'devices', label: 'Devices', stateId: 'devicesState' },
  { id: 'customers', label: 'Customers', stateId: 'customersState' },
];

const apply = (subState) => {
  const tab = this.tabs.find((t) => t.id === subState) || this.tabs[0];
  this.activeTab = tab.id;
  this.stateId = tab.stateId;
};

apply(ctx.stateController.getStateParams().subState);

this.selectTab = (subState) => ctx.stateController.updateState(null, { subState });

const stateSub = ctx.stateController.dashboardCtrl.dashboardCtx.stateChanged.subscribe(() => {
  apply(ctx.stateController.getStateParams().subState);
  ctx.detectChanges();
});

ctx.registerDestroyCallback(() => {
  stateSub.unsubscribe();
});
{:copy-code}
```

<br>
<br>

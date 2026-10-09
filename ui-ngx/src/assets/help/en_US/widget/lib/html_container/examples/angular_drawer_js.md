#### JavaScript function of slide-over device detail

```javascript
{:code-style="max-height: 400px;"}
this.ctx = ctx;
this.devices = [];
this.columns = ['name', 'people'];
this.selected = null;
this.selectedId = null;
this.drawerOpen = false;

this.openDevice = (d) => {
  this.selected = d;
  this.selectedId = d.entityId;
  this.drawerOpen = true;
  ctx.stateController.updateState(null, {
    entityId: { entityType: d.entityType, id: d.entityId },
    entityName: d.name,
  });
  ctx.detectChanges();
};

this.rebooting = false;
this.resetting = false;

this.reboot = () => {
  if (!this.selected || this.rebooting) return;
  this.rebooting = true;
  ctx.detectChanges();
  ctx.http.post('/api/rpc/twoway/' + this.selected.entityId, { method: 'reboot', params: {} }).subscribe({
    error: (e) => {
      console.error(e);
      this.rebooting = false;
      ctx.detectChanges();
    },
    complete: () => {
      this.rebooting = false;
      ctx.detectChanges();
    },
  });
};

this.resetCounter = () => {
  if (!this.selected || this.resetting) return;
  this.resetting = true;
  ctx.detectChanges();
  ctx.http.post('/api/rpc/twoway/' + this.selected.entityId, { method: 'resetCounter', params: {} }).subscribe({
    error: (e) => {
      console.error(e);
      this.resetting = false;
      ctx.detectChanges();
    },
    complete: () => {
      this.resetting = false;
      ctx.detectChanges();
    },
  });
};

const subOpts = {
  type: 'latest',
  datasources: [
    {
      type: 'entity',
      entityFilter: { type: 'deviceType', deviceTypes: ['peopleCount'], deviceNameFilter: '', resolveMultiple: true },
      dataKeys: [{ type: 'timeseries', name: 'peopleCount', settings: {} }],
    },
  ],
  callbacks: {
    onDataUpdated: (subscription) => {
      this.devices = (subscription.data || []).map((entry) => {
        const ds = entry.datasource;
        const last = entry.data && entry.data.length ? entry.data[entry.data.length - 1] : null;
        return {
          entityId: ds.entityId,
          entityType: ds.entityType,
          name: ds.entityName,
          lastValue: last ? last[1] : '—',
          lastSeen: last ? new Date(last[0]).toLocaleString() : '—',
        };
      });
      ctx.detectChanges();
    },
    onDataUpdateError: (subscription, e) => console.error(e),
  },
};

let subscriptionId = null;
ctx.subscriptionApi.createSubscription(subOpts, true).subscribe((subscription) => {
  subscriptionId = subscription.id;
});

ctx.registerDestroyCallback(() => {
  if (subscriptionId != null) ctx.subscriptionApi.removeSubscription(subscriptionId);
});
{:copy-code}
```

<br>
<br>

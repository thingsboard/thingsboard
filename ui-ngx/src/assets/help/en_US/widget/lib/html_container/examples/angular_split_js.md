#### JavaScript function of resizable split master–detail

```javascript
{:code-style="max-height: 400px;"}
this.ctx = ctx;
this.devices = [];
this.selected = null;
this.selectedId = null;
this.listWidth = 280;

this.select = (d) => {
  this.selected = d;
  this.selectedId = d.entityId;
  ctx.stateController.updateState(null, {
    entityId: { entityType: d.entityType, id: d.entityId },
    entityName: d.name,
  });
  ctx.detectChanges();
};

this.startDrag = (event) => {
  event.preventDefault();
  const root = event.currentTarget.closest('.split');
  const left = root.getBoundingClientRect().left;
  root.classList.add('split--dragging');
  const onMove = (e) => {
    this.listWidth = Math.max(160, Math.min(e.clientX - left, root.clientWidth - 220));
    ctx.detectChanges();
  };
  const onUp = () => {
    root.classList.remove('split--dragging');
    window.removeEventListener('mousemove', onMove);
    window.removeEventListener('mouseup', onUp);
  };
  window.addEventListener('mousemove', onMove);
  window.addEventListener('mouseup', onUp);
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
        return { entityId: ds.entityId, entityType: ds.entityType, name: ds.entityName, lastValue: last ? last[1] : '—' };
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

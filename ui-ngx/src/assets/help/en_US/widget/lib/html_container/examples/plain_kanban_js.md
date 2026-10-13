#### JavaScript function of kanban board

```javascript
{:code-style="max-height: 400px;"}
// Plain HTML mode: `container` is the widget's DOM. Build a status board and,
// on drop, persist the card's new column (status) back to the device.
const Sortable = window.Sortable; // loaded from the CDN added under Resources
const statuses = ['idle', 'active', 'maintenance'];
const labels = { idle: 'Idle', active: 'Active', maintenance: 'Maintenance' };

const board = container.querySelector('#board');
let devices = []; // { id, entityType, name, status }

// Escape untrusted strings (e.g. device names) before putting them in innerHTML.
const esc = (s) =>
  String(s).replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[c]);

function render() {
  board.innerHTML = statuses
    .map((s) => {
      const items = devices.filter((d) => d.status === s);
      const cards = items
        .map(
          (d) =>
            `<div class="kanban__card" data-id="${esc(d.id)}" data-type="${esc(d.entityType)}"><span class="kanban__dot kanban__dot--${s}"></span><span class="kanban__name">${esc(d.name)}</span></div>`,
        )
        .join('');
      return `<section class="kanban__col">
        <header class="kanban__head"><span class="kanban__dot kanban__dot--${s}"></span><span class="kanban__title">${labels[s]}</span><span class="kanban__count">${items.length}</span></header>
        <div class="kanban__cards" data-status="${s}">${cards}</div>
      </section>`;
    })
    .join('');

  // Make each column a Sortable list; cards share one group so they move between columns.
  board.querySelectorAll('.kanban__cards').forEach((listEl) => {
    new Sortable(listEl, {
      group: 'kanban',
      animation: 150,
      onAdd: (evt) => {
        const id = evt.item.dataset.id;
        const entityType = evt.item.dataset.type;
        const status = evt.to.dataset.status; // destination column
        const dev = devices.find((d) => d.id === id);
        if (dev) dev.status = status;
        ctx.attributeService
          .saveEntityAttributes({ id, entityType }, 'SERVER_SCOPE', [{ key: 'status', value: status }])
          .subscribe();
      },
    });
  });
}

// Live data: devices of type "machine" with their "status" attribute.
const subOpts = {
  type: 'latest',
  datasources: [
    {
      type: 'entity',
      entityFilter: { type: 'deviceType', deviceTypes: ['machine'], deviceNameFilter: '', resolveMultiple: true },
      dataKeys: [{ type: 'attribute', name: 'status', settings: {} }],
    },
  ],
  callbacks: {
    onDataUpdated: (subscription) => {
      devices = (subscription.data || []).map((entry) => {
        const ds = entry.datasource;
        const last = entry.data && entry.data.length ? entry.data[entry.data.length - 1] : null;
        const status = last ? String(last[1]) : 'idle';
        return { id: ds.entityId, entityType: ds.entityType, name: ds.entityName, status: statuses.includes(status) ? status : 'idle' };
      });
      render();
    },
    onDataUpdateError: (subscription, e) => console.error(e),
  },
};
// The JS re-runs on every reload — capture the id and remove the subscription on destroy.
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

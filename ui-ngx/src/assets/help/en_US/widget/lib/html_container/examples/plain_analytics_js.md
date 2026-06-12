#### JavaScript function of visitor analytics

```javascript
{:code-style="max-height: 400px;"}
// Plain HTML mode: `container` is the widget's DOM; ECharts comes from the CDN.
const echarts = window.echarts;
const el = (sel) => container.querySelector(sel);
const pad = (n) => String(n).padStart(2, '0');
const isoDate = (d) => d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate());
const dayLabel = (d) => d.toLocaleDateString(undefined, { month: 'short', day: 'numeric' });
const minutes = (v) => {
  const [h, m] = (v || '0:0').split(':').map(Number);
  return h * 60 + m;
};
const palette = ['#2563eb', '#10b981', '#f59e0b', '#8b5cf6', '#ef4444', '#0ea5e9'];

const chart = echarts.init(el('#vcChart'));
chart.setOption({
  tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
  legend: { top: 0 },
  grid: { left: 8, right: 16, top: 40, bottom: 8, containLabel: true },
  xAxis: { type: 'category', data: [], axisLabel: { hideOverlap: true } },
  yAxis: { type: 'value' },
  series: [],
});
const resizeObserver = new window.ResizeObserver(() => chart.resize());
resizeObserver.observe(el('#vcChart'));

let lastEntries = [];
let mode = 'daily';
let selectedDate = null;
let dailyDates = []; // iso date per daily x-index, for drill-down

// Click a day → drill into its hourly breakdown.
chart.on('click', (p) => {
  if (mode !== 'daily' || p.dataIndex == null || dailyDates[p.dataIndex] == null) return;
  selectedDate = dailyDates[p.dataIndex];
  mode = 'hourly';
  el('#vcBack').hidden = false;
  recompute();
});
el('#vcBack').addEventListener('click', () => {
  mode = 'daily';
  selectedDate = null;
  el('#vcBack').hidden = true;
  recompute();
});

// One stacked series per device, summing values into the given bucket index.
function buildSeries(labelCount, bucketOf) {
  return lastEntries.map((entry, k) => {
    const arr = new Array(labelCount).fill(0);
    (entry.data || []).forEach(([ts, value]) => {
      const i = bucketOf(new Date(Number(ts)));
      if (i != null && i >= 0) arr[i] += Number(value);
    });
    return { name: entry.datasource.entityName, type: 'bar', stack: 'v', data: arr, itemStyle: { color: palette[k % palette.length] } };
  });
}

function recompute() {
  const open = minutes(el('#vcOpen').value);
  const close = minutes(el('#vcClose').value);
  const inHours = (d) => {
    const m = d.getHours() * 60 + d.getMinutes();
    return m >= open && m < close;
  };

  // KPIs over the whole range (per day, summed across devices).
  const start = new Date(el('#vcStart').value + 'T00:00:00');
  const end = new Date(el('#vcEnd').value + 'T00:00:00');
  const dayIndex = {};
  const dayLabels = [];
  dailyDates = [];
  for (let t = new Date(start); t <= end; t.setDate(t.getDate() + 1)) {
    dayIndex[isoDate(t)] = dayLabels.length;
    dailyDates.push(isoDate(t));
    dayLabels.push(dayLabel(t));
  }
  const dayTotals = dayLabels.map(() => 0);
  const perHour = {};
  let total = 0;
  lastEntries.forEach((entry) =>
    (entry.data || []).forEach(([ts, value]) => {
      const d = new Date(Number(ts));
      if (!inHours(d)) return;
      const i = dayIndex[isoDate(d)];
      if (i === undefined) return;
      const v = Number(value);
      dayTotals[i] += v;
      perHour[d.getHours()] = (perHour[d.getHours()] || 0) + v;
      total += v;
    }),
  );
  el('#vcTotal').textContent = total;
  el('#vcPeakDay').textContent = total ? dayLabels[dayTotals.indexOf(Math.max(...dayTotals))] : '—';
  const ph = Object.keys(perHour).sort((a, b) => perHour[b] - perHour[a])[0];
  el('#vcPeakHour').textContent = ph != null ? ph + ':00' : '—';
  el('#vcAvg').textContent = Math.round(total / (dayTotals.filter((x) => x > 0).length || 1));

  // Chart: daily (stacked by device) or the hourly breakdown of the selected day.
  if (mode === 'daily') {
    el('#vcChartTitle').textContent = 'Visitors by day';
    const series = buildSeries(dayLabels.length, (d) => (inHours(d) ? dayIndex[isoDate(d)] : null));
    chart.setOption({ xAxis: { data: dayLabels }, series }, { replaceMerge: ['series'] });
  } else {
    const h0 = Math.floor(open / 60);
    const h1 = Math.ceil(close / 60);
    const labels = [];
    for (let h = h0; h < h1; h++) labels.push(pad(h) + ':00');
    el('#vcChartTitle').textContent = 'Visitors by hour — ' + dayLabel(new Date(selectedDate + 'T00:00:00'));
    const series = buildSeries(labels.length, (d) =>
      isoDate(d) === selectedDate && inHours(d) ? d.getHours() - h0 : null,
    );
    chart.setOption({ xAxis: { data: labels }, series }, { replaceMerge: ['series'] });
  }
}

// (Re)subscribe for the chosen date range (a fixed history window).
let subscriptionId = null;
function subscribe() {
  if (subscriptionId != null) {
    ctx.subscriptionApi.removeSubscription(subscriptionId);
    subscriptionId = null;
  }
  const startTimeMs = new Date(el('#vcStart').value + 'T00:00:00').getTime();
  const endTimeMs = new Date(el('#vcEnd').value + 'T23:59:59').getTime();
  const subOpts = {
    type: 'timeseries',
    useDashboardTimewindow: false,
    timeWindowConfig: {
      selectedTab: 1,
      history: { historyType: 1, fixedTimewindow: { startTimeMs, endTimeMs } },
      aggregation: { type: 'NONE', limit: 50000 },
    },
    datasources: [
      {
        type: 'entity',
        entityFilter: { type: 'deviceType', deviceTypes: ['peopleCount'], deviceNameFilter: '', resolveMultiple: true },
        dataKeys: [{ type: 'timeseries', name: 'peopleCount', settings: {} }],
      },
    ],
    callbacks: {
      onDataUpdated: (subscription) => {
        lastEntries = subscription.data || [];
        recompute();
      },
      onDataUpdateError: (subscription, e) => console.error(e),
    },
  };
  ctx.subscriptionApi.createSubscription(subOpts, true).subscribe((subscription) => {
    subscriptionId = subscription.id;
  });
}

// Changing the range resets the drill-down and re-subscribes; hours just re-filter.
function onRangeChange() {
  mode = 'daily';
  selectedDate = null;
  el('#vcBack').hidden = true;
  subscribe();
}

// Defaults: the current month (1st → today).
const now = new Date();
el('#vcEnd').value = isoDate(now);
el('#vcStart').value = isoDate(new Date(now.getFullYear(), now.getMonth(), 1));

el('#vcStart').addEventListener('change', onRangeChange);
el('#vcEnd').addEventListener('change', onRangeChange);
el('#vcOpen').addEventListener('change', recompute);
el('#vcClose').addEventListener('change', recompute);
subscribe();

// The JS re-runs on every reload — release the subscription, observer and chart.
ctx.registerDestroyCallback(() => {
  if (subscriptionId != null) ctx.subscriptionApi.removeSubscription(subscriptionId);
  resizeObserver.disconnect();
  chart.dispose();
});
{:copy-code}
```

<br>
<br>

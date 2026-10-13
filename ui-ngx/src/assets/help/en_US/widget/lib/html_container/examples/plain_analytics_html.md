#### HTML code of visitor analytics

```html
{:code-style="max-height: 400px;"}
<div class="vc">
  <header class="vc__head">
    <div>
      <h3 class="vc__title">Visitor analytics</h3>
      <span class="vc__sub">all peopleCount devices</span>
    </div>
    <div class="vc__controls">
      <label class="vc__field"><span>From</span><input type="date" id="vcStart" /></label>
      <label class="vc__field"><span>To</span><input type="date" id="vcEnd" /></label>
      <label class="vc__field"><span>Open</span><input type="time" id="vcOpen" value="09:00" /></label>
      <label class="vc__field"><span>Close</span><input type="time" id="vcClose" value="18:00" /></label>
    </div>
  </header>
  <div class="vc__kpis">
    <div class="vc-kpi"><span class="vc-kpi__label">Total visitors</span><span class="vc-kpi__value" id="vcTotal">—</span></div>
    <div class="vc-kpi"><span class="vc-kpi__label">Busiest day</span><span class="vc-kpi__value" id="vcPeakDay">—</span></div>
    <div class="vc-kpi"><span class="vc-kpi__label">Peak hour</span><span class="vc-kpi__value" id="vcPeakHour">—</span></div>
    <div class="vc-kpi"><span class="vc-kpi__label">Daily average</span><span class="vc-kpi__value" id="vcAvg">—</span></div>
  </div>
  <div class="vc__card">
    <div class="vc__card-head">
      <span class="vc__card-title" id="vcChartTitle">Visitors by day</span>
      <button class="vc__back" id="vcBack" hidden>← Back</button>
    </div>
    <div class="vc__chart" id="vcChart"></div>
  </div>
</div>
{:copy-code}
```

<br>
<br>

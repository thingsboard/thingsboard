#### CSS of visitor analytics

```css
{:code-style="max-height: 400px;"}
.vc {
  display: flex;
  flex-direction: column;
  gap: 16px;
  height: 100%;
  box-sizing: border-box;
  padding: 20px;
  background: #fff;
  font-family: 'Inter', 'Roboto', system-ui, sans-serif;
  color: #0f172a;
}
.vc__head { display: flex; align-items: flex-end; justify-content: space-between; gap: 16px; flex-wrap: wrap; }
.vc__title { margin: 0; font-family: 'Roboto', sans-serif; font-size: 20px; font-weight: 500; }
.vc__sub { font-size: 12px; font-weight: 600; color: #94a3b8; }
.vc__controls { display: flex; gap: 10px; flex-wrap: wrap; }
.vc__field { display: flex; flex-direction: column; gap: 4px; font-size: 11px; font-weight: 600; color: #64748b; }
.vc__field input { padding: 7px 10px; border: 1px solid #d8dee9; border-radius: 10px; font-size: 13px; color: #0f172a; }
.vc__field input:focus { outline: none; border-color: #2f6bff; box-shadow: 0 0 0 3px rgba(47, 107, 255, 0.15); }
.vc__kpis { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; }
.vc-kpi { display: flex; flex-direction: column; gap: 6px; padding: 14px 16px; border-radius: 14px; background: #f8fafc; border: 1px solid #e6ebf2; }
.vc-kpi__label { font-size: 10px; font-weight: 700; text-transform: uppercase; letter-spacing: 0.08em; color: #94a3b8; }
.vc-kpi__value { font-size: 26px; font-weight: 800; line-height: 1; color: #1e293b; }
.vc__card { flex: 1; min-height: 240px; display: flex; flex-direction: column; padding: 16px; border: 1px solid #e6ebf2; border-radius: 14px; }
.vc__card-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 8px; }
.vc__card-title { font-size: 13px; font-weight: 700; color: #334155; }
.vc__back { padding: 6px 12px; border: 1px solid #d8dee9; border-radius: 8px; background: #fff; color: #2f6bff; font-size: 12px; font-weight: 600; cursor: pointer; }
.vc__back[hidden] { display: none; }
.vc__back:hover { background: #f0f5ff; border-color: #2f6bff; }
.vc__chart { flex: 1; min-height: 200px; }
{:copy-code}
```

<br>
<br>

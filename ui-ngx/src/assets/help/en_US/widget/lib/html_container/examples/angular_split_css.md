#### CSS of resizable split master–detail

```css
{:code-style="max-height: 400px;"}
.split {
  display: flex;
  height: 100%;
  box-sizing: border-box;
  font-family: 'Inter', 'Roboto', system-ui, sans-serif;
  color: #0f172a;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 4px 24px rgba(15, 23, 42, 0.06);
  overflow: hidden;
}

.split__list {
  flex: 0 0 auto;
  min-width: 160px;
  overflow: auto;
  border-right: 1px solid #eef1f6;
}

.split__title {
  margin: 0;
  padding: 16px;
  font-family: 'Roboto', sans-serif;
  font-size: 20px;
  font-weight: 500;
}

.split__items {
  list-style: none;
  margin: 0;
  padding: 0;
}

.split__item {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 16px;
  cursor: pointer;
  border-bottom: 1px solid #f1f5f9;
  transition: background 0.15s ease;
}

.split__item:hover {
  background: #f8fafc;
}

.split__item.is-active {
  background: rgba(47, 107, 255, 0.08);
  box-shadow: inset 3px 0 0 #2f6bff;
}

.split__value {
  font-weight: 700;
  color: #475569;
}

.split__divider {
  flex: 0 0 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: col-resize;
  background: #eef1f6;
  transition: background 0.15s ease;
}

.split__divider:hover {
  background: #e2e8f0;
}

.split__divider::before {
  content: '';
  width: 4px;
  height: 32px;
  background-image: radial-gradient(circle, #94a3b8 1.2px, transparent 1.4px);
  background-position: center;
  background-size: 4px 6px;
  background-repeat: repeat-y;
}

.split--dragging {
  user-select: none;
}

.split--dragging .split__list,
.split--dragging .split__detail {
  pointer-events: none;
}

.split__detail {
  flex: 1 1 0;
  min-width: 220px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 20px;
  overflow: auto;
}

.split__eyebrow {
  font-size: 11px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: #94a3b8;
}

.split__detail-title {
  margin: 2px 0 0;
  font-size: 20px;
  font-weight: 800;
}

.split__state {
  flex: 1 1 auto;
  min-height: 260px;
  border: 1px solid #eef1f6;
  border-radius: 14px;
  overflow: hidden;
}

.split__state tb-dashboard-state {
  display: block;
  width: 100%;
  height: 100%;
}

.split__empty {
  margin: auto;
  color: #94a3b8;
  font-size: 14px;
}
{:copy-code}
```

<br>
<br>

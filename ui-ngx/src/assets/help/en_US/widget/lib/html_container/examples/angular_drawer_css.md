#### CSS of slide-over device detail

```css
{:code-style="max-height: 400px;"}
.dd {
  height: 100%;
  font-family: 'Inter', 'Roboto', system-ui, sans-serif;
  color: #0f172a;
}

.dd__main {
  padding: 20px;
  box-sizing: border-box;
}

.dd__title {
  margin: 0 0 16px;
  font-family: 'Roboto', sans-serif;
  font-size: 20px;
  font-weight: 500;
}

.dd__card {
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.08), 0 1px 3px rgba(0, 0, 0, 0.12);
}

.dd__table {
  width: 100%;
}

.dd__table .mat-column-name {
  flex: 1;
}

.dd__table .mat-column-people {
  flex: 0 0 120px;
  justify-content: flex-end;
}

.dd__table .mat-mdc-row {
  cursor: pointer;
}

.dd__table .mat-mdc-row:hover {
  background: rgba(0, 0, 0, 0.04);
}

.dd__table .mat-mdc-row.is-active {
  background: rgba(47, 107, 255, 0.08);
}

.dd__drawer {
  width: 70%;
  box-sizing: border-box;
}

.dd__panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 24px;
}

.dd__panel-bar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
}

.dd__panel-eyebrow {
  font-size: 11px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: .08em;
  color: #94a3b8;
}

.dd__panel-title {
  margin: 2px 0 0;
  font-size: 22px;
  font-weight: 800;
}

.dd__close {
  border: 0;
  background: #f1f5f9;
  border-radius: 10px;
  width: 36px;
  height: 36px;
  font-size: 16px;
  cursor: pointer;
  color: #475569;
}

.dd__close:hover {
  background: #e2e8f0;
}

.dd__cards {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.card {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 14px 16px;
  border-radius: 14px;
  background: linear-gradient(180deg, #f8fafc, #eef2f7);
  border: 1px solid #e6ebf2;
}

.card__label {
  font-size: 10px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: .08em;
  color: #94a3b8;
}

.card__value {
  font-size: 18px;
  font-weight: 800;
}

.dd__actions {
  display: flex;
  gap: 10px;
}

.dd__action {
  display: inline-flex;
  flex-direction: column;
  gap: 6px;
}

.dd__btn {
  padding: 9px 16px;
  border: 1px solid #d8dee9;
  border-radius: 10px;
  background: #fff;
  color: #0f172a;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.15s ease;
}

.dd__btn:hover {
  background: #f8fafc;
}

.dd__btn:disabled {
  opacity: 0.6;
  cursor: default;
}

.dd__btn--warn {
  color: #b91c1c;
  border-color: #fecaca;
}

.dd__btn--warn:hover {
  background: #fef2f2;
}

.dd__section {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.dd__section-title {
  margin: 0;
  font-size: 13px;
  font-weight: 700;
  color: #334155;
}

.dd__state {
  height: 280px;
  border: 1px solid #eef1f6;
  border-radius: 14px;
  overflow: hidden;
}

.dd__state tb-dashboard-state {
  display: block;
  width: 100%;
  height: 100%;
}
{:copy-code}
```

<br>
<br>

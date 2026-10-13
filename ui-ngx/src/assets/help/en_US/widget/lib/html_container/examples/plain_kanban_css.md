#### CSS of kanban board

```css
{:code-style="max-height: 400px;"}
.kanban {
  display: flex;
  gap: 12px;
  height: 100%;
  box-sizing: border-box;
  padding: 16px;
  overflow-x: auto;
  background: #f9fafb;
  font-family: 'Inter', 'Roboto', system-ui, sans-serif;
  color: #111827;
  font-size: 13px;
}
.kanban__col { flex: 1 1 0; min-width: 220px; display: flex; flex-direction: column; background: #f3f4f6; border-radius: 10px; }
.kanban__head { display: flex; align-items: center; gap: 8px; padding: 12px 14px 8px; }
.kanban__title { font-size: 12px; font-weight: 600; color: #374151; }
.kanban__count { margin-left: auto; min-width: 20px; height: 20px; padding: 0 6px; display: inline-flex; align-items: center; justify-content: center; border-radius: 999px; background: #e5e7eb; color: #6b7280; font-size: 11px; font-weight: 600; }
.kanban__cards { display: flex; flex-direction: column; gap: 8px; padding: 4px 8px 12px; flex: 1; min-height: 24px; overflow-y: auto; }
.kanban__card {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  cursor: grab;
  transition: border-color 0.12s ease, box-shadow 0.12s ease;
}
.kanban__card:hover { border-color: #d1d5db; box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05); }
.kanban__name { font-weight: 500; color: #111827; }
.kanban__dot { width: 8px; height: 8px; border-radius: 50%; flex: none; }
.kanban__dot--idle { background: #9ca3af; }
.kanban__dot--active { background: #22c55e; }
.kanban__dot--maintenance { background: #f59e0b; }
.kanban__card.sortable-ghost { opacity: 0; }
.kanban__card.sortable-chosen { box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12); }
{:copy-code}
```

<br>
<br>

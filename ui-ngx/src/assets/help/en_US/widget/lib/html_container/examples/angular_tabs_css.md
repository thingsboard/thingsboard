#### CSS of composite dashboard with tab navigation

```css
{:code-style="max-height: 400px;"}
.composite {
  display: flex;
  flex-direction: column;
  gap: 12px;
  height: 100%;
  box-sizing: border-box;
  padding: 12px;
  font-family: 'Roboto', system-ui, sans-serif;
}

.tabs {
  display: inline-flex;
  gap: 4px;
  padding: 4px;
  align-self: flex-start;
  background: #eef1f6;
  border-radius: 10px;
}

.tab {
  border: 0;
  background: transparent;
  padding: 8px 16px;
  border-radius: 7px;
  font-size: 13px;
  font-weight: 600;
  color: #5b6472;
  cursor: pointer;
  transition: background 0.15s ease, color 0.15s ease;
}

.tab:hover {
  color: #1f2733;
}

.tab.is-active {
  background: #fff;
  color: #2f6bff;
  box-shadow: 0 1px 2px rgba(16, 24, 40, 0.08);
}

.container {
  flex-grow: 1;
  width: 100%;
  display: flex;
  padding: 0;
  border: 1px solid #e6eaf0;
  border-radius: 12px;
  overflow: hidden;
}

.state {
  flex-grow: 1;
  width: 100%;
  height: 100%;
  padding: 0;
}
{:copy-code}
```

<br>
<br>

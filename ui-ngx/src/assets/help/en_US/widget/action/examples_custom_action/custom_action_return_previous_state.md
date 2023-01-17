#### Function return to the previous state

```javascript
{:code-style="max-height: 400px;"}
let stateIndex = widgetContext.stateController.getStateIndex();
if (stateIndex > 0) {
    stateIndex -= 1;
    backToPrevState(stateIndex);
}

function backToPrevState(stateIndex) {
    widgetContext.stateController.navigatePrevState(stateIndex);
}
{:copy-code}
```

<br>
<br>

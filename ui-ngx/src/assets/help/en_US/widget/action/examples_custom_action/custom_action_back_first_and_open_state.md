#### Function go back to the first state, after this go to the target state

```javascript
{:code-style="max-height: 400px;"}
var stateIndex = widgetContext.stateController.getStateIndex();
while (stateIndex > 0) {
  stateIndex -= 1;
  backToPrevState(stateIndex);
}
openDashboardState('devices');

function backToPrevState(stateIndex) {
  widgetContext.stateController.navigatePrevState(stateIndex);
}

function openDashboardState(statedId) {
  var currentState = widgetContext.stateController.getStateId();
  if (currentState !== statedId) {
    var params = {};
    widgetContext.stateController.updateState(statedId, params, false);
  }
}
{:copy-code}
```

<br>
<br>

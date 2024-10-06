#### Symbol tag click action function

<div class="divider"></div>
<br/>

*function (ctx, element, event): void*

A JavaScript function invoked when user clicks on SVG element with specific tag.

**Parameters:**

<ul>
  <li><b>ctx:</b> <code>ScadaSymbolContext</code> - <a href="${siteBaseUrl}/docs${docPlatformPrefix}/user-guide/scada/scada-symbols-dev-guide/#scadasymbolcontext" target="_blank">Context</a> of the SCADA symbol.
  </li>
  <li><b>element:</b> <code>Element</code> - <a href="https://svgjs.dev/docs/3.2/getting-started/" target="_blank">SVG.js</a> element.<br>
        See the examples below to learn how to <a href="https://svgjs.dev/docs/3.2/manipulating/" target="_blank">manipulate</a> and <a href="${siteBaseUrl}/docs${docPlatformPrefix}/user-guide/scada/scada-symbols-dev-guide/#scadasymbolanimation" target="_blank">animate</a> elements.<br>
  </li>
  <li><b>event:</b> <code>Event</code> - DOM event.
  </li>
</ul>

<div class="divider"></div>

##### Examples without setting values for callAction function


**Invoke widget click action**

<br>

This JavaScript snippet demonstrates triggering a <a href="${siteBaseUrl}/docs${docPlatformPrefix}/user-guide//ui/widget-actions/#action-types" target="_blank">widget action</a> using the <a href="${siteBaseUrl}/docs${docPlatformPrefix}/user-guide/scada/scada-symbols-dev-guide/#scadasymbolcontext" target="_blank">ScadaSymbolContext API</a> when the click event occurs. The widget action will be linked to the behaviorId 'click', which defines the action that will be executed upon the event.
The behavior of this action depends on the type of widget action configured in the ThingsBoard platform (e.g., navigating to a dashboard state, updating the current state, opening a URL, etc.).

<br>

```javascript
ctx.api.callAction(event, 'click');  // Trigger widget action 'click' on event
{:copy-code}
```

<br>

This action is executed automatically upon the 'click' event, making it useful for scenarios where the user interacts with a widget by clicking on it.
*Example Use Case*

- **Navigate to Dashboard State:** If the 'click' action is configured to navigate to a new dashboard state, the user will be redirected to that state when clicking on the widget.
- **Open URL:** If configured to open a URL, clicking on the widget will take the user to a specified web resource.

<br>

**Handle device activation toggle**

The example demonstrates how to dynamically call the 'turnOn' or 'turnOff' actions based on the 'active' status from the context. The actions are implemented using the following methods from the <a href="${siteBaseUrl}/docs${docPlatformPrefix}/user-guide/scada/scada-symbols-dev-guide/#scadasymbolapi" target="_blank">Scada Symbol API</a>:

- **callAction**: *(event: Event, behaviorId: string, value?: any, observer?: Partial\<Observer\<void\>\>): void* - Triggers a specific behavior action identified by its ID, allowing for the optional passing of values and observer callbacks.

- **setValue**: *(valueId: string, value: any): void* - Updates a specific value within the `ctx.values` object and initiates all related rendering functions.

For more detailed guidelines on device interaction, consider reviewing the <a href="${siteBaseUrl}/docs${docPlatformPrefix}/user-guide/scada/scada-symbols-dev-guide/#best-practices" target="_blank">best practices</a>.

```javascript
var active = ctx.values.active; // Current active status from context
var action = active ? 'turnOn' : 'turnOff'; // Determine action based on active status

// Call the action with observer callbacks for next and error handling
ctx.api.callAction(event, action, undefined, {
  next: () => {
    // Action succeeded; toggle the 'activate' status for debugging
    ctx.api.setValue('activate', !active);
  },
  error: () => {
    // Action failed; reset the 'activate' status for debugging
    ctx.api.setValue('activate', active);
  }
});
{:copy-code}
```

<br>

This example utilizes two specific action behaviors, `turnOn` and `turnOff`, which interact with the target device based on the `active` status from the context:
1. **turnOn Behavior**
  * **Type**: Action
  * **Default Value**: `true`
  * **Description**: This behavior is triggered when the device is activated. It sends a command to the target device to turn it on, indicating that the device should be in an operational state. The default value of `true` signifies that the action is intended to activate or enable the device.
2. **turnOf Behavior**
* **Type**: Action
* **Default Value**: `false`
* **Description**: This behavior is triggered when the device is deactivated. It sends a command to the target device to turn it off, indicating that the device should be in a non-operational state. The default value of `false` signifies that the action is intended to deactivate or disable the device.


##### Example with setting values for callAction function

**Temperature Adjustment Handler**

<br>

This JavaScript code demonstrates click actions for buttons that either increase or decrease the temperature value within a predefined range.
The behavior triggered by the click event updates the time series data for the temperature and ensures that the value stays within the defined limits.
The widget uses the <a href="${siteBaseUrl}/docs${docPlatformPrefix}/user-guide/scada/scada-symbols-dev-guide/#scadasymbolcontext" target="_blank">ScadaSymbolContext API</a> to manage both temperature changes and the corresponding actions.

1. **Decrease Temperature (levelDown button)**:
  * The user clicks the levelDown button to decrease the temperature. If the system is running, the temperature decreases by a step defined by `temperatureStep`, but it will never go below the `minTemperature`.
  * The updateTemperatureState action is then triggered to update the time series value for temperature.
  * In case of an error during the action, the temperature reverts to its previous value.

<br>

```javascript
if (ctx.values.running) {
  // Retrieve the current temperature from the context values
  var temperature = ctx.values.temperature;

  // Retrieve the minimum allowable temperature from the context properties
  var minTemperature = ctx.properties.minTemperature;

  // Retrieve the step size by which the temperature should change
  var step = ctx.properties.temperatureStep;

  // Calculate the new temperature, ensuring it does not go below the minimum temperature
  var newTemperature = Math.max(minTemperature, temperature - step);

  // Set the new temperature value in the context
  ctx.api.setValue('temperature', newTemperature);

  // Trigger the action to update the temperature state, passing the new temperature value
  ctx.api.callAction(event, 'updateTemperatureState', newTemperature, {
    // In case of an error, revert to the original temperature
    error: () => {
      ctx.api.setValue('temperature', temperature);
    }
  });
}
{:copy-code}
```

<br>

2. **Increase Temperature (levelUp button)**:
  * The user clicks the levelUp button to increase the temperature. If the system is running, the temperature increases by the step, but will not exceed the `maxTemperature`.
  * The action `updateTemperatureState` is called to update the time series value for temperature.
  * If an error occurs, the previous temperature is restored to ensure consistency.

<br>

```javascript
if (ctx.values.running) {
  // Retrieve the current temperature from the context values
  var temperature = ctx.values.temperature;

  // Retrieve the maximum and minimum allowable temperature from the context properties
  var maxTemperature = ctx.properties.maxTemperature;
  var minTemperature = ctx.properties.minTemperature;

  // Retrieve the step size by which the temperature should change
  var step = ctx.properties.temperatureStep;

  // Calculate the new temperature:
  // - Add the step to the current temperature
  // - Ensure it doesn't exceed the maxTemperature
  // - If temperature is null/undefined, use minTemperature as the initial value
  var newTemperature = temperature || minTemperature === 0 ? Math.min(maxTemperature, temperature + step) : minTemperature;

  // Set the new temperature value in the context
  ctx.api.setValue('temperature', newTemperature);

  // Trigger the action to update the temperature state, passing the new temperature value
  ctx.api.callAction(event, 'updateTemperatureState', newTemperature, {
    // In case of an error, revert to the original temperature
    error: () => {
      ctx.api.setValue('temperature', temperature);
    }
  });
}
{:copy-code}
```

<br>

The `updateTemperatureState` action is triggered to update the temperature value in the system. The action is configured with the following behavior:

*Action Behavior*: 'updateTemperatureState'
  * **Type**: action
  * **Value Type**: double
  * **Default Settings**: Add time series
  * **Time Series Key**: 'temperature'

This behavior updates the time series data for the temperature key, ensuring that the new temperature value is stored and displayed correctly.

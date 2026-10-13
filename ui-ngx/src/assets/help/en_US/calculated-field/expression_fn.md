## Calculated Field TBEL Script Function

The **calculate()** function is a user-defined script that enables custom calculations using [TBEL](${siteBaseUrl}/docs${docPlatformPrefix}/user-guide/tbel/) on telemetry and attribute data.
It receives arguments configured in the calculated field setup, along with an additional `ctx` object that stores `latestTs` and provides access to all arguments.

### Function Signature

```javascript
function calculate(ctx, arg1, arg2, ...): object | object[]
```

### Supported Arguments

Arguments are passed to the function by **name** defined in the calculated field configuration.

There are three types of arguments supported in the calculated field configuration:

#### Attribute and Latest Telemetry Arguments

These arguments are single values and may be of type: boolean, int64 (long), double, string, or JSON.

#### Direct argument access via **`<argName>`**

**Example: Convert Temperature from Fahrenheit to Celsius**

```javascript
var temperatureC = (temperatureF - 32) / 1.8;
return {
  "temperatureC": toFixed(temperatureC, 2)
}
```

#### Accessing argument via **`ctx.args.<argName>`**

In addition to direct access, arguments can be accessed via the `ctx.args.<argName>` object, which includes both the `value` of an argument and its timestamp as `ts`:

```json
{
  "temperatureF": {
    "ts": 1740644636669,
    "value": 36.6
  }
}
```

Let's modify the function that converts Fahrenheit to Celsius to also return the timestamp information:

```javascript
var temperatureC = (temperatureF - 32) / 1.8;
return {
  "ts": ctx.args.temperatureF.ts,
  "values": {"temperatureC": toFixed(temperatureC, 2)}
};
```

#### Time Series Rolling Arguments

These contain time series data within a defined time window. Example format:

```json
{
  "temperature": {
    "timeWindow": {
      "startTs": 1740643762896,
      "endTs": 1740644662896
    },
    "values": [
      { "ts": 1740644350000, "value": 72.32 },
      { "ts": 1740644360000, "value": 72.86 },
      { "ts": 1740644370000, "value": 73.58 },
      { "ts": 1740644380000, "value": "NaN" }
    ]
  }
}
```

The values are always converted to type `double`, and `NaN` is used when conversion fails. One may use `isNaN(double): boolean` function to check that the value is a valid number.

**Example: Accessing time series rolling argument data**

```javascript
var startOfInterval = temperature.timeWindow.startTs;
var endOfInterval = temperature.timeWindow.endTs;
var firstItem = temperature.values[0];
var firstItemTs = firstItem.ts;
var firstItemValue = firstItem.value;
var sum = 0.0;
// iterate through all values and calculate the sum using foreach:
foreach(t: temperature) {
  if(!isNaN(t.value)) { // check that the value is a valid number;
    sum += t.value;
  }
}
// iterate through all values and calculate the sum using for loop:
sum = 0.0;
for (var i = 0; i < temperature.values.size; i++) {
  sum += temperature.values[i].value;
}
// use built-in function to calculate the sum
sum = temperature.sum();
```

##### Built-in Methods for Rolling Arguments

Time series rolling arguments support built-in functions for calculations. These functions accept an optional `ignoreNaN` boolean parameter.

| Method          | Default Behavior (`ignoreNaN = true`)               | Alternative (`ignoreNaN = false`)           |
|-----------------|-----------------------------------------------------|---------------------------------------------|
| `max()`         | Returns the highest value, ignoring NaN values.     | Returns NaN if any NaN values exist.        |
| `min()`         | Returns the lowest value, ignoring NaN values.      | Returns NaN if any NaN values exist.        |
| `mean(), avg()` | Computes the average value, ignoring NaN values.    | Returns NaN if any NaN values exist.        |
| `std()`         | Calculates the standard deviation, ignoring NaN.    | Returns NaN if any NaN values exist.        |
| `median()`      | Returns the median value, ignoring NaN values.      | Returns NaN if any NaN values exist.        |
| `count()`       | Counts values, ignoring NaN values.                 | Counts all values, including NaN.           |
| `last()`        | Returns the most recent value, skipping NaN values. | Returns the last value, even if it is NaN.  |
| `first()`       | Returns the oldest value, skipping NaN values.      | Returns the first value, even if it is NaN. |
| `sum()`         | Computes the total sum, ignoring NaN values.        | Returns NaN if any NaN values exist.        |

Usage example:

```javascript
var avgTemp = temperature.mean(); // Returns 72.92
var tempMax = temperature.max(); // Returns 73.58
var valueCount = temperature.count(); // Returns 3

var avgTempNaN = temperature.mean(false);  // Returns NaN
var tempMaxNaN = temperature.max(false);   // Returns NaN
var valueCountNaN = temperature.count(false); // Returns 4
```

This function calculates air density using `altitude` (single value) and `temperature` (time series rolling argument).

```javascript
function calculate(ctx, altitude, temperature) {
  var avgTemperature = temperature.mean(); // Get average temperature
  var temperatureK = (avgTemperature - 32) * (5 / 9) + 273.15; // Convert Fahrenheit to Kelvin

  // Estimate air pressure based on altitude
  var pressure = 101325 * Math.pow((1 - 2.25577e-5 * altitude), 5.25588);

  // Air density formula
  var airDensity = pressure / (287.05 * temperatureK);

  return {
    "airDensity": toFixed(airDensity, 2)
  };
}
```

##### Merging Time Series Arguments

Time series rolling arguments can be **merged** to align timestamps across multiple datasets.

| Method                       | Description                                                                                                           | Returns                                             | Example                                                                                                                                                                                                                                                                                                                                                                                                                                  |
|:-----------------------------|:----------------------------------------------------------------------------------------------------------------------|:----------------------------------------------------|:-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `merge(other, settings)`     | Merges with another rolling argument. Aligns timestamps and filling missing values with the previous available value. | Merged object with `timeWindow` and aligned values. | <span tb-help-popup="calculated-field/examples/merge-functions/merge_input" tb-help-popup-placement="top" trigger-text="Input"></span> <br> <span tb-help-popup="calculated-field/examples/merge-functions/merge_usage" tb-help-popup-placement="top" trigger-text="Usage"></span> <br> <span tb-help-popup="calculated-field/examples/merge-functions/merge_output" tb-help-popup-placement="top" trigger-text="Output"></span>         |
| `mergeAll(others, settings)` | Merges multiple rolling arguments. Aligns timestamps and filling missing values with the previous available value.    | Merged object with `timeWindow` and aligned values. | <span tb-help-popup="calculated-field/examples/merge-functions/merge_input" tb-help-popup-placement="top" trigger-text="Input"></span> <br> <span tb-help-popup="calculated-field/examples/merge-functions/merge_all_usage" tb-help-popup-placement="top" trigger-text="Usage"></span> <br> <span tb-help-popup="calculated-field/examples/merge-functions/merge_all_output" tb-help-popup-placement="top" trigger-text="Output"></span> |

##### Parameters

| Parameter            | Description                                                                                                                                                              |
|:---------------------|:-------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `other` or `others`  | Another rolling argument or array of rolling arguments to merge with.                                                                                                    |
| `settings`(optional) | Configuration object that supports: <ul><li>`ignoreNaN` - controls whether NaN values should be ignored.</li> <li>`timeWindow` - defines a custom time window.</li></ul> |

**Example: Freezer temperature analysis**

This function merges `temperature` data with the fridge's `defrost` status. It then analyzes the merged data to identify instances where the fridge is not in defrost mode, yet the internal air temperature is too high ( > -5° C).

```javascript
function calculate(ctx, temperature, defrost) {
  var merged = temperature.merge(defrost);
  var result = [];

  foreach(item: merged) {
    if (item.v1 > -5.0 && item.v2 == 0) {
      result.add({
        ts: item.ts,
        values: {
          issue: {
            temperature: item.v1,
            defrostState: false
          }
        }
      });
    }
  }

  return result;
}
```

The result is a list of issues that may be used to configure alarm rules:

```json
[
  {
    "ts": 1741613833843,
    "values": {
      "issue": {
        "temperature": -3.12,
        "defrostState": false
      }
    }
  },
  {
    "ts": 1741613923848,
    "values": {
      "issue": {
        "temperature": -4.16,
        "defrostState": false
      }
    }
  }
]
```

### Function return format

The return format depends on the output type configured in the calculated field settings (default: **Time Series**).

### Message timestamp

The `ctx` object also includes property `latestTs`, which represents the latest timestamp of the arguments telemetry in milliseconds.

You can use `ctx.latestTs` to set the timestamp of the resulting output explicitly when returning a time series object.

```javascript
var temperatureC = (temperatureF - 32) / 1.8;
return {
  ts: ctx.latestTs,
  values: {
    "temperatureC": toFixed(temperatureC, 2)
  }
}

```

This ensures that the calculated data point aligns with the timestamp of the triggering telemetry.

##### Time Series Output

The function must return a JSON object or array with or without a timestamp.
Examples below return 5 data points: airDensity (double), humidity (integer), hvacEnabled (boolean), hvacState (string) and configuration (JSON):

Without timestamp:

```json
{
  "airDensity": 1.06,
  "humidity": 70,
  "hvacEnabled": true,
  "hvacState": "IDLE",
  "configuration": {
    "someNumber": 42,
    "someArray": [1,2,3],
    "someNestedObject": {"key": "value"}
  }
}
```

With timestamp:

```json
{
  "ts": 1740644636669,
  "values": {
    "airDensity": 1.06,
    "humidity": 70,
    "hvacEnabled": true,
    "hvacState": "IDLE",
    "configuration": {
      "someNumber": 42,
      "someArray": [1,2,3],
      "someNestedObject": {"key": "value"}
    }
  }
}
```

Array containing multiple timestamps and different values of the `airDensity` :

```json
[
  {
    "ts": 1740644636669,
    "values": {
      "airDensity": 1.06
    }
  },
  {
    "ts": 1740644636670,
    "values": {
      "airDensity": 1.07
    }
  }
]
```

##### Attribute Output

The function must return a JSON object **without timestamp** information.
Example below return 5 data points: airDensity (double), humidity (integer), hvacEnabled (boolean), hvacState (string) and configuration (JSON):

```json
{
  "airDensity": 1.06,
  "humidity": 70,
  "hvacEnabled": true,
  "hvacState": "IDLE",
  "configuration": {
    "someNumber": 42,
    "someArray": [1,2,3],
    "someNestedObject": {"key": "value"}
  }
}
```

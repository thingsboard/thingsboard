#### Calculated field TBEL script function

The **calculate()** function is user-defined script that takes values of arguments configured in the calculated field setup and an additional `ctx` object containing all arguments.

The **calculate()** function is a user-defined script that allows you to perform custom calculations using [TBEL{:target="_blank"}](${siteBaseUrl}/docs${docPlatformPrefix}/user-guide/tbel/) on telemetry and attribute data. 
It receives user-configured arguments and an additional `ctx` object, which provides access to all arguments.

##### Function signature

```javascript
  function calculate(ctx, arg1, arg2, ...): object | object[]
```

* the function automatically receives a `ctx` object containing all arguments.
* user-defined arguments `(arg1, arg2, ...)` are passed based on the calculated field configuration.
* the function must return a JSON object (single result) or an array of objects (multiple telemetry entries).

##### Example: Air Density Calculation

This example demonstrates how to calculate air density using altitude and temperature telemetry data.

```javascript
function calculate(ctx, altitude, temperature) {
  var avgTemperature = temperature.mean(); // Get average temperature
  var temperatureK = (avgTemperature - 32) * (5 / 9) + 273.15; // Convert Fahrenheit to Kelvin
    
  // Estimate air pressure based on altitude
  var pressure = 101325 * Math.pow((1 - 2.25577e-5 * altitude), 5.25588);

  // Air density formula
  var airDensity = pressure / (287.05 * temperatureK);

  return {
    "airDensity": airDensity
  };
}
```

##### Function arguments

* `ctx` - context object containing all predefined arguments.

  Use `ctx.args.argName` to access arguments.

  ```javascript
  var altitude = ctx.args.altitude.ts;
  var temperature = ctx.args.temperature;
  ```

* `arg1, arg2, ...` - user-defined arguments configured in the calculated field setup. These arguments follow the previously described types:
  * if an argument represents the latest telemetry data or attribute, it will be passed as a direct value.

  * if an argument is a time series rolling argument, it will be provided as a time series rolling argument described above.

The **calculate()** function receives predefined arguments, which fall into two categories:

* single value arguments - represent the latest telemetry data or attribute.
  ```json
    {
        "altitude": {
           "ts": 1740644636669,
           "value": 1034
        }
    }
  ```

Use dot notation (`.`) to access argument properties.

  ```javascript
  var altitudeTimestamp = ctx.args.altitude.ts;
  var altitudeValue = ctx.args.altitude.value;
  ```

* time series rolling arguments - contain historical data within a defined time window.
  ```json
  {
    "temperature": {
        "timeWindow": {
            "startTs": 1740643762896,
            "endTs": 1740644662896
        },
        "values": [
            { "ts": 1740644355935, "value": 72.32 },
            { "ts": 1740644365935, "value": 72.86 },
            { "ts": 1740644375935, "value": 73.58 },
            { "ts": 1740644385935, "value": "NaN" }
        ]
    }
  }
  ```

  Use dot notation (`.`) to access argument properties.

  ```javascript
  var startOfInterval = temperature.timeWindow.startTs;
  var firstTimestamp = temperature.values[0].ts;
  var firstValue = temperature.values[0].value;
  ```

**Built-in methods for rolling arguments**

Time series rolling arguments provide built-in functions for calculations.
These functions accept an optional `ignoreNaN` boolean parameter, which controls how NaN values are handled.
Each function has two function signatures:

* **Without parameters:** `method()` → called **without parameters** and defaults to `ignoreNaN = true`, meaning NaN values are ignored.
* **With an explicit parameter:** `method(boolean ignoreNaN)` → called with a boolean `ignoreNaN` parameter:
  * `true` → ignores NaN values (default behavior).
  * `false` → includes NaN values in calculations.

| Method     | Default Behavior (`ignoreNaN = true`)            | Alternative (`ignoreNaN = false`)           |
|------------|--------------------------------------------------|---------------------------------------------|
| `max()`    | Returns the highest value, ignoring NaN values.  | Returns NaN if any NaN values exist.        |
| `min()`    | Returns the lowest value, ignoring NaN values.   | Returns NaN if any NaN values exist.        |
| `mean()`   | Computes the average value, ignoring NaN values. | Returns NaN if any NaN values exist.        |
| `std()`    | Calculates the standard deviation, ignoring NaN. | Returns NaN if any NaN values exist.        |
| `median()` | Returns the median value, ignoring NaN values.   | Returns NaN if any NaN values exist.        |
| `count()`  | Counts only valid (non-NaN) values.              | Counts all values, including NaN.           |
| `last()`   | Returns the most recent non-NaN value.           | Returns the last value, even if it is NaN.  |
| `first()`  | Returns the oldest non-NaN value.                | Returns the first value, even if it is NaN. |
| `sum()`    | Computes the total sum, ignoring NaN values.     | Returns NaN if any NaN values exist.        |

The following calculations are executed over the provided above arguments:

**Example usage: default (`ignoreNaN = true`)**

```javascript
var avgTemp = temperature.mean();
var tempMax = temperature.max();
var valueCount = temperature.count();
```

**Output:**

```json
{
  "avgTemp": 72.92,
  "tempMax": 73.58,
  "valueCount": 3
}
```

**Example usage: explicit (`ignoreNaN = false`)**

```javascript
var avgTemp = temperature.mean(false);  // Returns NaN if any NaN values exist
var tempMax = temperature.max(false);   // Returns NaN if any NaN values exist
var valueCount = temperature.count(false); // Counts all values, including NaN
```

**Output:**

```json
{
  "avgTemp": "NaN",
  "tempMax": "NaN",
  "valueCount": 4
}
```

Time series rolling arguments can be merged for multi-sensor analysis.

Function Signatures:

* `merge(other, settings)` - merges the current rolling argument with another rolling argument by aligning timestamps and combining values.
    Parameters:
    * `other` - another time series rolling argument to merge with. 
    * `settings` (optional) - configuration object, supports:
      * `ignoreNaN` (boolean, default true) - controls whether NaN values should be ignored.
      * `timeWindow` (object, default {}) - defines a custom time window for filtering merged values.
    Returns: an object with time window and values from each provided time series rolling arguments.

  * `mergeAll(others, settings)` - merges the current rolling argument with multiple rolling arguments by aligning timestamps and combining values.
    Parameters:
    * `others` - an array of time series rolling arguments to merge with. 
    * `settings` (optional) - same as `merge()`. 
    Returns: an object with timeWindow and aligned values.

```json
{
  "humidity": {
    "timeWindow": {
      "startTs": 1741356332086,
      "endTs": 1741357232086
    },
    "values": [{
      "ts": 1741356882759,
      "value": 43
    }, {
      "ts": 1741356918779,
      "value": 46
    }]
  },
  "pressure": {
    "timeWindow": {
      "startTs": 1741356332086,
      "endTs": 1741357232086
    },
    "values": [{
      "ts": 1741357047945,
      "value": 1023
    }, {
      "ts": 1741357056144,
      "value": 1026
    }, {
      "ts": 1741357147391,
      "value": 1025
    }]
  },
  "temperature": {
    "timeWindow": {
      "startTs": 1741356332086,
      "endTs": 1741357232086
    },
    "values": [{
      "ts": 1741356874943,
      "value": 76
    }, {
      "ts": 1741357063689,
      "value": 77
    }]
  }
}
```

**Example usage**
```javascript
var mergedData = temperature.merge(humidity, { ignoreNaN: false });
```

**Output:**
```json
{
  "mergedData": {
    "timeWindow": {
      "startTs": 1741356332086,
      "endTs": 1741357232086
    },
    "values": [{
      "ts": 1741356874943,
      "values": [76.0, "NaN"]
    }, {
      "ts": 1741356882759,
      "values": [76.0, 43.0]
    }, {
      "ts": 1741356918779,
      "values": [76.0, 46.0]
    }, {
      "ts": 1741357063689,
      "values": [77.0, 46.0]
    }]
  }
}
```

**Example usage**
```javascript
var mergedData = temperature.mergeAll([humidity, pressure], { ignoreNaN: true });
```

**Output:**
```json
{
  "mergedData": {
    "timeWindow": {
      "startTs": 1741356332086,
      "endTs": 1741357232086
    },
    "values": [{
      "ts": 1741357047945,
      "values": [76.0, 46.0, 1023.0]
    }, {
      "ts": 1741357056144,
      "values": [76.0, 46.0, 1026.0]
    }, {
      "ts": 1741357063689,
      "values": [77.0, 46.0, 1026.0]
    }, {
      "ts": 1741357147391,
      "values": [77.0, 46.0, 1025.0]
    }]
  }
}
```

##### Function return format:

The script should return a JSON object formatted according to the [ThingsBoard Telemetry Upload API](${siteBaseUrl}/docs${docPlatformPrefix}/user-guide/telemetry/#time-series-data-upload-api/). 
The return value must match one of the supported telemetry upload formats.
**Example Formats**:

Single key-value format:

```json
{
  "airDensity": 1.06
}
```

Key-value format with a timestamp:

```json
{
  "ts": 1740644636669,
  "values": {
    "airDensity": 1.06
  }
}
```

Array of telemetry entries:

```json
[
  {
    "ts": 1740644636669,
    "values": {
      "airDensity": 1.06
    }
  },
  {
    "ts": 1740644662896,
    "values": [
      { "ts": 1740644355935, "value": 72.32 },
      { "ts": 1740644365935, "value": 72.86 },
      { "ts": 1740644375935, "value": 73.58 },
      { "ts": 1740644385935, "value": "NaN" }
    ]
  }
]
```

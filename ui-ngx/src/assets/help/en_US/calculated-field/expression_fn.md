#### Calculated field TBEL script function

The **calculate()** function is a user-defined script that allows you to perform custom calculations using [TBEL{:target="_blank"}](${siteBaseUrl}/docs${docPlatformPrefix}/user-guide/tbel/) on telemetry and attribute data. 
It receives arguments configured in the calculated field setup and an additional `ctx` object, which provides access to all arguments.

##### Function signature

```javascript
  function calculate(ctx, arg1, arg2, ...): object | object[]
```

##### Argument representation in the script

Before describing how arguments are passed to the function, let's define how different argument types are **represented** inside the script.

There are two types of arguments that can be used in the function:

* single value arguments - represent the latest telemetry data or attribute.
  ```json
    {
        "altitude": {
           "ts": 1740644636669,
           "value": 1034
        }
    }
  ```

   * when accessed via `ctx.args`, they remain objects:

  ```javascript
  var altitudeTimestamp = ctx.args.altitude.ts;
  var altitudeValue = ctx.args.altitude.value;
  ```

  * when accessed as a **function parameter**, only the value is passed:

  ```javascript
  function calculate(ctx, altitude/*(single value argument)*/, temperature/*(time series rolling argument)*/) {
    // altitude = 1035
  }
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

  * when accessed via `ctx.args`, they remain rolling argument objects:

  ```javascript
  var startOfInterval = temperature.timeWindow.startTs;
  var firstTimestamp = temperature.values[0].ts;
  var firstValue = temperature.values[0].value;
  ```

  * when accessed as a **function parameter**, they are passed as rolling arguments, retaining their structure:

  ```javascript
  function calculate(ctx, altitude/*(single value argument)*/, temperature/*(time series rolling argument)*/) {
    var avgTemp = temperature.mean(); // Use rolling argument functions
  }
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

  **Usage: default (`ignoreNaN = true`)**

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

  **Usage: explicit (`ignoreNaN = false`)**
  
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

  Time series rolling arguments can be **merged** to align timestamps across multiple datasets.

  | Method                       | Description                                                                                                                                                                                                              | Parameters                                                                                                                                                                                                                                                                                                                           | Returns                                          |
  |:-----------------------------|:-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|:-------------------------------------------------|
  | `merge(other, settings)`     | Merges the current rolling argument with another rolling argument by aligning timestamps and filling missing values with the previous available value.                                                                   | <ul><li>`other` (another rolling argument)</li><li>`settings` (optional) - configuration object, supports:<ul><br/><li>`ignoreNaN` (boolean, default true) - controls whether NaN values should be ignored.</li><li>`timeWindow` (object, default {}) - defines a custom time window for filtering merged values.</li></ul></li></ul>| Merged object with timeWindow and aligned values. |
  | `mergeAll(others, settings)` | Merges the current rolling argument with multiple rolling arguments by aligning timestamps and filling missing values with the previous available value.                                                                 | <ul><li>`others` (array of rolling arguments)</li><li>`settings` (optional) - configuration object, supports:<ul><br/><li>`ignoreNaN` (boolean, default true) - controls whether NaN values should be ignored.</li><li>`timeWindow` (object, default {}) - defines a custom time window for filtering merged values.</li></ul></li></ul>| Merged object with timeWindow and aligned values.|

  **Example arguments:**

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

  **Usage:**

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

  **Usage:**

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

##### Function arguments

* `ctx` - context object that contains all provided arguments, in the representations described above.

Accessing arguments via `ctx`:

  ```javascript
  var altitude = ctx.args.altitude; // single value argument
  var temperature = ctx.args.temperature; // time series rolling argument
  ```

* `arg1, arg2, ...` - user-defined arguments configured in the calculated field setup.

How they are passed depends on their type:

* **single value arguments** are passed as raw values **(e.g., 22.5, "ON")**.
* **time series rolling arguments** are passed as objects (containing multiple values).

##### Example: Air Density Calculation

This function calculates air density using `altitude` (single value argument) and `temperature` (time series rolling argument).

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

* `altitude` is a single value, passed as number.
* `temperature` is a rolling argument, retaining its full structure.

##### Function return format

The script should return a JSON object formatted according to the [ThingsBoard Telemetry Upload API](${siteBaseUrl}/docs${docPlatformPrefix}/user-guide/telemetry/#time-series-data-upload-api/). 
The return value must match one of the supported telemetry upload formats.

The script must return data in a format compatible with ThingsBoard’s APIs. 
The correct return format depends on the calculated field output configuration:

* if latest telemetry is used for output, the function must return data according to the [Telemetry Upload API](${siteBaseUrl}/docs${docPlatformPrefix}/reference/mqtt-api/#attributes-api/).

  * without timestamps
  ```json
  {
    "airDensity": 1.06,
    "someKey": "value"
  }
  ```
  * with a timestamp:
  ```json
  {
    "ts": 1740644636669,
    "values": {
      "airDensity": 1.06,
      "someKey": "value"
     }
  }
  ```

* if attributes are used for output, the function must return data according to the [Attributes API](${siteBaseUrl}/docs${docPlatformPrefix}/user-guide/telemetry/#time-series-data-upload-api/).

  ```json
  {
    "airDensity": 1.06,
    "someKey": "value"
  }
  ```

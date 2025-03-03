#### Calculated field TBEL script function

The **calculate()** function is user-defined and receives arguments configured by the user to generate new telemetry data.
It allows you to perform custom calculations using [TBEL{:target="_blank"}](${siteBaseUrl}/docs${docPlatformPrefix}/user-guide/tbel/).

##### Function signature

```javascript
  function calculate(arg1, arg2, ...): object | object[]
```

The function automatically receives user-defined arguments **(arg1, arg2, ...)** and should return a JSON object.

##### Example: Air Density Calculation

```javascript
function calculate(altitude, temperature) {
  var avgTemperature = temperature.mean(); // Get average temperature
  var temperatureK = (avgTemperature - 32) * (5 / 9) + 273.15; // Convert Fahrenheit to Kelvin
    
  // Estimate air pressure based on altitude
  var pressure = 101325 * Math.pow((1 - 2.25577e-5 * altitude.value), 5.25588);

  // Air density formula
  var airDensity = pressure / (287.05 * temperatureK);

  return {
    "airDensity": airDensity
  };
}
```

##### Function arguments

The function receives user-configured arguments, which can be of two types:

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
  var altitudeTimestamp = altitude.ts;
  var altitudeValue = altitude.value;
  ```

* time series rolling arguments - contain historical data within a defined time window.
  ```json
  {
    "temperature": {
        "timeWindow": {
            "startTs": 1740643762896,
            "endTs": 1740644662896,
            "limit": 5000
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
These functions accept an optional 'ignoreNaN' boolean parameter, which controls how NaN values are handled.
Each method has two function signatures:

* **Without parameters:** `method()` → called **without parameters** and defaults to `ignoreNaN = true`, meaning NaN values are ignored.
* **With an explicit parameter:** `method(boolean ignoreNaN)` → called with a boolean `ignoreNaN` parameter:
  * `true` → ignores NaN values (default behavior).
  * `false` → includes NaN values in calculations.

| Method    | Default Behavior (`ignoreNaN = true`)            | Alternative (`ignoreNaN = false`)           |
|-----------|--------------------------------------------------|---------------------------------------------|
| `max()`   | Returns the highest value, ignoring NaN values.  | Returns NaN if any NaN values exist.        |
| `min()`   | Returns the lowest value, ignoring NaN values.   | Returns NaN if any NaN values exist.        |
| `mean()`  | Computes the average value, ignoring NaN values. | Returns NaN if any NaN values exist.        |
| `std()`   | Calculates the standard deviation, ignoring NaN. | Returns NaN if any NaN values exist.        |
| `median()` | Returns the median value, ignoring NaN values.   | Returns NaN if any NaN values exist.        |
| `count()` | Counts only valid (non-NaN) values.              | Counts all values, including NaN.           |
| `last()`  | Returns the most recent non-NaN value.           | Returns the last value, even if it is NaN.  |
| `first()` | Returns the oldest non-NaN value.                | Returns the first value, even if it is NaN. |
| `sum()`   | Computes the total sum, ignoring NaN values.     | Returns NaN if any NaN values exist.        |

##### The following calculations are executed over the provided above arguments:

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

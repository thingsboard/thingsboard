## Calculated Field TBEL Map Function

The **map()** function is a [TBEL](${siteBaseUrl}/docs${docPlatformPrefix}/user-guide/tbel/) script used in aggregation metrics of a calculated field.

It determines the value applied by each related entity to the aggregation.

The function receives arguments configured in the calculated field setup, along with an additional `ctx` object that stores `latestTs` and provides access to all arguments.

The function is evaluated **per related entity**, after filtering is applied.

### Function Signature

```javascript
function map(ctx, arg1, arg2, ...): number | boolean | string
```

### Supported Arguments

Arguments are passed to the function by **name** defined in the calculated field configuration.

There are two types of arguments supported in the calculated field configuration: **Attribute and Latest Telemetry Arguments**

These arguments are single values and may be of type: boolean, int64 (long), double, string, or JSON.

#### Direct argument access via **`<argName>`**

**Example: Calculate average temperature across sensors**

**Scenario**: Multiple related sensors report temperature in Fahrenheit. You want to aggregate temperature values, but the aggregation must be performed in Celsius.

**Goal**: Convert temperature to Celsius before aggregation

```javascript
var temperatureC = (temperature - 32) / 1.8;
return toFixed(temperatureC, 2);
```

Instead of creating a separate calculated field for conversion, the transformation is performed per entity, before aggregation.

#### Accessing argument via **`ctx.args.<argName>`**

In addition to direct access, arguments can be accessed via the `ctx.args.<argName>` object, which includes both the `value` of an argument and its timestamp as `ts`:

```json
{
  "temperature": {
    "ts": 1740644656761,
    "value": 33.6
  }
}
```

The `ctx.latestTs` property represents the latest timestamp across all related entities and their arguments participating in the aggregation.

**Example: Calculate average temperature**

**Scenario**: Each sensor reports temperature approximately every 10 minutes, but reporting times may vary due to network delays (up to ~30 seconds).
If a temperature value is outdated, you want the entity to remain part of the aggregation, but contribute a default value instead of a stale one.

**Goal**: Return a default value when the temperature was not reported within 1 minute of the latest aggregation timestamp.

```javascript
var ONE_MINUTE = 60 * 1000;
if ((ctx.latestTs - ctx.args.temperature.ts) > ONE_MINUTE) {
  return 0;
};
return temperature;
```

### Function return format

The returned value is passed directly to the aggregation engine:

| Aggregation function           | Expected return value |
|--------------------------------|-----------------------|
| Count, Count unique            |       any value       |
| Sum, Average, Minimum, Maximum |        number         |

Returning a value of an incompatible type may result in the entity being ignored or the aggregation producing incorrect results.

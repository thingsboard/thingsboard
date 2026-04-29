## Alarm rule condition TBEL script function

The **expression()** function is a user-defined script that enables custom condition expressions using [TBEL](${siteBaseUrl}/docs${docPlatformPrefix}/user-guide/tbel/) on telemetry and attribute data.
It receives arguments configured in the alarm rule setup, along with an additional `ctx` object that stores `latestTs` and provides access to all arguments.

### Function signature

```javascript
function expression(ctx, arg1, arg2, ...): boolean
```

### Supported arguments

There are two types of arguments supported in the alarm rule configuration: attributes and latest telemetry.

These arguments are single values and may be of type: boolean, int64 (long), double, string, or JSON.

### Usage

**Example: Convert temperature from Fahrenheit to Celsius and raise the alarm if the Celsius value is greater than 36**

```javascript
var temperatureC = (temperatureF - 32) / 1.8;
return temperatureC > 36;
```

Alternatively, use `ctx` to access the argument as an object:

```json
{
  "temperatureF": {
    "ts": 1740644636669,
    "value": 98.7
  }
}
```

You may notice that the object includes both the `value` of an argument and its timestamp as `ts`.
The `ctx` object also includes the property `latestTs`, which represents the latest timestamp of the arguments telemetry in milliseconds.

Let's modify the expression that converts Fahrenheit to Celsius to also check if the temperature's timestamp is exactly at the start of an hour:

```javascript
var temperatureC = (ctx.args.temperatureF.value - 32) / 1.8;
var temperatureTs = ctx.args.temperatureF.ts;
return temperatureC > 36 && ((temperatureTs / 1000) % 3600) == 0;
```

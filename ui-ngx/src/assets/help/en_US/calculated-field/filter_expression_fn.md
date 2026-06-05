## Calculated Field TBEL Filter Function

The **filter()** function is a [TBEL](${siteBaseUrl}/docs${docPlatformPrefix}/user-guide/tbel/) script used in aggregation metrics of a calculated field.

It receives arguments configured in the calculated field setup, along with an additional `ctx` object that stores `latestTs` and provides access to all arguments.

It allows you to include or exclude related entities from aggregation based on their telemetry or attribute values.

The filter is evaluated per related entity before the aggregation function is applied.

### Function Signature

```javascript
function filter(ctx, arg1, arg2, ...): boolean
```

### Supported Arguments

Arguments are passed to the function by **name** defined in the calculated field configuration.

There are two types of arguments supported in the calculated field configuration: **Attribute and Latest Telemetry Arguments**

These arguments are single values and may be of type: boolean, int64 (long), double, string, or JSON.

#### Direct argument access via **`<argName>`**

**Example: Count free parking spaces**

**Goal**: Include only parking spaces that are active and not occupied.

```javascript
return active == true && occupied == false;
```

Only entities that satisfy this condition will be included in the aggregation.

#### Accessing argument via **`ctx.args.<argName>`**

In addition to direct access, arguments can be accessed via the `ctx.args.<argName>` object, which includes both the `value` of an argument and its timestamp as `ts`:

```json
{
  "consumption": {
    "ts": 1740644656669,
    "value": 542.6
  }
}
```

The `ctx.latestTs` property represents the latest timestamp across all related entities and their arguments participating in the aggregation.

**Example: Calculate the total consumption across multiple related pumps**

**Scenario**: Each pump reports consumption telemetry approximately every 10 minutes, but reporting times may vary due to network delays (up to ~30 seconds).
To avoid counting outdated values, only recently updated telemetry should be included.

**Goal**: Include only pumps whose consumption value was reported within 1 minute of the latest timestamp.

```javascript
var ONE_MINUTE = 60 * 1000;
return (ctx.latestTs - ctx.args.consumption.ts) <= ONE_MINUTE;
```

### Function return format

The function **must** return a boolean:
- `true` → include entity in aggregation
- `false` → exclude entity from aggregation

Any other return type is considered invalid.

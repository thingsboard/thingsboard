## Calculated Field TBEL Filter Function

The **filter()** function is a user-defined script that enables custom calculations using [TBEL](${siteBaseUrl}/docs${docPlatformPrefix}/user-guide/tbel/) on telemetry and attribute data.
It receives arguments configured in the calculated field setup, along with an additional `ctx` object that stores `latestTs` and provides access to all arguments.

### Function Signature

```javascript
function calculate(ctx, arg1, arg2, ...): boolean
```

#### API usage limit notification templatization

<div class="divider"></div>
<br/>

Notification subject and message fields support templatization. The list of available templatization parameters depends on the template type.
See the available types and parameters below:

Available template parameters:

  * *recipientEmail* - email of the recipient;
  * *recipientFirstName* - first name of the recipient;
  * *recipientLastName* - last name of the recipient;
  * *feature* - API feature for which the limit is applied; one of: 'Device API', 'Telemetry persistence', 'Rule Engine execution', 'JavaScript functions execution', 'Email messages', 'SMS messages', 'Alarms';
  * *status* - one of: 'enabled', 'warning', 'disabled';
  * *unitLabel* - name of the limited unit; one of: 'message', 'data point', 'Rule Engine execution', 'JavaScript execution', 'email message', 'SMS message', 'alarm';
  * *limit* - the limit on used feature units;    
  * *currentValue* - current number of used units;    
  * *tenantId* - id of the tenant;
  * *tenantName* - name of the tenant;

Parameter names must be wrapped using `${...}`. For example: `${recipientFirstName}`. 
You may also modify the value of the parameter with one of the suffixes:

  * `upperCase`, for example - `${recipientFirstName:upperCase}`
  * `lowerCase`, for example - `${recipientFirstName:lowerCase}`
  * `capitalize`, for example - `${recipientFirstName:capitalize}`

<div class="divider"></div>

##### Examples

Let's assume tenant's devices pushed 8K messages with the max allowed number of 10K and warn threshold in tenant profile set to 0.8 (80%). The following template:

```text
${feature} feature - ${status:upperCase} (usage: ${currentValue} out of ${limit} ${unitLabel}s)
{:copy-code}
```

will be transformed to:

```text
Device API feature - WARNING (usage: 8000 out of 10000 messages)
{:copy-code}
```

<br>
<br>

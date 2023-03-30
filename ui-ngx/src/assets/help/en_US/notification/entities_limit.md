#### Entity count limit notification templatization

<div class="divider"></div>
<br/>

Notification subject and message fields support templatization. The list of available templatization parameters depends on the template type.
See the available types and parameters below:

Available template parameters:

  * *recipientEmail* - email of the recipient;
  * *recipientFirstName* - first name of the recipient;
  * *recipientLastName* - last name of the recipient;
  * *entityType* - one of: 'Device', 'Asset', 'User', etc.;
  * *currentCount* - the current count of entities;
  * *limit* - the limit on number of entities;    
  * *percents* - the percent from the notification rule configuration;

Parameter names must be wrapped using `${...}`. For example: `${recipientFirstName}`. 
You may also modify the value of the parameter with one of the sufixes:

  * `upperCase`, for example - `${recipientFirstName:upperCase}`
  * `lowerCase`, for example - `${recipientFirstName:lowerCase}`
  * `capitalize`, for example - `${recipientFirstName:capitalize}`

<div class="divider"></div>

##### Examples

Let's assume the tenant created 400 devices with the max allowed number is 500 and rule threshold 0.8 (80%). The following template:

```text
${entityType:capitalize}s usage: ${currentCount}/${limit} (${percents}%)
{:copy-code}
```

will be transformed to:

```text
Devices usage: 400/500 (80%)
{:copy-code}
```

<br>
<br>

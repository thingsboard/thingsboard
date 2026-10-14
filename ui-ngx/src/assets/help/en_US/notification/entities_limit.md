#### Entity count limit notification templatization

<div class="divider"></div>
<br/>

Notification subject, message and button support templatization and localization.
The list of available templatization parameters depends on the template type.
See the available types and parameters below:

Available template parameters:

* `entityType` - one of: 'Device', 'Asset', 'User', etc.;
* `currentCount` - the current count of entities;
* `limit` - the limit on number of entities;
* `percents` - the percent from the notification rule configuration;
* `tenantId` - id of the tenant;
* `tenantName` - name of the tenant;
* `recipientTitle` - title of the recipient (first and last name if specified, email otherwise);
* `recipientEmail` - email of the recipient;
* `recipientFirstName` - first name of the recipient;
* `recipientLastName` - last name of the recipient;

Parameter names must be wrapped using `${...}`. For example: `${recipientFirstName}`.
You may also modify the value of the parameter with one of the suffixes:

* `upperCase`, for example - `${recipientFirstName:upperCase}`
* `lowerCase`, for example - `${recipientFirstName:lowerCase}`
* `capitalize`, for example - `${recipientFirstName:capitalize}`

To localize the notification, use `translate` suffix: `${some.translation.key:translate}`

For example, if you have a custom translation key `custom.notifications.greetings` with value `Hello, ${recipientFirstName}!`, the template
`${custom.notifications.greetings:translate}` will be transformed to `Hello, John!`. 
The needed locale is taken from recipient's profile settings, using English by default.


<div class="divider"></div>

##### Examples

Let's assume the tenant created 400 devices with the max allowed number is 500 and rule threshold 0.8 (80%).
The following template:

```text
${entityType:capitalize}s usage: ${currentCount}/${limit} (${percents}%)
{:copy-code}
```

will be transformed to:

```text
Devices usage: 400/500 (80%)
```

<br>
<br>

#### Integration lifecycle notification templatization

<div class="divider"></div>
<br/>

Notification subject, message and button support templatization and localization.
The list of available templatization parameters depends on the template type.
See the available types and parameters below:

Available template parameters:

* `integrationType` - type of the integration;
* `integrationName` - name of the integration;
* `integrationId` - id of the integration as uuid string;
* `eventType` - one of: 'started', 'updated', 'stopped';
* `action` - one of: 'start', 'update', 'stop';
* `error` - the error text;
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

Let's assume MQTT integration 'My integration' failed to start. The following template:

```text
${integrationType} integration '${integrationName}' - ${action} failure: ${error}
{:copy-code}
```

will be transformed to:

```text
MQTT integration 'My integration' - start failure: failed to connect to MQTT broker
```

<br>
<br>

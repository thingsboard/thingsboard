#### Task processing failure notification templatization

<div class="divider"></div>
<br/>

Notification subject, message and button support templatization and localization.
The list of available templatization parameters depends on the template type.
See the available types and parameters below:

Available template parameters:

* `taskType` - the task type, e.g. 'telemetry deletion';
* `taskDescription` - the task description, e.g. 'telemetry deletion for device c4d93dc0-63a1-11ee-aa6d-f7cbc0a71325';
* `error` - the error stacktrace
* `tenantId` - the tenant id;
* `entityType` - the type of the entity to which the task is related;
* `entityId` - the id of the entity to which the task is related;
* `attempt` - the number of attempts processing the task
* `recipientEmail` - email of the recipient;
* `recipientFirstName` - first name of the recipient;
* `recipientLastName` - last name of the recipient;

Parameter names must be wrapped using `${...}`. For example: `${entityType}`.
You may also modify the value of the parameter with one of the suffixes:

* `upperCase`, for example - `${entityType:upperCase}`
* `lowerCase`, for example - `${entityType:lowerCase}`
* `capitalize`, for example - `${entityType:capitalize}`

To localize the notification, use `translate` suffix: `${some.translation.key:translate}`

For example, if you have a custom translation key `custom.notifications.greetings` with value `Hello, ${recipientFirstName}!`, the template
`${custom.notifications.greetings:translate}` will be transformed to `Hello, John!`.
The needed locale is taken from recipient's profile settings, using English by default.


<div class="divider"></div>

##### Examples

Let's assume that telemetry deletion failed for some device.
The following template:

```text
Failed to process ${taskType} for ${entityType:lowerCase} ${entityId}
{:copy-code}
```

will be transformed to:

```text
Failed to process telemetry deletion for device c4d93dc0-63a1-11ee-aa6d-f7cbc0a71325
```

<br>
<br>

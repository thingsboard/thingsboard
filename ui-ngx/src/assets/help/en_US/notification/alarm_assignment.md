#### Alarm assignment notification templatization

<div class="divider"></div>
<br/>

Notification subject, message and button support templatization and localization.
The list of available templatization parameters depends on the template type.
See the available types and parameters below:

Available template parameters:

* `alarmType` - alarm type;
* `alarmId` - the alarm id as uuid string;
* `alarmSeverity` - alarm severity (lower case);
* `alarmStatus` - the alarm status;
* `alarmOriginatorEntityType` - the entity type of the alarm originator, e.g. 'Device';
* `alarmOriginatorName` - the name of the alarm originator, e.g. 'Sensor T1';
* `alarmOriginatorLabel` - the label of the alarm originator, e.g. 'Sensor T1';
* `alarmOriginatorId` - the alarm originator entity id as uuid string;
* `assigneeTitle` - title of the assignee;
* `assigneeEmail` - email of the assignee;
* `assigneeFirstName` - first name of the assignee;
* `assigneeLastName` - last name of the assignee;
* `assigneeId` - the id of the assignee as uuid string;
* `userTitle` - title of the user who made the action;
* `userEmail` - email of the user who made the action;
* `userFirstName` - first name of the user who made the action;
* `userLastName` - last name of the user who made the action;
* `action` - one of: 'assigned', 'unassigned';
* `recipientTitle` - title of the recipient (first and last name if specified, email otherwise);
* `recipientEmail` - email of the recipient;
* `recipientFirstName` - first name of the recipient;
* `recipientLastName` - last name of the recipient;

Parameter names must be wrapped using `${...}`. For example: `${action}`.
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

Let's assume the notification about alarm with type 'High Temperature' for device 'Sensor A' was assigned
to user 'John Doe'. The following template:

```text
Alarm '${alarmType}' - ${action:upperCase}
{:copy-code}
```

will be transformed to:

```text
Alarm 'High Temperature' - ASSIGNED
```

<br/>

The following template:

```text
Alarm '${alarmType}' (${alarmSeverity:capitalize}) was assigned to user
{:copy-code}
```

will be transformed to:

```text
Alarm 'High Temperature' (Critical) was assigned to user
```

<br>
<br>

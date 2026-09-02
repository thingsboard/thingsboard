#### Alarm notification templatization

<div class="divider"></div>
<br/>

Notification subject and message fields support templatization.
The list of available templatization parameters depends on the template type.
See the available types and parameters below:

Available template parameters:

* `alarmType` - alarm type;
* `action` - one of: 'created', 'severity changed', 'acknowledged', 'cleared', 'deleted';
* `alarmId` - the alarm id as uuid string;
* `alarmSeverity` - alarm severity (lower case);
* `alarmStatus` - the alarm status;
* `alarmOriginatorEntityType` - the entity type of the alarm originator, e.g. 'Device';
* `alarmOriginatorName` - the name of the alarm originator, e.g. 'Sensor T1';
* `alarmOriginatorLabel` - the label of the alarm originator, e.g. 'Sensor T1';
* `alarmOriginatorId` - the alarm originator entity id as uuid string;
* `recipientTitle` - title of the recipient (first and last name if specified, email otherwise);
* `recipientEmail` - email of the recipient;
* `recipientFirstName` - first name of the recipient;
* `recipientLastName` - last name of the recipient;
* `details.<key>` - any key field from the alarm's details. Fox example, if details are `{"data": "Temperature is 25"}`, use `${details.data}` to access "Temperature is 25";

Parameter names must be wrapped using `${...}`. For example: `${action}`.
You may also modify the value of the parameter with one of the suffixes:

* `upperCase`, for example - `${recipientFirstName:upperCase}`
* `lowerCase`, for example - `${recipientFirstName:lowerCase}`
* `capitalize`, for example - `${recipientFirstName:capitalize}`

<div class="divider"></div>

##### Examples

Let's assume the notification about new alarm with type 'High Temperature' for device 'Sensor A'. 
The following template:

```text
Alarm '${alarmType}' - ${action:upperCase}
{:copy-code}
```

will be transformed to:

```text
Alarm 'High Temperature' - CREATED
```

<br/>

The following template:

```text
${alarmOriginatorEntityType:capitalize} '${alarmOriginatorName}'
{:copy-code}
```

will be transformed to:

```text
Device - Sensor A
```

<br>
<br>

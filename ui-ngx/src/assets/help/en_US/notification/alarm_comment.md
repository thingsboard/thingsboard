#### Alarm comment notification templatization

<div class="divider"></div>
<br/>

Notification subject and message fields support templatization. The list of available templatization parameters depends on the template type.
See the available types and parameters below:

Available template parameters:

  * *recipientEmail* - email of the recipient;
  * *recipientFirstName* - first name of the recipient;
  * *recipientLastName* - last name of the recipient;
  * *alarmType* - alarm type;
  * *alarmId* - the alarm id as uuid string;
  * *alarmSeverity* - alarm severity (lower case);
  * *alarmStatus* - the alarm status;
  * *alarmOriginatorEntityType* - the entity type of the alarm originator, e.g. 'Device';
  * *alarmOriginatorName* - the name of the alarm originator, e.g. 'Sensor T1';
  * *alarmOriginatorId* - the alarm originator entity id as uuid string;
  * *comment* - text of the comment;
  * *userName* - name of the user who made the comment;
  * *action* - one of: 'added', 'updated';

Parameter names must be wrapped using `${...}`. For example: `${action}`. 
You may also modify the value of the parameter with one of the sufixes:

  * `upperCase`, for example - `${recipientFirstName:upperCase}`
  * `lowerCase`, for example - `${recipientFirstName:lowerCase}`
  * `capitalize`, for example - `${recipientFirstName:capitalize}`

<div class="divider"></div>

##### Examples

 * Let's assume the notification about alarm with type 'High Temperature' for device 'Sensor A' was assigned to user 'John Doe'. 
   The following template:

```text
Alarm '${alarmType}' - comment ${action}
{:copy-code}
```

will be transformed to:

```text
Alarm 'High Temperature' - comment added
{:copy-code}
```

The following template:

```text
Alarm '${alarmType}' (${alarmSeverity:capitalize}) was commented
{:copy-code}
```

will be transformed to:

```text
Alarm 'High Temperature' (Critical) was commented
{:copy-code}
```

<br>
<br>

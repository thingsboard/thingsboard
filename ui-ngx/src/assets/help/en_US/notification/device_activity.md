#### Device activity notification templatization

<div class="divider"></div>
<br/>

Notification subject, message and button support templatization and localization.
The list of available templatization parameters depends on the template type.
See the available types and parameters below:

Available template parameters:

* `deviceId` - the device id as uuid string;
* `deviceName` - the device name;
* `deviceLabel` - the device label;
* `deviceType` - the device type;
* `eventType` - one of: 'inactive', 'active';
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

Let's assume the notification about inactive thermometer device 'Sensor T1'.
The following template:

```text
Device '${deviceName}' inactive
{:copy-code}
```

will be transformed to:

```text
Device 'Sensor T1' inactive
```

<br/>

The following template:

```text
${deviceType:capitalize} '${deviceName}' became ${eventType}
{:copy-code}
```

will be transformed to:

```text
Thermometer 'Sensor T1' became inactive
```

<br>
<br>

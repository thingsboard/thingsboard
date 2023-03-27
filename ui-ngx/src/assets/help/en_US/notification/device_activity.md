#### Device activity notification templatization

<div class="divider"></div>
<br/>

Notification subject and message fields support templatization. The list of available templatization parameters depends on the template type.
See the available types and parameters below:

Available template parameters:

  * *recipientEmail* - email of the recipient;
  * *recipientFirstName* - first name of the recipient;
  * *recipientLastName* - last name of the recipient;
  * *deviceId* - the device id as uuid string;
  * *deviceName* - the device name;    
  * *deviceLabel* - the device label;    
  * *deviceType* - the device type;
  * *eventType* - one of: 'inactive', 'active';

Parameter names must be wrapped using `${...}`. For example: `${recipientFirstName}`. 
You may also modify the value of the parameter with one of the suffixes:

  * `upperCase`, for example - `${recipientFirstName:upperCase}`
  * `lowerCase`, for example - `${recipientFirstName:lowerCase}`
  * `capitalize`, for example - `${recipientFirstName:capitalize}`

<div class="divider"></div>

##### Examples

Let's assume the notification about inactive thermometer device 'Sensor T1'. 
The following template:

Template message: ``

```text
Device '${deviceName}' inactive
{:copy-code}
```

will be transformed to:

```text
Device 'Sensor T1' inactive
{:copy-code}
```


<br>
The following template:

```text
${deviceType:capitalize} '${deviceName}' became ${eventType}
{:copy-code}
```

will be transformed to:

```text
Thermometer 'Sensor T1' became inactive
{:copy-code}
```
<br>
<br>

#### Rule engine notification templatization

<div class="divider"></div>
<br/>

Notification subject, message and button support templatization and localization.
The list of available templatization parameters depends on the template type.
See the available types and parameters below:

Available template parameters:

* values from the incoming message metadata referenced using the metadata key name;
* values from the incoming message data referenced using the data key name;
* `originatorType` - type of the originator, e.g. 'Device';
* `originatorId` - id of the originator
* `customerId` - id of the customer if any
* `msgType` - type of the message
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

Let's assume the incoming message to Rule node has the following data:

```json
{
  "building_1": {
    "temperature": 24
  }
}
```

The following template:

```text
Building 1: temperature is ${building_1.temperature} 
{:copy-code}
```

will be transformed to:

```text
Building 1: temperature is 24
```

<br>
<br>

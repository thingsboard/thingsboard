#### Edge connection notification templatization

<div class="divider"></div>
<br/>

Notification subject, message and button support templatization and localization.
The list of available templatization parameters depends on the template type.
See the available types and parameters below:

Available template parameters:

* `edgeId` - the edge id as uuid string;
* `edgeName` - the name of the edge;
* `eventType` - the string representation of the connectivity status: connected or disconnected;
* `recipientEmail` - email of the recipient;
* `recipientFirstName` - first name of the recipient;
* `recipientLastName` - last name of the recipient;

Parameter names must be wrapped using `${...}`. For example: `${edgeName}`.
You may also modify the value of the parameter with one of the suffixes:

* `upperCase`, for example - `${edgeName:upperCase}`
* `lowerCase`, for example - `${edgeName:lowerCase}`
* `capitalize`, for example - `${edgeName:capitalize}`

To localize the notification, use `translate` suffix: `${some.translation.key:translate}`

For example, if you have a custom translation key `custom.notifications.greetings` with value `Hello, ${recipientFirstName}!`, the template
`${custom.notifications.greetings:translate}` will be transformed to `Hello, John!`.
The needed locale is taken from recipient's profile settings, using English by default.


<div class="divider"></div>

##### Examples

Let's assume the notification about the connecting Edge into the ThingsBoard.
The following template:

```text
Edge '${edgeName}' is now ${eventType}
{:copy-code}
```

will be transformed to:

```text
Edge 'DatacenterEdge' is now connected
```

<br/>

<br>
<br>

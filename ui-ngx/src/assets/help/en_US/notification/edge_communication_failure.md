#### Edge communication failure notification templatization

<div class="divider"></div>
<br/>

Notification subject, message and button support templatization and localization.
The list of available templatization parameters depends on the template type.
See the available types and parameters below:

Available template parameters:

* `edgeId` - the edge id as uuid string;
* `edgeName` - the name of the edge;
* `failureMsg` - the string representation of the failure, occurred on the Edge;
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

Let's assume the notification about the failing of processing connection to Edge.
The following template:

```text
Edge '${edgeName}' communication failure occurred
{:copy-code}
```

will be transformed to:

```text
Edge 'DatacenterEdge' communication failure occurred
```

<br/>

The following template:

```text
Failure message: '${failureMsg}'
{:copy-code}
```

will be transformed to:

```text
Failure message: 'Failed to process edge connection!'
```

<br>
<br>

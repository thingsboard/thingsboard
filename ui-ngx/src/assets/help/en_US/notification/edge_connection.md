#### Edge connection notification templatization

<div class="divider"></div>
<br/>

Notification subject and message fields support templatization.
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

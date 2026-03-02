#### Edge communication failure notification templatization

<div class="divider"></div>
<br/>

Notification subject and message fields support templatization.
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

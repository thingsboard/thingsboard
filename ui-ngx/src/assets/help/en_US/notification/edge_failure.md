#### Edge notification templatization

<div class="divider"></div>
<br/>

Notification subject and message fields support templatization.
The list of available templatization parameters depends on the template type.
See the available types and parameters below:

Available template parameters:

* `edgeId` - the edge id as uuid string;
* `edgeName` - the name of the edge;
* `errorMsg` - the string representation of the error, occurred on the Edge;

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
Edge '${edgeName}' received error
{:copy-code}
```

will be transformed to:

```text
Edge 'DatacenterEdge' received error
```

<br/>

The following template:

```text
Error message: '${errorMsg}'
{:copy-code}
```

will be transformed to:

```text
Error message: 'Failed to process edge connection!'
```

<br>
<br>

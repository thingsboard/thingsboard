#### Resources shortage notification templatization

<div class="divider"></div>
<br/>

Notification subject and message fields support templatization.
The list of available templatization parameters depends on the template type.
See the available types and parameters below:

Available template parameters:

* `resource` - the resource name (e.g., "CPU", "RAM", "STORAGE");
* `usage` - the current usage value of the resource;
* `serviceId` - the service id (convenient in cluster setup);
* `serviceType` - the service type (convenient in cluster setup);
* `recipientEmail` - email of the recipient;
* `recipientFirstName` - first name of the recipient;
* `recipientLastName` - last name of the recipient;

Parameter names must be wrapped using `${...}`. For example: `${resource}`.
You may also modify the value of the parameter with one of the suffixes:

* `upperCase`, for example - `${resource:upperCase}`
* `lowerCase`, for example - `${resource:lowerCase}`
* `capitalize`, for example - `${resource:capitalize}`

<div class="divider"></div>

##### Examples

Let's assume there is a resource usage shortage and the system is low on free resources (CPU, RAM, or Storage).
The following template:

```text
Warning: ${resource} is critically high at ${usage}%
{:copy-code}
```

will be transformed to:

```text
Warning: CPU is critically high at 83%
```

<br>
<br>

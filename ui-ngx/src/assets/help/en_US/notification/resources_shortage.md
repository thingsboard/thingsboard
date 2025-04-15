#### Resource usage shortage notification templatization

<div class="divider"></div>
<br/>

Notification subject and message fields support templatization.
The list of available templatization parameters depends on the template type.
See the available types and parameters below:

Available template parameters:

* `cpuThreshold` - the CPU shortage threshold;
* `ramThreshold` - the RAM shortage threshold;
* `storageThreshold` - the Storage shortage threshold;

Parameter names must be wrapped using `${...}`. For example: `${entityType}`.
You may also modify the value of the parameter with one of the suffixes:

* `upperCase`, for example - `${entityType:upperCase}`
* `lowerCase`, for example - `${entityType:lowerCase}`
* `capitalize`, for example - `${entityType:capitalize}`

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

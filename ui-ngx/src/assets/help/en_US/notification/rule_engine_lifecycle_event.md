#### Rule engine lifecycle notification templatization

<div class="divider"></div>
<br/>

Notification subject and message fields support templatization.
The list of available templatization parameters depends on the template type.
See the available types and parameters below:

Available template parameters:

* `componentType` - one of: 'Rule chain', 'Rule node';
* `componentId` - the component id as uuid string;
* `componentName` - the rule chain or rule node name;
* `ruleChainId` - the rule chain id as uuid string;
* `ruleChainName` - the rule chain name;
* `eventType` - one of: 'started', 'updated', 'stopped';
* `action` - one of: 'start', 'update', 'stop';
* `error` - the error text;
* `recipientTitle` - title of the recipient (first and last name if specified, email otherwise)
* `recipientEmail` - email of the recipient;
* `recipientFirstName` - first name of the recipient;
* `recipientLastName` - last name of the recipient;

Parameter names must be wrapped using `${...}`. For example: `${recipientFirstName}`.
You may also modify the value of the parameter with one of the suffixes:

* `upperCase`, for example - `${recipientFirstName:upperCase}`
* `lowerCase`, for example - `${recipientFirstName:lowerCase}`
* `capitalize`, for example - `${recipientFirstName:capitalize}`

<div class="divider"></div>

##### Examples

Let's assume the notification about misconfigured Kafka rule node. The following template:

```text
Rule node '${componentName}' - ${action} failure:<br/>${error}
{:copy-code}
```

will be transformed to:

```text
Rule node 'Export to Kafka' - start failure:<br/>Connection refused!
```

<br>
<br>

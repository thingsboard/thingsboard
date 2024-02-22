#### Entity action notification templatization

<div class="divider"></div>
<br/>

Notification subject and message fields support templatization.
The list of available templatization parameters depends on the template type.
See the available types and parameters below:

Available template parameters:

* `entityType` - the entity type, e.g. 'Device';
* `entityId` - the entity id as uuid string;
* `entityName` - the name of the entity;
* `actionType` - one of: 'added', 'updated', 'deleted';
* `userId` - id of the user who made the action;
* `userTitle` - title of the user who made the action;
* `userEmail` - email of the user who made the action;
* `userFirstName` - first name of the user who made the action;
* `userLastName` - last name of the user who made the action;
* `recipientTitle` - title of the recipient (first and last name if specified, email otherwise);
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

Let's assume the notification about device 'T1' was added by user 'john.doe@gmail.com'.
The following template:

```text
${entityType:capitalize} was ${actionType}!
{:copy-code}
```

will be transformed to:

```text
Device was added!
```

<br/>

The following template:

```text
${entityType} '${entityName}' was ${actionType} by user ${userEmail}
{:copy-code}
```

will be transformed to:

```text
Device 'T1' was added by user john.doe@gmail.com
```

<br>
<br>

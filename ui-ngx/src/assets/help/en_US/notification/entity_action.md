#### Entity action notification templatization

<div class="divider"></div>
<br/>

Notification subject, message and button support templatization and localization.
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

To localize the notification, use `translate` suffix: `${some.translation.key:translate}`

For example, if you have a custom translation key `custom.notifications.greetings` with value `Hello, ${recipientFirstName}!`, the template
`${custom.notifications.greetings:translate}` will be transformed to `Hello, John!`. 
The needed locale is taken from recipient's profile settings, using English by default.


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

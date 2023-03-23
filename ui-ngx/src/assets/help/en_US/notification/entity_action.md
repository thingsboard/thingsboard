#### Entity action notification templatization

<div class="divider"></div>
<br/>

Notification subject and message fields support templatization. The list of available templatization parameters depends on the template type.
See the available types and parameters below:

Available template parameters:

  * *recipientEmail* - email of the recipient;
  * *recipientFirstName* - first name of the recipient;
  * *recipientLastName* - last name of the recipient;
  * *entityType* - the entity type, e.g. 'Device';
  * *entityId* - the entity id as uuid string;
  * *entityName* - the name of the entity;
  * *actionType* - one of: 'added', 'updated', 'deleted';    
  * *originatorUserId* - the user who made the action;    
  * *originatorUserName* - the user who made the action;    // WHY no EMAIL, first, last, entityLabel (if applicable)

Parameter names must be wrapped using `${...}`. For example: `${recipientFirstName}`. 
You may also modify the value of the parameter with one of the sufixes:

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
{:copy-code}
```

<br>
The following template:

```text
${entityType} '${entityName}' was ${actionType} by user '${originatorUserName}'!
{:copy-code}
```

will be transformed to:

```text
Device 'T1' was added by user 'john.doe@gmail.com'!
{:copy-code}
```

<br>
<br>

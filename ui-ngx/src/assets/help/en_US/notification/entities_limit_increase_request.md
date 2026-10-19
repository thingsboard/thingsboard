#### Entity count limit increase request notification templatization

<div class="divider"></div>
<br/>

Notification subject and message fields support templatization.
The list of available templatization parameters depends on the template type.
See the available types and parameters below:

Available template parameters:

* `entityType` - one of: 'Device', 'Asset', 'Customer', 'User', 'Dashboard', 'Rule chain', 'Edge', 'Integration', 'Converter', 'Scheduler event';
* `userEmail` - email of the user who sends the request;
* `increaseLimitActionLabel` - label of the button used to open Limits Management page, for ex: 'Set new limit';
* `increaseLimitLink` - link to the Limits Management page;
* `baseUrl` - used to construct the full URL for the Limits Management page in email notifications;
* `recipientTitle` - title of the recipient (first and last name if specified, email otherwise);
* `recipientEmail` - email of the recipient;
* `recipientFirstName` - first name of the recipient;
* `recipientLastName` - last name of the recipient;

Parameter names must be wrapped using `${...}`. For example: `${userEmail}`.
You may also modify the value of the parameter with one of the suffixes:

* `upperCase`, for example - `${userEmail:upperCase}`
* `lowerCase`, for example - `${userEmail:lowerCase}`
* `capitalize`, for example - `${userEmail:capitalize}`

To localize the notification, use `translate` suffix: `${some.translation.key:translate}`

For example, if you have a custom translation key `custom.notifications.greetings` with value `Hello, ${recipientFirstName}!`, the template
`${custom.notifications.greetings:translate}` will be transformed to `Hello, John!`.
The needed locale is taken from recipient's profile settings, using English by default.

<div class="divider"></div>

##### Examples

Let's assume the notification about the increasing limit of the maximum allowed devices for the tenant.
The following template:

```text
${userEmail} has reached the maximum number of ${entityType:lowerCase}s allowed and is requesting an increase to the ${entityType:lowerCase} limit.
{:copy-code}
```

will be transformed to:

```text
johndoe@company.com has reached the maximum number of devices allowed and is requesting an increase to the device limit.
```

<br/>

<br>
<br>

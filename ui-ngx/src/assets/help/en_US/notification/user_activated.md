#### Entity action notification templatization

<div class="divider"></div>
<br/>

Notification subject and message fields support templatization.
The list of available templatization parameters depends on the template type.
See the available types and parameters below:

Available template parameters:

* `userFullName` - full name of the user;
* `userEmail` - email of the user;
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

For example, if you have a custom translation key `custom.notifications.user-activated` with value `${userFullName} just activated his account`, the template
`${custom.notifications.user-activated:translate}` will be transformed to `John Smith just activated his account`.
The needed locale is taken from recipient's profile settings, using English by default.


<div class="divider"></div>

##### Examples

Let's assume user 'tenant@thingsboard.org' activated his account.
The following template:

```text
${userEmail} was activated
{:copy-code}
```

will be transformed to:

```text
tenant@thingsboard.org was activated
```

<br>
<br>

#### Add-on/Feature access error notification templatization

<div class="divider"></div>
<br/>

Notification subject and message fields support templatization.
The list of available templatization parameters depends on the template type.
See the available types and parameters below:

Available template parameters:

* `addon` - one of: 'Edge Computing add-on', 'Trendz Analytics add-on', 'White labeling';
* `userEmail` - email of the user who sends the request;
* `checkConfigurationActionLabel` - label of the button used to open Addon Management page or to send add-on error notification, for ex: 'Check configuration';
* `checkConfigurationLink` - link to the Addon Management page or link to trigger send add-on error action;
* `baseUrl` - used to construct the full URL for the Addon Management page or add-on error action in email notifications;
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

Let's assume the notification is reporting an error with access to the Trendz Analytics add-on.
The following template:

```text
${userEmail} was unable to access ${addon}.
{:copy-code}
```

will be transformed to:

```text
johndoe@company.com was unable to access Trendz Analytics.
```

<br/>

<br>
<br>

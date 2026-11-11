#### Add-on/Feature access request notification templatization

<div class="divider"></div>
<br/>

Notification subject and message fields support templatization.
The list of available templatization parameters depends on the template type.
See the available types and parameters below:

Available template parameters:

* `addon` - one of: 'Edge Computing add-on', 'Trendz Analytics add-on', 'White labeling';
* `userEmail` - email of the user who sends the request;
* `enableAddonActionLabel` - label of the button used to open License Management page or to send add-on request notification, for ex: 'Add to plan';
* `enableAddonLink` - link to the License Management page or link to trigger send add-on request action;
* `baseUrl` - used to construct the full URL for the License Management page or add-on request action in email notifications; 

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

Let's assume the notification requesting access to the Edge Computing add-on.
The following template:

```text
${userEmail} is requesting access to the ${addon}.
{:copy-code}
```

will be transformed to:

```text
johndoe@company.com is requesting access to the Edge Computing add-on.
```

<br/>

<br>
<br>

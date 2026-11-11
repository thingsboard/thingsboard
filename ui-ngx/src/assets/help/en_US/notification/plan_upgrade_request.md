#### Plan upgrade request notification templatization

<div class="divider"></div>
<br/>

Notification subject and message fields support templatization.
The list of available templatization parameters depends on the template type.
See the available types and parameters below:

Available template parameters:

* `planName` - the Subscription plan name (ex. 'Prototype');
* `userEmail` - email of the user who sends the request;
* `upgradePlanLink` - link to the License Management page;
* `baseUrl` - used to construct the full URL for the License Management page in email notifications; 

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

Let's assume the notification requesting upgrade to Prototype plan.
The following template:

```text
${userEmail} is unable to install a new Solution due to subscription plan restrictions and is requesting upgrade to ${planName} plan.
{:copy-code}
```

will be transformed to:

```text
johndoe@company.com is unable to install a new Solution due to subscription plan restrictions and is requesting upgrade to Prototype plan.
```

<br/>

<br>
<br>

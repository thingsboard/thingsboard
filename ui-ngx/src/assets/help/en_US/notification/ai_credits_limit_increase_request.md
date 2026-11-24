#### AI credits limit increase request notification templatization

<div class="divider"></div>
<br/>

Notification subject and message fields support templatization.
The list of available templatization parameters depends on the template type.
See the available types and parameters below:

Available template parameters:

* `userEmail` - email of the user who sends the request;
* `tenantName` - name of the tenant whose users ran out of AI credits;
* `limitDescription` - which AI credits are used up, ex. 'its monthly limit of 5,000,000 AI credits' or 'all AI credits of this installation's license';
* `increaseLimitActionLabel` - label of the button, 'Set new limit' or 'Add credits';
* `increaseLimitLink` - link to the tenant page, or to the License Management page when the installation's AI credits are used up;
* `baseUrl` - used to construct the full URL of the link in email notifications; 

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

Let's assume the notification requesting more AI credits for the tenant 'Acme' that used its monthly limit.
The following template:

```text
${userEmail} from tenant ${tenantName} has used ${limitDescription} and is requesting more AI credits.
{:copy-code}
```

will be transformed to:

```text
johndoe@company.com from tenant Acme has used its monthly limit of 5,000,000 AI credits and is requesting more AI credits.
```

<br/>

<br>
<br>

#### New platform version notification templatization

<div class="divider"></div>
<br/>

Notification subject, message and button support templatization and localization.
The list of available templatization parameters depends on the template type.
See the available types and parameters below:

Available template parameters:

* `latestVersion` - the latest platform version available;
* `latestVersionReleaseNotesUrl` - release notes link for latest version;
* `upgradeInstructionsUrl` - upgrade instructions link for latest version;
* `currentVersion` - the current platform version
* `currentVersionReleaseNotesUrl` - release notes link for current version;
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

Let's assume that new 3.5.0 version is released but currently deployed version is 3.4.4. The following template:

```text
New version ${latestVersion} is available. Current version is ${currentVersion}
{:copy-code}
```

will be transformed to:

```text
New version 3.5.0 is available. Current version is 3.4.4
```

<br>
<br>

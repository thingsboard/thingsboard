#### Markdown text function

<div class="divider"></div>
<br/>

*function (data): string*

A JavaScript function used to calculate markdown or HTML content.

**Parameters:**

<ul>
  <li><b>data:</b> <code><a href="https://github.com/thingsboard/thingsboard/blob/5bb6403407aa4898084832d6698aa9ea6d484889/ui-ngx/src/app/modules/home/components/widget/lib/maps/map-models.ts#L108" target="_blank">FormattedData[]</a></code> - An array of <a href="https://github.com/thingsboard/thingsboard/blob/5bb6403407aa4898084832d6698aa9ea6d484889/ui-ngx/src/app/modules/home/components/widget/lib/maps/map-models.ts#L108" target="_blank">FormattedData</a> objects resolved from configured datasources.<br/>
     Each object represents basic entity properties (ex. <code>entityId</code>, <code>entityName</code>)<br/>and provides access to other entity attributes/timeseries declared in widget datasource configuration.
  </li>
</ul>

**Returns:**

Should return string value presenting markdown or HTML content.

<div class="divider"></div>

##### Examples

* Display markdown with first entity name information:

```javascript
return '# Some title\n - Entity name: ' + data[0]['entityName'];
{:copy-code}
```

<ul>
<li>
Display greetings for currently logged-in user.<br>
Let's assume widget has first datasource configured using <code>Current User</code> <a target="_blank" href="${siteBaseUrl}/docs/user-guide/ui/aliases/#single-entity">Single entity</a> alias<br>
and has data keys for <code>firstName</code>, <code>lastName</code> and <code>name</code> entity fields:
</li>
</ul>

```javascript
var userEntity = data[0];
var userName;
if (userEntity.firstName || userEntity['First name']) {
  userName = userEntity.firstName || userEntity['First name'];
} else if (userEntity.lastName || userEntity['Last name']) {
  userName = userEntity.lastName || userEntity['Last name'];
} else if (userEntity.name || userEntity['Name']) {
  userName = userEntity.name || userEntity['Name'];
}

var welcomeText = 'Hi, ' + userName + '!\n\n';
return welcomeText;
{:copy-code}
```

<br>
<br>

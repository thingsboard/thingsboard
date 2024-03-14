#### Exceeded rate limits notification templatization

<div class="divider"></div>
<br/>

Notification subject and message fields support templatization.
The list of available templatization parameters depends on the template type.
See the available types and parameters below:

Available template parameters:

* `api` - rate-limited API label; one of: 'REST API requests', 'REST API requests per customer', 'transport messages', 
  'transport messages per device', 'Cassandra queries', 'WS updates per session', 'notification requests', 'notification requests per rule',
  'entity version creation', 'entity version load', 'Edge events', 'Edge events per edge', 'Edge uplink messages', 'Edge uplink messages per edge';
* `limitLevelEntityType` - entity type of the limit level entity, e.g. 'Tenant', 'Device', 'Notification rule', 'Customer', etc.;
* `limitLevelEntityId` - id of the limit level entity;
* `limitLevelEntityName` - name of the limit level entity;
* `tenantId` - id of the tenant;
* `tenantName` - name of the tenant;
* `recipientTitle` - title of the recipient (first and last name if specified, email otherwise);
* `recipientEmail` - email of the recipient;
* `recipientFirstName` - first name of the recipient;
* `recipientLastName` - last name of the recipient;

Parameter names must be wrapped using `${...}`. For example: `${recipientFirstName}`.
You may also modify the value of the parameter with one of the suffixes:

* `upperCase`, for example - `${recipientFirstName:upperCase}`
* `lowerCase`, for example - `${recipientFirstName:lowerCase}`
* `capitalize`, for example - `${recipientFirstName:capitalize}`

<div class="divider"></div>

##### Examples

Let's assume customer 'Customer A' exceeded rate limit for per-customer REST API requests. The following template:

```text
Rate limits for ${api} exceeded for ${limitLevelEntityType:lowerCase} '${limitLevelEntityName}'
{:copy-code}
```

will be transformed to:

```text
Rate limits for REST API requests per customer exceeded for customer 'Customer A'
```

<br>
<br>

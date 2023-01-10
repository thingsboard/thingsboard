#### Clear alarm details builder function

<div class="divider"></div>
<br/>

*function Details(msg, metadata, msgType): any*

JavaScript function generating **Alarm Details** object to update existing one. Used for storing additional parameters inside Alarm.<br>
For example you can save attribute name/value pair from Original Message payload or Metadata.

**Parameters:**

{% include rulenode/common_node_script_args %}

**Returns:**

Should return the object presenting **Alarm Details**.

Current Alarm Details can be accessed via `metadata.prevAlarmDetails`.<br>
**Note** that `metadata.prevAlarmDetails` is a raw String field, and it needs to be converted into object using this construction:

```javascript
var details = {};
if (metadata.prevAlarmDetails) {
  // remove prevAlarmDetails from metadata
  delete metadata.prevAlarmDetails;
  details = JSON.parse(metadata.prevAlarmDetails);
}
{:copy-code}
```

<div class="divider"></div>

##### Examples

<ul>
<li>
Take <code>count</code> property from previous Alarm and increment it.<br>
Also put <code>temperature</code> attribute from inbound Message payload into Alarm details:
</li>
</ul>

```javascript
var details = {temperature: msg.temperature, count: 1};

if (metadata.prevAlarmDetails) {
  var prevDetails = JSON.parse(metadata.prevAlarmDetails);
  // remove prevAlarmDetails from metadata
  delete metadata.prevAlarmDetails;
  if (prevDetails.count) {
    details.count = prevDetails.count + 1;
  }
}

return details;
{:copy-code}
```

<br>

More details about Alarms can be found in [this tutorial{:target="_blank"}](${siteBaseUrl}/docs/user-guide/alarms/).

You can see the real life example, where this node is used, in the next tutorial:

- [Create and Clear Alarms{:target="_blank"}](${siteBaseUrl}/docs/user-guide/rule-engine-2-0/tutorials/create-clear-alarms/)

<br>
<br>

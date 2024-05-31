#### Create alarm details builder function

<div class="divider"></div>
<br/>

*function Details(msg, metadata, msgType): any*

JavaScript function generating **Alarm Details** object. Used for storing additional parameters inside Alarm.<br>
For example you can save attribute name/value pair from Original Message payload or Metadata.

**Parameters:**

{% include rulenode/common_node_script_args %}

**Returns:**

Should return the object presenting **Alarm Details**.

**Optional:** previous Alarm Details can be accessed via `metadata.prevAlarmDetails`.<br>
If previous Alarm does not exist, this field will not be present in Metadata. **Note** that `metadata.prevAlarmDetails`<br>
is a raw String field, and it needs to be converted into object using this construction:

```javascript
var details = {};
if (metadata.prevAlarmDetails != null) {
  details = JSON.parse(metadata.prevAlarmDetails);
  // remove prevAlarmDetails from metadata
  metadata.remove('prevAlarmDetails');
  //now metadata is the same as it comes IN this rule node
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

if (metadata.prevAlarmDetails != null) {
  var prevDetails = JSON.parse(metadata.prevAlarmDetails);
  // remove prevAlarmDetails from metadata
  metadata.remove('prevAlarmDetails');
  if (prevDetails.count != null) {
    details.count = prevDetails.count + 1;
  }
}

return details;
{:copy-code}
```

<br>

More details about Alarms can be found in [this tutorial{:target="_blank"}](${siteBaseUrl}/docs${docPlatformPrefix}/user-guide/alarms/).

You can see the real life example, where this node is used, in the next tutorial:

- [Create and Clear Alarms{:target="_blank"}](${siteBaseUrl}/docs${docPlatformPrefix}/user-guide/rule-engine-2-0/tutorials/create-clear-alarms/)

<br>
<br>

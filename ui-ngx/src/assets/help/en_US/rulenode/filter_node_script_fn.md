#### Filter message function

<div class="divider"></div>
<br/>

*function Filter(msg, metadata, msgType): boolean*

JavaScript function evaluating **true/false** condition on incoming Message.

**Parameters:**

{% include rulenode/common_node_script_args %}

**Returns:**

Should return `boolean` value. If `true` - send Message via **True** chain, otherwise **False** chain is used.

<div class="divider"></div>

##### Examples

* Forward all messages with `temperature` value greater than `20` to the **True** chain and all other messages to the **False** chain:

```javascript
return msg.temperature > 20;
{:copy-code}
```

* Forward all messages with type `ATTRIBUTES_UPDATED` to the **True** chain and all other messages to the **False** chain:

```javascript
if (msgType === 'ATTRIBUTES_UPDATED') {
    return true;
} else {
    return false;
}
{:copy-code}
```

<ul>
<li>Send message to the <strong>True</strong> chain if the following conditions are met.<br>Message type is <code>POST_TELEMETRY_REQUEST</code> and<br>
(device type is <code>vehicle</code> and <code>humidity</code> value is greater than <code>50</code> or<br>
device type is <code>controller</code> and <code>temperature</code> value is greater than <code>20</code> and <code>humidity</code> value is greater than <code>60</code>).<br>
Otherwise send message to the <strong>False</strong> chain:
</li>
</ul>

```javascript
if (msgType === 'POST_TELEMETRY_REQUEST') {
  if (metadata.deviceType === 'vehicle') {
    return msg.humidity > 50;
  } else if (metadata.deviceType === 'controller') {
    return msg.temperature > 20 && msg.humidity > 60;
  }
}
return false;
{:copy-code}
```

<br>

You can see real life example, how to use this node in those tutorials:

- [Create and Clear Alarms{:target="_blank"}](${siteBaseUrl}/docs/user-guide/rule-engine-2-0/tutorials/create-clear-alarms/#node-a-filter-script)
- [Reply to RPC Calls{:target="_blank"}](${siteBaseUrl}/docs/user-guide/rule-engine-2-0/tutorials/rpc-reply-tutorial#add-filter-script-node)

<br>
<br>


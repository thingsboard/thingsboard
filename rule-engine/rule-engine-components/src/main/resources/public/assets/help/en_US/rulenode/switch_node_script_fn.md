#### Switch message function

<div class="divider"></div>
<br/>

*function Switch(msg, metadata, msgType): string[]*

JavaScript function computing **an array of next Relation names** for incoming Message.

**Parameters:**

{% include rulenode/common_node_script_args %}

**Returns:**

Should return an array of `string` values presenting **next Relation names** where Message should be routed.<br>
If returned array is empty - message will not be routed to any Node and discarded.

<div class="divider"></div>

##### Examples

<ul>
<li>
Forward all messages with <code>temperature</code> value greater than <code>30</code> to the <strong>'High temperature'</strong> chain,<br>
with <code>temperature</code> value lower than <code>20</code> to the <strong>'Low temperature'</strong> chain and all other messages<br>
to the <strong>'Normal temperature'</strong> chain:
</li>
</ul>

```javascript
if (msg.temperature > 30) {
    return ['High temperature'];
} else if (msg.temperature < 20) {
    return ['Low temperature'];
} else {
    return ['Normal temperature'];
}
{:copy-code}
```

<ul>
  <li>
    For messages with type <code>POST_TELEMETRY_REQUEST</code>:
      <ul>
        <li>
          if <code>temperature</code> value lower than <code>18</code> forward to the <strong>'Low temperature telemetry'</strong> chain,
        </li>
        <li>
          otherwise to the <strong>'Normal temperature telemetry'</strong> chain.
        </li>
      </ul>
    For messages with type <code>POST_ATTRIBUTES_REQUEST</code>:<br>
      <ul>
        <li>
            if <code>currentState</code> value is <code>IDLE</code> forward to the <strong>'Idle State'</strong> and <strong>'Update State Attribute'</strong> chains,
        </li>
        <li>
            if <code>currentState</code> value is <code>RUNNING</code> forward to the <strong>'Running State'</strong> and <strong>'Update State Attribute'</strong> chains,
        </li>
        <li>
            otherwise to the <strong>'Unknown State'</strong> chain.
        </li>
      </ul>
    For all other message types - discard the message (do not route to any Node).
  </li>
</ul>

```javascript
if (msgType === 'POST_TELEMETRY_REQUEST') {
  if (msg.temperature < 18) {
    return ['Low Temperature Telemetry'];
  } else {
    return ['Normal Temperature Telemetry'];
  }
} else if (msgType === 'POST_ATTRIBUTES_REQUEST') {
  if (msg.currentState === 'IDLE') {
    return ['Idle State', 'Update State Attribute'];
  } else if (msg.currentState === 'RUNNING') {
    return ['Running State', 'Update State Attribute'];
  } else {
    return ['Unknown State'];
  }
}
return [];
{:copy-code}
```

<br>

You can see real life example, how to use this node in this tutorial:

- [Data function based on telemetry from 2 devices{:target="_blank"}](${baseUrl}/docs/user-guide/rule-engine-2-0/tutorials/function-based-on-telemetry-from-two-devices#delta-temperature-rule-chain)

<br>
<br>

#### Message to string function

<div class="divider"></div>
<br/>

*function toString(msg, metadata, msgType): string*

JavaScript function transforming incoming Message to String for further logging to the server log file.

**Parameters:**

{% include rulenode/common_node_script_args %}

**Returns:**

Should return `string` value used for logging to the server log file.

<div class="divider"></div>

##### Examples

* Create string message containing incoming message and incoming metadata values:

```javascript
return 'Incoming message:\n' + JSON.stringify(msg) + 
       '\nIncoming metadata:\n' + JSON.stringify(metadata);
{:copy-code}
```

<br>

You can see real life example, how to use this node in this tutorial:

- [Reply to RPC Calls{:target="_blank"}](${siteBaseUrl}/docs/user-guide/rule-engine-2-0/tutorials/rpc-reply-tutorial#log-unknown-request)

<br>
<br>

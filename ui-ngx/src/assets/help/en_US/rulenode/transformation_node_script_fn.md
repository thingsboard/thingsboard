#### Transform message function

<div class="divider"></div>
<br/>

*function Transform(msg, metadata, msgType): {msg: object, metadata: object, msgType: string}*

JavaScript function transforming input Message payload, Metadata or Message type.  

**Parameters:**

{% include rulenode/common_node_script_args %}

**Returns:**

Should return the object with the following structure:

```javascript
{ 
   msg?: {[key: string]: any},
   metadata?: {[key: string]: string},
   msgType?: string
}
```

All fields in resulting object are optional and will be taken from original message if not specified.

<div class="divider"></div>

##### Examples

* Change message type to `CUSTOM_REQUEST`:

```javascript
return { msgType: 'CUSTOM_REQUEST' };
{:copy-code}
```

<ul>
  <li>Change message type to <code>CUSTOM_UPDATE</code>,<br/>add additional attribute <strong><em>version</em></strong> into payload with value <strong><em>v1.1</em></strong>,<br/>change <strong><em>sensorType</em></strong> attribute value in Metadata to <strong><em>roomTemp</em></strong>:</li>
</ul>

```javascript
var newType = "CUSTOM_UPDATE";
msg.version = "v1.1";
metadata.sensorType = "roomTemp"
return {msg: msg, metadata: metadata, msgType: newType};
{:copy-code}
```

<br>

You can see real life example, how to use this node in those tutorials:

- [Transform incoming telemetry{:target="_blank"}](${siteBaseUrl}/docs/user-guide/rule-engine-2-0/tutorials/transform-incoming-telemetry/)
- [Reply to RPC Calls{:target="_blank"}](${siteBaseUrl}/docs/user-guide/rule-engine-2-0/tutorials/rpc-reply-tutorial#add-transform-script-node)

<br>
<br>

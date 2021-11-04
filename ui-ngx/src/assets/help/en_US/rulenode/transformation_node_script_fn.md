#### Transform message function

<div class="divider"></div>
<br/>

*function Transform(msg, metadata, msgType): {msg: object, metadata: object, msgType: string}*

The JavaScript function to transform input Message payload, Metadata and/or Message type to the output message.  

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

* Add sum of two fields ('a' and 'b') as a new field ('sum') of existing message:

```javascript
if(typeof msg.a !== "undefined" && typeof msg.b !== "undefined"){
    msg.sum = msg.a + msg.b;
}
return {msg: msg};
```

* Transform value of the 'temperature' field from °F to °C:

```javascript
msg.temperature = (msg.temperature - 32) * 5 / 9;
return {msg: msg};
```

* Replace the incoming message with the new message that contains only one field - count of properties in the original message:

```javascript
var newMsg = {
    count: Object.keys(msg).length
};
return {msg: newMsg};
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

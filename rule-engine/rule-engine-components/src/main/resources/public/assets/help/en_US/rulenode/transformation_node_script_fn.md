#### Transform message function

<div class="divider"></div>
<br/>

*function (msg, metadata msgType): {msg: object, metadata: object, msgType: string}*

JavaScript function transforming input Message payload, Metadata or Message type.  

**Parameters:**

<ul>
  <li><b>msg:</b> <code>{[key: string]: any}</code> - is a Message payload key/value object.
  </li>
  <li><b>metadata:</b> <code>{[key: string]: string}</code> - is a Message metadata key/value object.
  </li>
  <li><b>msgType:</b> <code>string</code> - is a string Message type. See <a href="https://github.com/thingsboard/thingsboard/blob/ea039008b148453dfa166cf92bc40b26e487e660/ui-ngx/src/app/shared/models/rule-node.models.ts#L338" target="_blank">MessageType</a> enum for common used values.
  </li>
</ul>

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

The following transform function will perform all necessary modifications:

```javascript
var newType = "CUSTOM_UPDATE";
msg.version = "v1.1";
metadata.sensorType = "roomTemp"
return {msg: msg, metadata: metadata, msgType: newType};
{:copy-code}
```

You can see real life example, how to use this node in those tutorials:

- [Transform incoming telemetry{:target="_blank"}](${baseUrl}/docs/user-guide/rule-engine-2-0/tutorials/transform-incoming-telemetry/)
- [Reply to RPC Calls{:target="_blank"}](${baseUrl}/docs/user-guide/rule-engine-2-0/tutorials/rpc-reply-tutorial#add-transform-script-node)



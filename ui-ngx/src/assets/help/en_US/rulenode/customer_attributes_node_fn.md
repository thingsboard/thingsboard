#### Name of popup

<div class="divider"></div>
<br/>

*function Filter(msg, metadata, msgType): boolean

<a href="https://github.com/thingsboard/thingsboard/blob/ea039008b148453dfa166cf92bc40b26e487e660/ui-ngx/src/app/shared/models/rule-node.models.ts#L338" target="_blank">TBEL</a>function defines a boolean expression based on the incoming Message and Metadata.

**Parameters:**

<ul>
  <li><b>msg:</b> <code>{[key: string]: any}</code> - is a Message payload key/value object.
  </li>
  <li><b>metadata:</b> <code>{[key: string]: string}</code> - is a Message metadata key/value map, where both keys and values are strings.
  </li>
  <li><b>msgType:</b> <code>string</code> - is a string containing Message type. See MessageType enum for common used values.
  </li>
</ul>

Enable 'debug mode' for your rule node to see the messages that arrive in near real-time. See Debugging for more information.

**Parameters:**
Must return a boolean value. If true - routes Message to subsequent rule nodes that are related via True link, otherwise sends Message to rule nodes related via False link. Uses 'Failure' link in case of any failures to evaluate the expression.

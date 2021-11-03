<ul>
  <li><b>msg:</b> <code>{[key: string]: any}</code> - is a Message payload key/value object.
  </li>
  <li><b>metadata:</b> <code>{[key: string]: string}</code> - is a Message metadata key/value map, where both keys and values are strings.
  </li>
  <li><b>msgType:</b> <code>string</code> - is a string containing Message type. See <a href="https://github.com/thingsboard/thingsboard/blob/ea039008b148453dfa166cf92bc40b26e487e660/ui-ngx/src/app/shared/models/rule-node.models.ts#L338" target="_blank">MessageType</a> enum for common used values.
  </li>
</ul>

Enable 'debug mode' for your rule node to see the messages that arrive in near real-time. 
See <a href="https://thingsboard.io/docs/user-guide/rule-engine-2-0/overview/#debugging" target="_blank">Debugging</a> for more information.
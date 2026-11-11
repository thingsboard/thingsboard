#### Downlink data converter encoder function

<div class="divider"></div>
<br/>

*function Encoder(msg, metadata, msgType, integrationMetadata): {msg: object, metadata: object, msgType: string}*

JavaScript function used to transform the incoming rule engine message and its metadata to the format that is used by corresponding Integration.

**Parameters:**

<ul>
  <li><b>msg:</b> <code>{[key: string]: any}</code> - JSON with rule engine msg.
  </li>
  <li><b>metadata:</b> <code>{[key: string]: string}</code> - list of key-value pairs with additional data about the message (produced by the rule engine).
  </li>
  <li><b>msgType:</b> <code>{[key: string]: string}</code> - Rule Engine message type.
        See <a href="${siteBaseUrl}/docs${docPlatformPrefix}/reference/rule-engine/message-types/" target="_blank">predefined message types</a> for more details.
  </li>
  <li><b>integrationMetadata:</b> <code>{[key: string]: string}</code> - key-value map with some integration specific fields.<br>
        You can configure additional metadata for each integration in the integration details.
  </li>
</ul>


**Returns:**

Should return a valid JSON document with the following structure:

<br>

<div style="padding-left: 64px;"
     tb-help-popup="converter/examples/encoder/json_output"
     tb-help-popup-placement="top"
     [tb-help-popup-style]="{maxHeight: '50vh', maxWidth: '50vw'}"
     trigger-style="font-size: 16px;"
     trigger-text="Example json output">
</div>

<div class="divider"></div>

##### Examples

###### Encode payload for further sending to an external MQTT broker

Let’s assume an example where temperature and humidity upload frequency attributes are updated via platform REST API<br>
and you would like to push this update to an external MQTT broker (TTN, Mosquitto, AWS IoT, etc).<br>
You may also want to include the “firmwareVersion” attribute value that was configured long time ago and is not present<br>
in this particular request. The topic to push the update should contain the device name.

<table style="max-width: 500px;">
<thead>
<tr>
<th style="max-width: 300px; padding-left: 22px;">
<b>Input arguments</b>
</th>
<th style="max-width: 200px; padding-left: 22px;">
<b>Encoder function</b>
</th>
</tr>
</thead>
<tbody>
<tr>
<td>
<span tb-help-popup="converter/examples/encoder/example1/message" tb-help-popup-placement="top" trigger-style="font-size: 16px;" trigger-text="msg"></span><br><br>
<span tb-help-popup="converter/examples/encoder/example1/metadata" tb-help-popup-placement="top" trigger-style="font-size: 16px;" trigger-text="metadata" [tb-help-popup-style]="{maxWidth: '600px'}"></span><br><br>
<span tb-help-popup-content="**ATTRIBUTES_UPDATED**" tb-help-popup-placement="top" trigger-style="font-size: 16px;" trigger-text="msgType"></span><br><br>
<span tb-help-popup="converter/examples/encoder/example1/integration_metadata" tb-help-popup-placement="top" trigger-style="font-size: 16px;" trigger-text="integrationMetadata" [tb-help-popup-style]="{maxWidth: '600px'}"></span>
</td>
<td>
<span tb-help-popup="converter/examples/encoder/example1/encoder_fn" tb-help-popup-placement="top" trigger-style="font-size: 16px; line-height: 120px;" trigger-text="Encoder function"></span>
</td>
</tr>
</tbody>
</table>

<br>
<br>

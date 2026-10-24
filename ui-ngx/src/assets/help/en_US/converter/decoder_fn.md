#### Uplink data converter decoder function

<div class="divider"></div>
<br/>

*function Decoder(payload, metadata): object | object[]*

JavaScript function used to parse and transform uplink message from the integration to the common format used by the platform.

**Parameters:**

<ul>
  <li><b>payload:</b> <code>any</code> - is a <b>byte array</b> that contains the incoming message produced by the corresponding Integration.<br>
    Integration may produce payload with one of the following content types: JSON, TEXT and Binary(Base64). 
    The content type is basically a hint for storing the debug events and it does not impact the decoder function. 
    The decoder may use <i>decodeToString</i> and <i>decodeToJson</i> functions to convert the byte array to String or JSON objects.<br/> 
    Most of the integrations (e.g. SigFox, LORIOT, ChirpStack, The Things Stack) always produce JSON payload 
    that wraps the binary device payload with useful metadata, like RSSI, SNR, etc.<br/>
    HTTP based integrations and CoAP integration determine content type of the payload based on the request headers. 
    Single integration may produce JSON, TEXT or BINARY payload depending on the request <br/> 
    MQTT integration payload is always of type BINARY, since before MQTT 3.x has no content-type in the MQTT publish message.
  </li>
  <li><b>metadata:</b> <code>{[key: string]: string}</code> - is a metadata key/value map with some integration specific fields.<br>
     You can configure additional metadata for each integration in the integration details. 
  </li>
</ul>

**Returns:**

Must return a valid JSON document with the following requirements:

 * **must** contain a pair of either **deviceName** and **deviceType** or **assetName** and **assetType** properties.
   Those properties identify device or asset, since the device and asset names are unique in scope of tenant.
   The platform will lookup existing device/asset using those parameters. 
   If such an entity is not found and 'Allow to create devices or assets' setting is enabled for the integration, platform will create new entity.
   The DevEUI, MAC address or other unique identifiers are often used as device names. 
   See **deviceLabel** to add a non-unique, user-friendly label that you may use instead of device name on the dashboards.
 * may contain **attributes** object that represents a set of server-side attributes to assign to the device/asset.
 * may contain **telemetry** object/array that represents time-series data of the device/asset.   
 * may contain **customerName** property. The platform will use it to automatically assign the device to the customer. 
   Creates a new customer if customer with such a name does not exist.
   The assignment will happen only during the process of the device or asset creation by the current integration (see the first requirement).
   In other words, the platform will ignore this parameter if device or asset already exists.
 * may contain **groupName** property. The platform will use it to automatically assign the device to a certain entity group.
   Creates a new group if group with such a name does not exist. 
   The group will be created in scope of the tenant (default) or customer (if **customerName** property is present) 
   The assignment will happen only during the process of the device or asset creation by the current integration (see the first requirement).
   In other words, the platform will ignore this parameter if device or asset already exists.
 * may contain **deviceLabel** or **assetLabel** property. 
   Useful to create non-unique user-friendly labels for devices/assets that you may use on the dashboards.  
   The assignment will happen only during the process of the device or asset creation by the current integration (see the first requirement).
   In other words, the platform will ignore this parameter if device or asset already exists.
   

Let's review few decoder output examples below. 

Simple JSON that contains device name, type, 'serialNumber' attribute and few telemetry values.

<br>

<div style="padding-left: 64px;"
     tb-help-popup="converter/examples/decoder/simple_json_output"
     tb-help-popup-placement="top"
     [tb-help-popup-style]="{maxHeight: '50vh', maxWidth: '50vw'}"
     trigger-style="font-size: 16px;"
     trigger-text="Simple json output">
</div>

<br>

Similar JSON that also contains device label, customer and group name.

<br>

<div style="padding-left: 64px;"
     tb-help-popup="converter/examples/decoder/label_json_output"
     tb-help-popup-placement="top"
     [tb-help-popup-style]="{maxHeight: '50vh', maxWidth: '50vw'}"
     trigger-style="font-size: 16px;"
     trigger-text="Output with device label, customer and group names">
</div>

<br>

Same JSON that also contains the timestamp of the event. 
Platform expects timestamp as a Unix epoch time in milliseconds. 
Otherwise, the server timestamp is used.

<br>

<div style="padding-left: 64px;"
     tb-help-popup="converter/examples/decoder/simple_json_output_with_ts"
     tb-help-popup-placement="top"
     [tb-help-popup-style]="{maxHeight: '50vh', maxWidth: '50vw'}"
     trigger-style="font-size: 16px;"
     trigger-text="Output with custom timestamp">
</div>

<br>

Finally, the output of data conversion may be an array of objects that contain multiple devices/assets. 
Each of the objects may also contain multiple time-series data points with different timestamps:

<br>

<div style="padding-left: 64px;"
     tb-help-popup="converter/examples/decoder/json_array_output"
     tb-help-popup-placement="top"
     [tb-help-popup-style]="{maxHeight: '50vh', maxWidth: '50vw'}"
     trigger-style="font-size: 16px;"
     trigger-text="Example json array output">
</div>

<div class="divider"></div>

##### Examples

See table with the examples that contain input parameters, decoder function and expected output.

<table style="max-width: 1200px;">
<thead>
<tr>
<th style="max-width: 150px; padding-left: 22px;">
<b>Name</b>
</th>
<th style="max-width: 150px; padding-left: 22px;">
<b>Content type</b>
</th>
<th style="max-width: 300px; padding-left: 22px;">
<b>Notes</b>
</th>
<th style="max-width: 200px; padding-left: 22px;">
<b>Input</b>
</th>
<th style="max-width: 200px; padding-left: 22px;">
<b>Decoder</b>
</th>
<th style="max-width: 200px; padding-left: 22px;">
<b>Output</b>
</th>
</tr>
</thead>
<tbody>
<tr>
<td>
<b>Simple JSON with date</b>
</td>
<td>
<b>JSON</b>
</td>
<td>
<b>Parse specific JSON format with string representation of the timestamp</b>
</td>
<td>
<span tb-help-popup="converter/examples/decoder/simple-json/payload" tb-help-popup-placement="top" trigger-style="font-size: 16px; line-height: 75px;" trigger-text="payload"></span>
</td>
<td>
<span tb-help-popup="converter/examples/decoder/simple-json/decoder_fn" tb-help-popup-placement="top" trigger-style="font-size: 16px; line-height: 75px;" trigger-text="Decoder function"></span>
</td>
<td>
<span tb-help-popup="converter/examples/decoder/simple-json/output" tb-help-popup-placement="top" trigger-style="font-size: 16px; line-height: 75px;" trigger-text="Decoder output"></span>
</td>
</tr>
<tr>
<td>
<b>Simple CSV</b>
</td>
<td>
<b>TEXT</b>
</td>
<td>
<b>Parse CSV data</b>
</td>
<td>
<span tb-help-popup="converter/examples/decoder/simple-csv/payload" tb-help-popup-placement="top" trigger-style="font-size: 16px; line-height: 75px;" trigger-text="payload"></span>
</td>
<td>
<span tb-help-popup="converter/examples/decoder/simple-csv/decoder_fn" tb-help-popup-placement="top" trigger-style="font-size: 16px; line-height: 75px;" trigger-text="Decoder function"></span>
</td>
<td>
<span tb-help-popup="converter/examples/decoder/simple-csv/output" tb-help-popup-placement="top" trigger-style="font-size: 16px; line-height: 75px;" trigger-text="Decoder output"></span>
</td>
</tr>
<tr>
<td>
<b>Simple binary data</b>
</td>
<td>
<b>BINARY</b>
</td>
<td>
<b>Parse binary payload with device serial number, battery level, temperature and saturation</b>
</td>
<td>
<span tb-help-popup="converter/examples/decoder/simple-binary/payload" tb-help-popup-placement="top" trigger-style="font-size: 16px; line-height: 75px;" trigger-text="payload"></span>
</td>
<td>
<span tb-help-popup="converter/examples/decoder/simple-binary/decoder_fn" tb-help-popup-placement="top" trigger-style="font-size: 16px; line-height: 75px;" trigger-text="Decoder function"></span>
</td>
<td>
<span tb-help-popup="converter/examples/decoder/simple-binary/output" tb-help-popup-placement="top" trigger-style="font-size: 16px; line-height: 75px;" trigger-text="Decoder output"></span>
</td>
</tr>
<tr>
<td>
<b>JSON with multiple hex encoded values</b>
</td>
<td>
<b>JSON</b>
</td>
<td>
<b>Convert multiple JSON objects with hex “value” fields and timestamp</b>
</td>
<td>
<span tb-help-popup="converter/examples/decoder/complex-json-hex/payload" tb-help-popup-placement="top" trigger-style="font-size: 16px;" trigger-text="payload"></span><br><br>
</td>
<td>
<span tb-help-popup="converter/examples/decoder/complex-json-hex/decoder_fn" tb-help-popup-placement="top" trigger-style="font-size: 16px; line-height: 75px;" trigger-text="Decoder function"></span>
</td>
<td>
<span tb-help-popup="converter/examples/decoder/complex-json-hex/output" tb-help-popup-placement="top" trigger-style="font-size: 16px; line-height: 75px;" trigger-text="Decoder output"></span>
</td>
</tr>
<tr>
<td>
<b>Use metadata fields</b>
</td>
<td>
<b>JSON</b>
</td>
<td>
<b>Use metadata fields to determine device type, model and customer name</b>
</td>
<td>
<span tb-help-popup="converter/examples/decoder/simple-metadata/payload" tb-help-popup-placement="top" trigger-style="font-size: 16px;" trigger-text="payload"></span><br><br>
<span tb-help-popup="converter/examples/decoder/simple-metadata/metadata" tb-help-popup-placement="top" trigger-style="font-size: 16px;" trigger-text="metadata" [tb-help-popup-style]="{maxWidth: '600px'}"></span>
</td>
<td>
<span tb-help-popup="converter/examples/decoder/simple-metadata/decoder_fn" tb-help-popup-placement="top" trigger-style="font-size: 16px; line-height: 75px;" trigger-text="Decoder function"></span>
</td>
<td>
<span tb-help-popup="converter/examples/decoder/simple-metadata/output" tb-help-popup-placement="top" trigger-style="font-size: 16px; line-height: 75px;" trigger-text="Decoder output"></span>
</td>
</tr>
</tbody>
</table>

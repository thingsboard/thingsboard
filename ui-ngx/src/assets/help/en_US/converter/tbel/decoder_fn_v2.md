#### Uplink data converter decoder function

<div class="divider"></div>
<br/>

*function payloadDecoder(payload, metadata): object | object[]*

[TBEL{:target="_blank"}](${siteBaseUrl}/docs${docPlatformPrefix}/user-guide/tbel/) function used to parse and transform uplink message from the integration to the common format used by the platform.

**Parameters:**

<ul>
  <li>
    <b>payload:</b> <code>any</code> - represents a byte array of encoded data received in the message from the integration. By default, the payload is encoded in Binary (Base64), but payload may also contain already decoded data in JSON format.
  </li>
  <li>
    <b>metadata:</b> <code>{[key: string]: object}</code> - represents key-value data from the integration message. Additional metadata can be configured for each integration in the integration details.
  </li>
</ul>

There are two types of outputs:

1. **Converter output:**  
   This output combines pre-configured settings with the result of the decoding function. The initial configuration defines default keys and values, but the decoding function can overwrite these keys with new values if necessary. This flexible approach ensures that both pre-configured settings and dynamic data are seamlessly integrated into the final JSON output.

2. **Decoder output:**
   This output is the direct result of the decoding function. It represents the data decoded from the incoming message without any additional configuration or processing. It must be a valid JSON object and meet the following requirements:

* Required **attributes** object:  
  The result must include an **attributes** object that holds details about the device/asset. It must contain at least one key-value pair; in other words, you need to provide at least one value within the attributes object to ensure it is not empty.

* Required **telemetry** object or array:  
  The result must include a **telemetry** object or array that shows time series data for the device/asset. It must contain at least one entry; in other words, you need to provide at least one data point within the telemetry object or array to ensure it is not empty.

* Optional **name** property (overridable):  
  The **name** uniquely identifies the device/asset within the tenant's scope. Often, unique identifiers such as the eui, MAC address, or other hardware-specific values are used as the device/asset name. The platform uses this property to locate an existing device/asset. If no match is found and the integration is allowed to create entities, a new device/asset will be created.

* Optional **type** property (overridable):  
  The **type** must be either **Asset** or **Device**, and it indicates the nature of the entity being processed. This field ensures correct classification within the platform's data model.

* Optional **profile** property (overridable):  
  This field defines the profile to be associated with the device/asset. If the **profile** property is not set in either the pre-configuration or within the decoding function, then the default value **default** will be applied automatically.

* Optional **customer** property (overridable):   
  The platform will use this property to automatically assign the device/asset to a customer, creating a new customer if one with the specified name does not exist. This assignment occurs only during the creation process of the device/asset by the current integration; if the device/asset already exists, the platform ignores this parameter.

* Optional **group** property (overridable):   
  The platform will use this property to automatically assign the device/asset to a specific entity group, creating a new group if one with the specified name does not exist. The group is created within the tenant's scope (by default) or under a customer (if the **customer** property is present). This assignment only happens during the initial creation by the current integration; if the device/asset already exists, the parameter is ignored.

* Optional **label** property (overridable):   
  This property provides non-unique, user-friendly labels for devices/assets, which can be displayed on dashboards. Like other properties, it is applied only during creation by the current integration.

<div class="divider"></div>

##### Converter output examples.

Simple Converter output JSON result that contains entity name, type, profile, few attributes and telemetry values.

<br>

<div style="padding-left: 64px;"
     tb-help-popup="converter/tbel/examples/decoder_v2/simple_converter_output"
     tb-help-popup-placement="top"
     [tb-help-popup-style]="{maxHeight: '50vh', maxWidth: '50vw'}"
     trigger-style="font-size: 16px;"
     trigger-text="Simple converter output">
</div>

<br>

Similar Converter output JSON result that also contains entity label, group, customer.

<br>

<div style="padding-left: 64px;"
     tb-help-popup="converter/tbel/examples/decoder_v2/extended_converter_output"
     tb-help-popup-placement="top"
     [tb-help-popup-style]="{maxHeight: '50vh', maxWidth: '50vw'}"
     trigger-style="font-size: 16px;"
     trigger-text="Converter output with entity label, group, customer">
</div>

<br>

##### Decoder output examples.

Simple Decoder output JSON that contains attributes and telemetry.

<br>

<div style="padding-left: 64px;"
     tb-help-popup="converter/tbel/examples/decoder_v2/simple_decoder_output"
     tb-help-popup-placement="top"
     [tb-help-popup-style]="{maxHeight: '50vh', maxWidth: '50vw'}"
     trigger-style="font-size: 16px;"
     trigger-text="Simple decoder output">
</div>

<br>

Similar Decoder output JSON result that also contains entity label, group, customer.

<br>

<div style="padding-left: 64px;"
     tb-help-popup="converter/tbel/examples/decoder_v2/extended_decoder_output"
     tb-help-popup-placement="top"
     [tb-help-popup-style]="{maxHeight: '50vh', maxWidth: '50vw'}"
     trigger-style="font-size: 16px;"
     trigger-text="Decoder output with entity label, group, customer">
</div>

<br>

<div class="divider"></div>

##### Examples

Refer to the table below for examples that include input parameters, decoder functions, and expected outputs.

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
<b>Decoder Output</b>
</th>
<th style="max-width: 200px; padding-left: 22px;">
<b>Converter Output</b>
</th>
</tr>
</thead>
<tbody>
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
<span tb-help-popup="converter/tbel/examples/decoder_v2/simple-binary/payload" tb-help-popup-placement="top" trigger-style="font-size: 16px; line-height: 75px;" trigger-text="payload"></span>
<span tb-help-popup="converter/tbel/examples/decoder_v2/simple-metadata/metadata" tb-help-popup-placement="top" trigger-style="font-size: 16px;" trigger-text="metadata" [tb-help-popup-style]="{maxWidth: '600px'}"></span>
</td>
<td>
<span tb-help-popup="converter/tbel/examples/decoder_v2/simple-binary/decoder_fn" tb-help-popup-placement="top" trigger-style="font-size: 16px; line-height: 75px;" trigger-text="Decoder function"></span>
</td>
<td>
<span tb-help-popup="converter/tbel/examples/decoder_v2/simple-binary/decoder_output" tb-help-popup-placement="top" trigger-style="font-size: 16px; line-height: 75px;" trigger-text="Decoder output"></span>
</td>
<td>
<span tb-help-popup="converter/tbel/examples/decoder_v2/simple-binary/converter_output" tb-help-popup-placement="top" trigger-style="font-size: 16px; line-height: 75px;" trigger-text="Converter output"></span>
</td>
</tr>
<tr>
<td>
<b>Simple JSON data</b>
</td>
<td>
<b>JSON</b>
</td>
<td>
<b>Parse specific JSON with device serial number, battery level, temperature and saturation</b>
</td>
<td>
<span tb-help-popup="converter/tbel/examples/decoder_v2/simple-json/payload" tb-help-popup-placement="top" trigger-style="font-size: 16px; line-height: 75px;" trigger-text="payload"></span>
<span tb-help-popup="converter/tbel/examples/decoder_v2/simple-metadata/metadata" tb-help-popup-placement="top" trigger-style="font-size: 16px;" trigger-text="metadata" [tb-help-popup-style]="{maxWidth: '600px'}"></span>
</td>
<td>
<span tb-help-popup="converter/tbel/examples/decoder_v2/simple-json/decoder_fn" tb-help-popup-placement="top" trigger-style="font-size: 16px; line-height: 75px;" trigger-text="Decoder function"></span>
</td>
<td>
<span tb-help-popup="converter/tbel/examples/decoder_v2/simple-json/decoder_output" tb-help-popup-placement="top" trigger-style="font-size: 16px; line-height: 75px;" trigger-text="Decoder output"></span>
</td>
<td>
<span tb-help-popup="converter/tbel/examples/decoder_v2/simple-json/converter_output" tb-help-popup-placement="top" trigger-style="font-size: 16px; line-height: 75px;" trigger-text="Converter output"></span>
</td>
</tr>
</tbody>
</table>

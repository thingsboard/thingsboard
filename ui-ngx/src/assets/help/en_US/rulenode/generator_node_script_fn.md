#### Message generator function

<div class="divider"></div>
<br/>

*function Generate(prevMsg, prevMetadata, prevMsgType): {msg: object, metadata: object, msgType: string}*

JavaScript function generating new Message using previous Message payload, Metadata and Message type as input arguments.

**Parameters:**

<ul>
  <li><b>prevMsg:</b> <code>{[key: string]: any}</code> - is a previously generated Message payload key/value object.
  </li>
  <li><b>prevMetadata:</b> <code>{[key: string]: string}</code> - is a previously generated Message metadata key/value object.
  </li>
  <li><b>prevMsgType:</b> <code>string</code> - is a previously generated string Message type. See <a href="https://github.com/thingsboard/thingsboard/blob/ea039008b148453dfa166cf92bc40b26e487e660/ui-ngx/src/app/shared/models/rule-node.models.ts#L338" target="_blank">MessageType</a> enum for common used values.
  </li>
</ul>

**Returns:**

Should return the object with the following structure:

```javascript
{ 
   msg: {[key: string]: any},
   metadata: {[key: string]: string},
   msgType: string
}
```

All fields in resulting object are mandatory.

<div class="divider"></div>

##### Examples

* Generate message of type `POST_TELEMETRY_REQUEST` with random `temperature` value from `18` to `32`:

```javascript
var temperature = 18 + Math.random() * (32 - 18);
// Round to at most 2 decimal places (optional)
temperature = Math.round( temperature * 100 ) / 100;
var msg = { temperature: temperature };
return { msg: msg, metadata: {}, msgType: "POST_TELEMETRY_REQUEST" };
{:copy-code}
```


<ul>
<li>
Generate message of type <code>POST_TELEMETRY_REQUEST</code> with <code>temp</code> value <code>42</code>,
<code>humidity</code> value <code>77</code><br>
and <strong>metadata</strong> with field <code>data</code> having value <code>40</code>:
</li>
</ul>

```javascript
var msg = { temp: 42, humidity: 77 };
var metadata = { data: 40 };
return { msg: msg, metadata: metadata, msgType: "POST_TELEMETRY_REQUEST" };
{:copy-code}
```

<ul>
<li>
Generate message of type <code>POST_TELEMETRY_REQUEST</code> with <code>temperature</code> value<br>
increasing and decreasing linearly in the range from <code>18</code> to <code>32</code>:
</li>
</ul>

```javascript
var lower = 18;
var upper = 32;
var isDecrement = 'false';
var temperature = lower;

// Get previous values

if (typeof prevMetadata !== 'undefined' &&
  typeof prevMetadata.isDecrement !== 'undefined') {
  isDecrement = prevMetadata.isDecrement;
}
if (typeof prevMsg !== 'undefined' &&
  typeof prevMsg.temperature !== 'undefined') {
  temperature = prevMsg.temperature;
}

if (isDecrement === 'true') {
  temperature--;
  if (temperature <= lower) {
    isDecrement = 'false';
    temperature = lower;
  }
} else {
  temperature++;
  if (temperature >= upper) {
    isDecrement = 'true';
    temperature = upper;
  }
}

var msg = { temperature: temperature };
var metadata = { isDecrement: isDecrement };

return { msg: msg, metadata: metadata, msgType: "POST_TELEMETRY_REQUEST" };
{:copy-code}
```

<br>
<br>

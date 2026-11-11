#### Encoder JSON output example

```json
{
  "contentType": "JSON",
  "data": "{\"tempFreq\":60,\"firmwareVersion\":\"1.2.3\"}",
  "metadata": {
    "topic": "temp-sensor/sensorA/upload"
  }
}
```

**Where:**

<ul>
  <li><b>contentType:</b> <code>string</code> - JSON, TEXT or BINARY (Base64 string) and is specific to your Integration type.
  </li>
  <li><b>data:</b> <code>string</code> - data string according to the content type.
  </li>
  <li><b>metadata:</b> <code>{[key: string]: string}</code> - list of key-value pairs with additional data about the message.<br>
       For example, topic to use for MQTT integration, etc.
  </li>
</ul>

<br>
<br>

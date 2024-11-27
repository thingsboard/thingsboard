### Expressions
#### JSON Path:

The expression field is used to extract data from the MQTT message. There are various available options for different parts of the messages:

 - The JSONPath format can be used to extract data from the message body.

 - The regular expression format can be used to extract data from the topic where the message will arrive.

 - Slices can only be used in the expression fields of bytes converters.

JSONPath expressions specify the items within a JSON structure (which could be an object, array, or nested combination of both) that you want to access. These expressions can select elements from JSON data on specific criteria. Here's a basic overview of how JSONPath expressions are structured:

- `$`: The root element of the JSON document;

- `.`: Child operator used to select child elements. For example, $.store.book ;

- `[]`: Child operator used to select child elements. $['store']['book'] accesses the book array within a store object;

##### Examples:

For example, if we want to extract the device name from the following message, we can use the expression below:

MQTT message:

```
{
  "sensorModelInfo": {
    "sensorName": "AM-123",
    "sensorType": "myDeviceType"
  },
  "data": {
    "temp": 12.2,
    "hum": 56,
    "status": "ok"
  }
}
{:copy-code}
```

Expression:

`${sensorModelInfo.sensorName}`

Converted data:

`AM-123`

If we want to extract all data from the message above, we can use the following expression:

`${data}`

Converted data:

`{"temp": 12.2, "hum": 56, "status": "ok"}`

Or if we want to extract specific data (for example “temperature”), you can use the following expression:

`${data.temp}`

And as a converted data we will get:

`12.2`

<br/>

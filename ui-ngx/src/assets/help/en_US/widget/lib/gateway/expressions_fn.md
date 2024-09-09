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

#### Regular expressions for topic:

Device name or device profile can be parsed from the MQTT topic using regular expression. A regular expression, often abbreviated as regex or regexp, is a sequence of characters that forms a search pattern, primarily used for string matching and manipulation.

##### Regular expression for topic examples:

| Topic                      | Regular expression               | Output data              | Description                          |
|:---------------------------|----------------------------------|--------------------------|--------------------------------------|
| /devices/AM123/mytype/data |  /devices/([^/]+)/mytype/data    |  AM123                   | Getting device name from topic       |
| /devices/AM123/mytype/data |  /devices/[A-Z0-9]+/([^/]+)/data |  mytype                  | Getting device profile from topic    |

<br/>

#### Bytes converter:

For bytes converter, expression fields can use slices format only. A slice specifies how to slice a sequence, determining the start point, and the endpoint. Here's a basic overview of slice components:

- `start`: The starting index of the slice. It is included in the slice. If omitted, slicing starts at the beginning of the sequence. Indexing starts at 0, so the first element of the sequence is at index 0.

- `stop`: The ending index of the slice. It is excluded from the slice, meaning the slice will end just before this index. If omitted, slicing goes through the end of the sequence.

##### Bytes parsing examples:


| Message body           |  Slice          | Output data              | Description                  |
|:-----------------------|-----------------|--------------------------|------------------------------|
|   AM123,mytype,12.2,45 |  [:5]           |  AM123                   | Extracting device name       |
|   AM123,mytype,12.2,45 |  [:]            |  AM123,mytype,12.2,45    | Extracting all data          |
|   AM123,mytype,12.2,45 |  [18:]          |  45                      | Extracting humidity value    |
|   AM123,mytype,12.2,45 |  [13:17]        |  12.2                    | Extracting temperature value |

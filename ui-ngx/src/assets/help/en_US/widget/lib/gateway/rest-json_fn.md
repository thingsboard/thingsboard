### Expressions
#### JSON Path

The expression field is used to specify the path for extracting data from an HTTP response message.

JSONPath expressions specify the items within a JSON structure (which could be an object, array, or nested combination of both) that you want to access. These expressions can select elements from JSON data on specific criteria. Here's a basic overview of how JSONPath expressions are structured:

- `$`: The root element of the JSON document;
- `.`: Child operator used to select child elements. For example, $.store.book ;
- `[]`: Child operator used to select child elements. $['store']['book'] accesses the book array within a store object;

#### Examples

For example, if we want to extract the device name from the following message, we can use the expression below:

HTTP response message:

```json
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
```

JSON Path Expression

`${sensorModelInfo.sensorName}`

Converted data:

`AM-123`

To extract all data from the message above, use:

`${data}`

Converted data:

`{"temp": 12.2, "hum": 56, "status": "ok"}`

To extract a specific value, such as the temperature, use:

`${data.temp}`

Converted data:

`12.2`

### Expressions
#### Request URL

JSONPath expressions are used to construct URL addresses for sending messages.

JSONPath expressions specify items within a JSON structure (objects, arrays, or a nested combination) that you wish to access. These expressions can select elements from JSON data on specific criteria. Here's a basic overview of how JSONPath expressions are structured:

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

Url Expression:

`${sensorModelInfo.sensorName}`

Converted data:

`AM-123`

To extract all data from the message above, use:

`${data}`

Converted data:

`{"temp": 12.2, "hum": 56, "status": "ok"}`

To extract specific data (e.g., "temperature"), use:

`${data.temp}`

Converted data:

`12.2`

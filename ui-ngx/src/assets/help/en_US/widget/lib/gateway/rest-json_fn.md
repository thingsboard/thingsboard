### Expressions
#### JSON Path

The expression field is used to specify the path for extracting data from an incoming HTTP request body.

JSONPath expressions specifies the items within a JSON structure (which could be an object, array, or nested combination of both) that you want to access.  
These expressions can select elements from JSON data on specific criteria.  
Basic overview of how JSONPath expressions are structured:

- `$`: The root element of the JSON document;
- `.`: Child operator used to select child elements. For example, $.store.book ;
- `[]`: Child operator used to select child elements. $['store']['book'] accesses the book array within a store object;

#### Examples

Incoming request body:

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
	},
  "rxInfo": [
    {
      "rssi": 50,
      "snr": 3
    },
    {
      "rssi": 22,
      "snr": 1
    }
  ]
}
```
<br>

Examples below shows on how to extract values from incoming request body.

| JSON Path Expression            | Extracted data                              |
|---------------------------------|---------------------------------------------|
| `${sensorModelInfo.sensorName}` | `AM-123`                                    |
| `${data}`                       | `{"temp": 12.2, "hum": 56, "status": "ok"}` |
| `${$.data.temp}`                | `12.2`                                      |
| `${$.rxInfo[0].rssi}`           | `50`                                        |

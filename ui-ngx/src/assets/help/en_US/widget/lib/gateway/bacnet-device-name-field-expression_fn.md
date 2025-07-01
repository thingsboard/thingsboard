## Expressions

This field allows dynamically constructing a formatted device name using values extracted from a JSON object. You can specify variables to access the relevant fields in the JSON.

# Available Variables

You can use the following variables to extract specific device information:

* `objectName`: Extracts the device's object name (e.g., `"Main Controller"`).
* `vendorId`: Extracts the device's vendor ID, typically a numeric identifier representing the manufacturer (e.g., `"1234"`).
* `objectId`: Extracts the device's unique object identifier (e.g., `"999"`).
* `address`: Extracts the device's network address (e.g., `"192.168.1.1"`).

# Examples

* `"Device ${objectName}"`
  If the objectName variable exist and contains `"objectName": "Main Controller"`, the device on platform will have the following name: `Device Main Controller`.
* `"Vendor: ${vendorId}"`
  If the vendorId variable exist and contains `"vendorId": 1234`, the device on platform will have the following name: `Vendor: 1234`.
* `"Device ID: ${objectId} at ${address}"`
  If the objectId variable exist and contains `"vendorId": 999 `and address variable exist and contains `"address": "192.168.1.1"` , the device on platform will have the following name: `Device ID: 999 at 192.168.1.1`.

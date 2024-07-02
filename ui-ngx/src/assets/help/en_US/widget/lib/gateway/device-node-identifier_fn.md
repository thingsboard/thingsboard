## Device Node Field

### Identifier:

Device Node field value is used to identify the node for the current device.

An Identifier type is a unique ID assigned to a node within the OPC-UA server. It is used to directly reference specific nodes without navigating through the namespace hierarchy.
The Identifier type in the OPC-UA connector configuration can be used in various forms to uniquely reference nodes in the OPC-UA server's address space. Identifiers can be of different types, such as numeric (`i`), string (`s`), byte string (`b`), and GUID (`g`). Below is an explanation of each identifier type with examples.

- ##### Numeric Identifier (`i`)
A numeric identifier uses an integer value to uniquely reference a node in the OPC-UA server.
###### Example:
`ns=2;i=1234`
In this example, `ns=2` specifies the namespace index `2`, and `i=1234` specifies the numeric identifier `1234` for the node.

- ##### String Identifier (`s`)
A string identifier uses a string value to uniquely reference a node in the OPC-UA server.
###### Example:
`ns=3;s=TemperatureSensor`
Here, `ns=3` specifies the namespace index `3`, and `s=TemperatureSensor` specifies the string identifier for the node.

- ##### Byte String Identifier (`b`)
An byte string identifier uses a byte string to uniquely reference a node in the OPC-UA server. This is useful for binary data that can be converted to a byte string.
###### Example:
`ns=4;b=Q2xpZW50RGF0YQ==`
In this example, `ns=4` specifies the namespace index `4`, and `b=Q2xpZW50RGF0YQ==` specifies the byte string identifier for the node (base64 encoded).

- ##### GUID Identifier (`g`)
A GUID identifier uses a globally unique identifier (GUID) to uniquely reference a node in the OPC-UA server.
###### Example:
`ns=1;g=550e8400-e29b-41d4-a716-446655440000`
Here, `ns=1` specifies the namespace index `1`, and `g=550e8400-e29b-41d4-a716-446655440000` specifies the GUID for the node.

By using these different identifier types, you can accurately and uniquely reference nodes in the OPC-UA server's address space, regardless of the format of the node identifiers.

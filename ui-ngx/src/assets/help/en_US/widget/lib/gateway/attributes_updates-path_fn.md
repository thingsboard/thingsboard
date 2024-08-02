A **Path** type refers to the hierarchical address within the OPC-UA server's namespace. It is used to navigate to 
specific nodes in the server.

The path for device name can be absolute or relative.

### Absolute Path
An **absolute path** specifies the full hierarchical address from the root of the OPC-UA server's namespace to 
the target node.

###### Example:
Gateway expects that the node exist and the value of **Root\\.Objects\\.TempSensor\\.Version** is “**1.0.3**”.

_Expression:_

**`Root\.Objects\.TempSensor\.Version`**

In this example, the attribute update request will write the received data to the configured node above.

### Relative Path
A **relative path** specifies the address relative to a predefined starting point in the OPC-UA server's namespace.

###### Example:
Gateway expects that the node exist and the value of **Root\\.Objects\\.TempSensor\\.Name** is “**TH-101**”.

_Device Node expression:_

**`Root\.Objects\.TempSensor`**

_Expression:_

**`.Version`**

In this example, **the gateway will search for the child node "Name" in the device node (parent node) 
"Root\\.Objects\\.TempSensor"** and will write received data on it.

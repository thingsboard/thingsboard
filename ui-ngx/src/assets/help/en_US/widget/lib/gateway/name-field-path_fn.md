## Device Name Field

Device name field is used for looking the device name in some variable.

A **Path** type refers to the hierarchical address within the OPC-UA server's namespace. It is used to navigate to 
specific nodes in the server.

The path for device name can be absolute or relative.

### Absolute Path
An **absolute path** specifies the full hierarchical address from the root of the OPC-UA server's namespace to the 
target node.

##### **Example:**

Gateway expects that the node exist and the value of **Root\\.Objects\\.TempSensor\\.Name** is “**TH-101**”.

_Expression:_

`**Device ${Root\.Objects\.TempSensor\.Name}**`

In this example, created device on platform will have “**Device TH-101**” name.

### Relative Path
A relative path specifies the address relative to a predefined starting point in the OPC-UA server's namespace.

##### **Example:**

Gateway expects that the node exist and the value of **Root\\.Objects\\.TempSensor\\.Name** is “**TH-101**”.

_Device Node expression:_

`**Root\.Objects\.TempSensor**`

_Expression:_

`**Device ${Name}**`

In this example, **the gateway will search for the child node "Name" in the device node (parent node) 
"Root\\.Objects\\.TempSensor"** and created device on the platform will be named "**Device TH-101**".


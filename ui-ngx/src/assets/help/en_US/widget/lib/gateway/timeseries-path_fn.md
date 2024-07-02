A **Path** type refers to the hierarchical address within the OPC-UA server's namespace. It is used to navigate to 
specific nodes in the server.

The path for the attribute value can be absolute or relative.

### Absolute Path
An **absolute path** specifies the full hierarchical address from the root of the OPC-UA server's namespace to 
the target node.

###### Example:
Gateway expects that the node exist and the value of **Root\\.Objects\\.TempSensor\\.Temperature** is **23.54**.

_Expression:_

**`${Root\.Objects\.TempSensor\.Temperature}`**

_Converted data:_

**`23.54`**

### Relative Path
A **relative path** specifies the address relative to a predefined starting point in the OPC-UA server's namespace.

###### Example:
Gateway expects that the node exist and the value of “**Root\\.Objects\\.TempSensor\\.Temperature**” is **23.56**.

_Device Node expression:_

**`Root\.Objects\.TempSensor`**

_Expression:_

**`${Temperature}`**

_Converted data:_

**`23.56`**

In this example, **the gateway will search for the child node "Temperature" in the device node (parent node) 
"Root\\.Objects\\.TempSensor"** and will send converted data to the device.

Additionally, you can use the node browser name to ensure that your path cannot be altered by the user or server.

###### Example:
**`Root\.0:Objects\.3:Simulation`**

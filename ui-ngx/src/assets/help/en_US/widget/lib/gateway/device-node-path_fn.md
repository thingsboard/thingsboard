## Device Node Field
### Path:

Device Node field value is used to identify the node for the current device. The connector will use this node as the parent node of the device.

A Path type refers to the hierarchical address within the OPC-UA server's namespace. It is used to navigate to specific nodes in the server.

The path for Device node can be only absolute.

An absolute path specifies the full hierarchical address from the root of the OPC-UA server's namespace to the target node.

###### Examples:

- `Root\.Objects\.TempSensor`

In this example, the `Value` specifies the full path to the `TempSensor` node located in the `Objects` namespace, starting from the root.

Additionally, you can use the node browser name to ensure that your path cannot be altered by the user or server.

- `Root\.0:Objects\.3:Simulation`

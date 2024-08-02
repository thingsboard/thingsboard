## Device Name Field

Device name field is used for looking the device name in some variable.

An **Identifier** type is a unique ID assigned to a node within the OPC-UA server. It is used to directly reference 
specific nodes without navigating through the namespace hierarchy.

**Identifier** type in the OPC-UA connector configuration can be used in various forms to uniquely reference nodes in 
the OPC-UA server's address space. Identifiers can be of different types, such as numeric (`**i**`), string (`**s**`), 
byte string (`**b**`), and GUID (`**g**`). Below is an explanation of each identifier type with examples.

- ##### **Numeric Identifier (`i`)**
    
  A **numeric identifier** uses an integer value to uniquely reference a node in the OPC-UA server.

  ###### **Example:**

  Gateway expects that the node exist and the value of **ns=2;i=1234** node is “**TH-101**”.

  _Expression:_

  **`Device ${ns=2;i=1234}`**
  
  In this example, created device on platform will have “**Device TH-101**” name.

- ##### **String Identifier (`s`)**

  A **string identifier** uses a string value to uniquely reference a node in the OPC-UA server.

  ###### **Example:**

  Gateway expects that the node exist and the value of **ns=3;s=TemperatureSensor** node is “**TH-101**”.

  _Expression:_

  **`Device ${ns=3;s=TemperatureSensor}`**

  In this example, created device on platform will have “**Device TH-101**” name.

- ##### **Byte String Identifier (`b`)**

  A **byte string identifier** uses a byte string to uniquely reference a node in the OPC-UA server. 
  This is useful for binary data that can be converted to a byte string.

  ###### **Example:**

  Gateway expects that the node exist and the value of **ns=4;b=Q2xpZW50RGF0YQ==** node is “**TH-101**”.

  _Expression:_

  **`Device ${ns=4;b=Q2xpZW50RGF0YQ==}`**

  In this example, created device on platform will have “**Device TH-101**” name.

- ##### GUID Identifier (**`g`**)

  A **GUID identifier** uses a globally unique identifier (GUID) to uniquely reference a node in the OPC-UA server.

  ###### **Example:**

  Gateway expects that the node exist and the value of **ns=1;g=550e8400-e29b-41d4-a716-446655440000** node is “**TH-101**”.

  _Expression:_

  **`Device ${ns=1;g=550e8400-e29b-41d4-a716-446655440000}`**

  In this example, created device on platform will have “**Device TH-101**” name.

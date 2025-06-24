### **Request expression field**

The expression that is used to know if the request from the device is “**Attribute Request**” or not.

### **Examples of data converting**

Let’s review example of data converting:

We have a device that measures temperature and humidity. And for example, you want to send a request for a shared attribute that stores the firmware version of the device. In order for the connector to understand that the received message refers to "**Attribute request**" and not telemetry type, we must specify in the configuration what the message should begin with.

In our case, let the beginning of the message contain “**myShrAttrRequest**”.

Accordingly, the field will contain the following value:

`${[0:16]==myShrAttrRequest}`

And if the device sends a message with the following payload:

`myShrAttrRequestFirmwareVersion`

The connector will take the specified range from 0 to 16 bytes and see that this message belongs to the “**Attribute request**” type.

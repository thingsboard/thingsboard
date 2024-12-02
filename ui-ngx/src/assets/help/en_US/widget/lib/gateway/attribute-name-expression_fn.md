### **Attribute name expression field**

The expression that is used to get the name of the requested attribute from the received data.

### **Examples of data converting**

Let’s review example of data converting:

We have a device that measures temperature and humidity. And for example, you want to send a request for a shared attribute that stores the firmware version of the device. To do this, we need to specify exactly where in the message the attribute name is located, the value of which we want to get.

In our case, let the requested attribute name is “**FirmwareVersion**”.

Accordingly, the field will contain the following value:

`[16:]`

And if the device sends a message with the following payload:

`myShrAttrRequestFirmwareVersion`

The connector, according to the configuration above, will take bytes starting from **16** to the **end** (from **0** to **16** is the “**Request expression**”) and send a request to the platform for the dispersed name of the shared attribute.

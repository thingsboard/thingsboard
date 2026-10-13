## Byte field

The byte field is used to slice received data from the specific index.

### Examples of data converting

Let’s review more examples of data converting:
We have a device that measures temperature and humidity. Device has charasteristic that can be
read and when we receive data from her, the data combine temperature and humidity. So, data
from device have the next view:b’\x08<\x08\x00’ and in human readable format:[8, 34](first array
element is temperature and the second is humidity).

1. We want to read only temperature value

   **“Bytes from”: “0”**

   **“Bytes to”: “1”**

   Data to platform: **8**

2. We want to read only humidity value

   **“Bytes from”: “1”**

   **“Bytes to”: “-1”**

   Data to platform: **34**

3. We want to read all values

   **“Bytes from”: “0”**

   **“Bytes to”: “-1”**

   Data to platform:**834**

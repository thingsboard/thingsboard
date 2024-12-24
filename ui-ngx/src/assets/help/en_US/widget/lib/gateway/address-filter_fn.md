### Address filter field

It is used to filter the allowed IP addresses to connect to the connector.

### **Examples of IP addresses filtering**

Let’s review more examples of IP addresses filtering:

For example, we have a device that has the following IP address: 192.168.0.120:5001. Now let's look at examples of the configuration of the field to allow the connection of different variants of IP addresses:

1. Only one device with a specified IP address and port can connect:

   **Address filter**: 192.168.0.120:5001
2. Allow any devices with any IP address and only port 5001:

   **Address filter:** *:5001

3. Allow all devices that have the IP address 192.168.0.120 with any port:

   **Address filter:** 192.168.0.120:*

4. Allow any devices:

   **Address filter:** `*:*`

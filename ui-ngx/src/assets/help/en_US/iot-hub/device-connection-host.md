##### Which host should I use?

The device connects to the ThingsBoard **MQTT transport**, so enter the address at which the device can reach it.

* **Not `localhost` or `127.0.0.1`.** On the device, these mean the device itself.
* **ThingsBoard on your computer.** Use the computer's IP address in the local network, for example `192.168.1.10`. The device and the computer must be in the same network.
* **ThingsBoard on a server.** Use the server's hostname or public IP address. With a separate MQTT host or load balancer, use that address instead of the web UI address.
* **Enter only the host,** without `http://`, a port or a path.

<b>Port.</b> MQTT usually listens on `1883`. If the transport uses another port, or the port is forwarded (Docker, a load balancer), enter the port the device connects to.

To set the host and port for all users, ask the system administrator to fill them in **Settings → General → Device connectivity**.

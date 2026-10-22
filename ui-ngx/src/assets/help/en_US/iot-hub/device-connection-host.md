##### Which host should I use?

The device connects to ThingsBoard over MQTT. Enter an address the device can reach from its own network:

* **ThingsBoard on your computer, device on the same Wi-Fi:** your computer's IP in that network, e.g. `192.168.1.10`. Allow the MQTT port (`1883` by default) in its firewall.
* **ThingsBoard reachable from the internet** (it has a public IP or a public domain): use that address, e.g. `tb.mycompany.com` or `203.0.113.15`. The device needs internet access through its Wi-Fi.
* **Device on a local network, ThingsBoard elsewhere:** the server's local IP isn't reachable from there. Use its public domain or IP.
* **MQTT on a separate host:** enter that host. It can differ from the web UI address.

Don't use `localhost` or `127.0.0.1`: on the device they mean the device itself.

**Port.** MQTT usually listens on port `1883`. If the transport uses another port, or the port is forwarded (Docker, load balancer), enter the port the device connects to.

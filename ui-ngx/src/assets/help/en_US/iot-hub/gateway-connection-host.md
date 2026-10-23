##### Which host should I use?

The gateway runs in Docker and connects to ThingsBoard over MQTT. Enter an address that the machine running the gateway can reach:

* **Gateway on a separate machine in the same network** (e.g. a reComputer next to the equipment): the IP of the ThingsBoard server in that network, e.g. `192.168.1.10`. Allow the MQTT port (`1883` by default) in its firewall.
* **ThingsBoard reachable from the internet** (it has a public IP or a public domain): use that address, e.g. `tb.mycompany.com` or `203.0.113.15`.
* **Gateway on the same machine as ThingsBoard:** `host.docker.internal`, or the machine's IP in its network.
* **MQTT on a separate host:** enter that host. It can differ from the address you open ThingsBoard at in the browser.

Don't use `localhost` or `127.0.0.1`: depending on the Docker network mode, they mean the gateway container itself or the machine it runs on, not the ThingsBoard server. When the gateway runs on the same machine as ThingsBoard, use `host.docker.internal`.

**Port.** MQTT usually listens on port `1883`. If the transport uses another port, or the port is forwarded (Docker, load balancer), enter the port the gateway connects to.

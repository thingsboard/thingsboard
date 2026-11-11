### 📡 Edge computing

**Optionally**, this solution can be extended to use edge computing.

<a href="https://thingsboard.io/products/thingsboard-edge/" target="_blank">ThingsBoard Edge</a> allows bringing data analysis and management to the edge, where the data created.
At the same time ThingsBoard Edge seamlessly synchronizing with the ThingsBoard cloud according to your business needs.

As example, in the context of Smart Office solution, edge computing could be useful if you have remote offices that are scattered throughout the country. 
In this case, ThingsBoard Edge can be deployed into every office to process data from sensors, cameras, and other devices, enabling real-time analysis and decision-making, such as turning off lights or adjusting temperature automatically. 
Edge is going to process data in case there is no network connection to the central ThingsBoard server, and thus no data will be lost and required decisions are going to be taken locally. 
Eventually, required data is going to be pushed to the cloud, once network connection is established. 
Configuration of edge computing business logic is centralized in a single place - ThingsBoard server.

In the scope of this solution, new edge entity <a href="${Remote Office R1EDGE_DETAILS_URL}" target="_blank">Remote Office R1</a> was created.

Additionally, particular entity groups were already assigned to the edge entity to simplify the edge deployment:

* **"Buildings"** *ASSET* group;
* **"Office sensors"** *DEVICE* group;
* **"Smart office dashboards"** *DASHBOARD* group.

To install ThingsBoard Edge and connect to the cloud, please navigate to <a href="${Remote Office R1EDGE_DETAILS_URL}" target="_blank">edge details page</a> and click **Install & Connect instructions** button.

Once the edge is installed and connected to the cloud, you will be able to log in into edge using your tenant credentials.

#### 🔄 Push data to device on edge

**"Office sensors"** *DEVICE* group was assigned to the edge entity "Remote Office R1".
This means that all devices from this group will be automatically provisioned to the edge.

You can see devices from this group once you log in into edge and navigate to the **Entities -> Devices** page.

To emulate the data upload on behalf of device "Energy meter" to the edge, one should execute the following command:

```bash
curl -v -X POST -d "{\"voltage\":  220, \"frequency\":  60, \"amperage\": 16, \"power\": 3000, \"energy\": 300}" http://localhost:8080/api/v1/${Energy meterACCESS_TOKEN}/telemetry --header "Content-Type:application/json"{:copy-code}
```

<br>

Or please use next command if you updated edge HTTP 8080 bind port to **18080** during edge installation:

```bash
curl -v -X POST -d "{\"voltage\":  220, \"frequency\":  60, \"amperage\": 16, \"power\": 3000, \"energy\": 300}" http://localhost:18080/api/v1/${Energy meterACCESS_TOKEN}/telemetry --header "Content-Type:application/json"{:copy-code}
```

<br>

Once you'll push data to the device "Energy meter" on edge, you'll be able to see telemetry update on the cloud for this device as well.

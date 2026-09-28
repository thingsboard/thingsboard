### 📡 Edge computing

**Optionally**, this solution can be extended to use edge computing.

<a href="https://thingsboard.io/products/thingsboard-edge/" target="_blank">ThingsBoard Edge</a> allows bringing data analysis and management to the edge, where the data created.
At the same time ThingsBoard Edge seamlessly synchronizes with the ThingsBoard cloud according to your business needs.

As example, in the context of Smart Irrigation solution, edge computing could be useful if you have farms that are located in different parts of country or worldwide.
In this case, ThingsBoard Edge can be deployed into every farm to process data from soil moisture sensors, enabling real-time analysis and decision-making, such as enable the irrigation in case humidity thresholds are violated.
Edge is going to process data in case there is no network connection to the central ThingsBoard server, and thus no data will be lost and required decisions are going to be taken locally.
Eventually, required data is going to be pushed to the cloud, once network connection is established.
Configuration of edge computing business logic is centralized in a single place - ThingsBoard server.

In the scope of this solution, new edge entity <a href="${Remote Farm R1EDGE_DETAILS_URL}" target="_blank">Remote Farm R1</a> was created.

Additionally, particular entities and entity groups were already assigned to the edge entity to simplify the edge deployment:

* **"SI Field 1"** asset;
* **"SI Water Meter 1"** device;
* **"SI Smart Valve 1"** device;
* **"SI Soil Moisture 1"** device;
* **"SI Soil Moisture 2"** device;
* **"Smart Irrigation"** *DASHBOARD* group.

To install ThingsBoard Edge and connect to the cloud, please navigate to <a href="${Remote Farm R1EDGE_DETAILS_URL}" target="_blank">edge details page</a> and click **Install & Connect instructions** button.

Once the edge is installed and connected to the cloud, you will be able to log in into edge using your tenant credentials.

#### 🔄 Push data to device on edge

Four **"SI \*\*\*"** devices were assigned to the edge entity "Remote Farm R1", thus these devices will be automatically provisioned to the edge.

You can see these devices once you log in into edge and navigate to the **Entities -> Devices** page.

To emulate the data upload on behalf of device "SI Soil Moisture 1" to the edge, one should execute the following command:

```bash
curl -v -X POST -d "{\"moisture\":  77}" http://localhost:8080/api/v1/${SI Soil Moisture 1ACCESS_TOKEN}/telemetry --header "Content-Type:application/json"{:copy-code}
```

<br>

Or please use next command if you updated edge HTTP 8080 bind port to **18080** during edge installation:

```bash
curl -v -X POST -d "{\"moisture\":  77}" http://localhost:18080/api/v1/${SI Soil Moisture 1ACCESS_TOKEN}/telemetry --header "Content-Type:application/json"{:copy-code}
```

<br>

Once you'll push data to the device "SI Soil Moisture 1" on edge, you'll be able to see telemetry update on the cloud for this device as well.

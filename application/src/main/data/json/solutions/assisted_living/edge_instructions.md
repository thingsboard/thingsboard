### 📡 Edge computing

**Optionally**, this solution can be extended to use edge computing.

<a href="https://thingsboard.io/products/thingsboard-edge/" target="_blank">ThingsBoard Edge</a> allows bringing data analysis and management to the edge, where the data created.
At the same time ThingsBoard Edge seamlessly synchronizing with the ThingsBoard cloud according to your business needs.

As example, in the context of  Assisted Living solution, edge computing could be useful if you have residences that are located in different parts of town, country or worldwide.
In this case, ThingsBoard Edge can be deployed into every residence to process data from resident sensors, enabling real-time analysis and decision-making, such as getting alarm notifications in case health status of resident become critical and calling for assistance.
Edge is going to process data in case there is no network connection to the central ThingsBoard server, and thus no data will be lost and required decisions are going to be taken locally.
Eventually, required data is going to be pushed to the cloud, once network connection is established.
Configuration of edge computing business logic is centralized in a single place - ThingsBoard server.

In the scope of this solution, new edge entity <a href="${Remote Residence R1EDGE_DETAILS_URL}" target="_blank">Remote Residence R1</a> was created.

Additionally, particular entities and entity groups were already assigned to the edge entity to simplify the edge deployment:

* **"Administrators"** *USER* group of customer "Assisted Living Company";
* **"Residents"** *USER* group of customer "Assisted Living Company";
* **"Floor 1"** asset;
* **"Floor 2"** asset;
* **"Root 101"** asset;
* **"Root 102"** asset;
* **"D00000010001"** device;
* **"D00000020002"** device;
* **"C00000015FE1"** device;
* **"C00000025FE2"** device;
* **"Assisted Living"** *DASHBOARD* group.

**NOTE**: only limited number of assets and devices were assigned to the edge due to the limitation of default edge license.

To install ThingsBoard Edge and connect to the cloud, please navigate to <a href="${Remote Residence R1EDGE_DETAILS_URL}" target="_blank">edge details page</a> and click **Install & Connect instructions** button.

Once the edge is installed and connected to the cloud, you will be able to log in into edge using your tenant credentials.

#### 🔄 Push data to device on edge

All the devices that were assigned to the edge entity "Remote Residence R1" are going to be automatically provisioned to the edge.

You can see these devices once you log in into edge and navigate to the **Entities -> Devices** page.

To emulate the data upload on behalf of resident **"William Harris"** and simulate pulse update to the edge, one should execute the following command:

```bash
curl -v -X POST -d "{\"serial\": \"C00000025FE2\", \"data\":{\"pulse\":55}}" http://localhost:8080/api/v1/${D00000020002ACCESS_TOKEN}/telemetry --header "Content-Type:application/json"{:copy-code}
```

Or please use next command if you updated edge HTTP 8080 bind port to **18080** during edge installation:

```bash
curl -v -X POST -d "{\"serial\": \"C00000025FE2\", \"data\":{\"pulse\":55}}" http://localhost:18080/api/v1/${D00000020002ACCESS_TOKEN}/telemetry --header "Content-Type:application/json"{:copy-code}
```

Once you'll push data on behalf of resident **"William Harris"** to edge, you'll be able to see alarm generated on dashboard and telemetry update on the cloud for this resident as well.

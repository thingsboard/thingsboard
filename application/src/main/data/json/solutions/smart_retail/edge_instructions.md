### 📡 Edge computing

**Optionally**, this solution can be extended to use edge computing.

<a href="https://thingsboard.io/products/thingsboard-edge/" target="_blank">ThingsBoard Edge</a> allows bringing data analysis and management to the edge, where the data created.
At the same time ThingsBoard Edge seamlessly synchronizing with the ThingsBoard cloud according to your business needs.

As example, in the context of Smart Retail solution, edge computing could be useful if you have supermarkets that are located in different parts of town, country or worldwide.
In this case, ThingsBoard Edge can be deployed into every supermarket to process data from sensors and cameras, enabling real-time analysis and decision-making, such as alarm generation in case temperature in the fridge is above threshold. 
Edge is going to process data in case there is no network connection to the central ThingsBoard server, and thus no data will be lost and required decisions are going to be taken locally. 
Eventually, required data is going to be pushed to the cloud, once network connection is established. 
Configuration of edge computing business logic is centralized in a single place - ThingsBoard server.

In the scope of this solution, new edge entity <a href="${Remote Supermarket R1EDGE_DETAILS_URL}" target="_blank">Remote Supermarket R1</a> was added to a customer "Retail Company B".

Additionally, particular entity groups were already assigned to the edge entity to simplify the edge deployment:

* **"Smart Retail Users"** *USER* group of customer "Retail Company B";
* **"Supermarkets"** *ASSET* group of customer "Retail Company B";
* **"Supermarket Devices"** *DEVICE* group of customer "Retail Company B";
* **"Supermarket Users Shared"** *DASHBOARD* group of your tenant.

To install ThingsBoard Edge and connect to the cloud, please navigate to <a href="${Remote Supermarket R1EDGE_DETAILS_URL}" target="_blank">edge details page</a> and click **Install & Connect instructions** button.

Once the edge is installed and connected to the cloud, you will be able to log in into edge using your tenant or users of customer "Retail Company B" credentials.

#### 🔄 Push data to device on edge

**"Supermarket Devices"** *DEVICE* group of customer "Retail Company B" was assigned to the edge entity "Remote Supermarket R1".
This means that all devices from this group will be automatically provisioned to the edge.

You can see devices from this group once you log in into edge and navigate to the **Entities -> Devices** page of customer "Retail Company B".

To emulate the data upload on behalf of device "Freezer 67478" (located inside supermarket "Supermarket S3") to the edge, one should execute the following command:

```bash
curl -v -X POST -d "{\"temperature\":  -5.4}" http://localhost:8080/api/v1/${Freezer 67478ACCESS_TOKEN}/telemetry --header "Content-Type:application/json"{:copy-code}
```

Or please use next command if you updated edge HTTP 8080 bind port to **18080** during edge installation:

```bash
curl -v -X POST -d "{\"temperature\":  -5.4}" http://localhost:18080/api/v1/${Freezer 67478ACCESS_TOKEN}/telemetry --header "Content-Type:application/json"{:copy-code}
```

Once you'll push data to the device "Freezer 67478" on edge, you'll be able to see telemetry update on the cloud for this device as well.

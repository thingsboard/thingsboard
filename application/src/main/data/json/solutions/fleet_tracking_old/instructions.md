## Solution instructions

Welcome to your new **Fleet Tracking** solution 👋 We have generated the <a href="${MAIN_DASHBOARD_URL}" target="_blank">"Fleet Tracking"</a> dashboard for you. Use it to:

* 📍 **Observe** real-time bus locations and status;
* 🔔 **Monitor** tracking events (alarms);
* 📈 **Browse** route history, speed, and fuel levels.

### 🖥 Mastering the dashboard

The dashboard has two states. The **Main** state displays the list of the buses, their location on the map as well as the list of their alarms.
* **Route History:** Click the "Route history" icon on any table row to see a popup with a road map where the bus has been.
* **Bus Details:** Click the table row itself to drill down into the details view to inspect alarms, speed, and fuel history.

You can always customize this dashboard using our <a href="${DOCS_BASE_URL}/user-guide/dashboards/" target="_blank">dashboard development guide</a>.

### 🔌 Devices

We have pre-provisioned four bus tracking devices with demo data. You can find their info and credentials below:

<div class="tb-markdown-view table-wrapper">

${device_list_and_credentials}

</div>

#### ⚡ Test it now

Want to see the dashboard come alive? You can simulate a real bus right now.
The solution expects the device to upload `latitude`, `longitude`, `speed`, `fuel`, and `status`.

The most simple example of the expected payload is in JSON format:

```json
{"latitude":  37.764702, "longitude":  -122.476071, "speed":  50, "fuel":  5, "status": "On route"}{:copy-code}
```

<br>

To emulate the data upload on behalf of device "Bus C", one should execute the following command:

```bash
curl -v -X POST -d "{\"latitude\":  37.764702, \"longitude\":  -122.476071, \"speed\":  50, \"fuel\":  5, \"status\": \"On route\"}" ${BASE_URL}/api/v1/${Bus CACCESS_TOKEN}/telemetry --header "Content-Type:application/json"{:copy-code}
```

<br>

The example above uses <a href="${DOCS_BASE_URL}/reference/http-api/getting-connected/#telemetry-upload-api" target="_blank">HTTP API</a>.
See <a href="${DOCS_BASE_URL}/user-guide/connectivity-guide/" target="_blank">connecting devices</a> for other connectivity options.

Go check your dashboard — you should see the values update instantly 🚀

### 🚨 Alarms

Your solution monitors data based on the <a href="${DOCS_BASE_URL}/user-guide/alarm-rules/" target="_blank">alarm rules</a> configured in the "bus" device profile:

<div class="tb-markdown-view table-wrapper">

${alarm_rules}

</div>

### 📦 Solution entities

The following entities were automatically created to power this solution:

<div class="tb-markdown-view table-wrapper">

${all_entities}

</div>

${edge_instructions}
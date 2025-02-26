#### Fields templatization

<div class="divider"></div>
<br/>

{% include rulenode/common_node_fields_templatization %}

##### Example

Let's assume that a tenant manages two assets:

 - `TemperatureManager` asset - responsible for aggregating data from temperature sensors, essential for environmental monitoring and alerts.
 - `HumidityManager` asset - collects data from humidity sensors, analyzing relative humidity levels
  and correlating them with temperature data for comprehensive atmospheric condition monitoring and automated environmental adjustments.

Each message received from the device includes `deviceType` property in the message metadata
with either `Temperature` or `Humidity` value according to the sensor type.

In order to change the originator to the corresponding manager for further message processing, you can define the next node configuration:

![image](${helpBaseUrl}/help/images/rulenode/examples/change-originator-ft.png)

Imagine that you receive the message defined below from the `Temperature` sensor
and forwarded it to the **change originator** node with configuration added above.

- incoming message definition:

```json
{
  "msg": {
    "temperature": 32
  },
  "metadata": {
    "deviceType": "Temperature",
    "deviceName": "TH-001",
    "ts": "1685379440000"
  }
}
```
<br>

The same example for the `humidity` sensor:

- incoming message definition:

```json
{
  "msg": {
    "humidity": 77
  },
  "metadata": {
    "deviceType": "Humidity",
    "deviceName": "HM-001",
    "ts": "1685379440000"
  }
}
```

<br>

To demonstrate that the originator was changed based on rule node configuration, the screenshots with debug events captured for the **change originator** rule node will be added below:

 - Debug events for processed message from the `Temperature` sensor:

![image](${helpBaseUrl}/help/images/rulenode/examples/change-originator-ft-2.png)

 - Debug events for processed message from the `Humidity` sensor:

![image](${helpBaseUrl}/help/images/rulenode/examples/change-originator-ft-3.png)

In the debug events displayed, the `IN` message points to the incoming message received by the node, 
with the originator type specified as `DEVICE`. This reflects the message from the actual sensor (e.g., a `Temperature` or `Humidity` sensor). 
After processing through the `change originator` rule node, the `OUT` message has an originator type of `ASSET`, 
indicating that the message originator has been successfully changed to the corresponding managing asset, such as `TemperatureManager` or `HumidityManager`.

<br>

These examples showcases using the **change originator** node with dynamic configuration based on the substitution of metadata fields.

<br>
<br>

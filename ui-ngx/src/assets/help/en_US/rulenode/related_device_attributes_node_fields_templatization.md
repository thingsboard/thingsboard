#### Fields templatization

<div class="divider"></div>
<br/>

{% include rulenode/common_node_fields_templatization %}

##### Examples

Let's assume that we have a moisture meter device(message originator) that publishes a telemetry messages that includes the following data readings:

- `soilMoisture`
- `windSpeed`
- `windDirection`
- `temperature`
- `humidity`

Depending on certain conditions, we might need to fetch additional server-side attributes from the related irrigation controller device.

Specifically, if the soil moisture reading drops below a certain threshold, let's say 30%, this is considered critical as it directly impacts crop health and growth.
In this case, we need to fetch the `lastIrrigationTime` attribute.
This additional information can help us understand when the field was last watered and take necessary action, such as activating the irrigation system.
However, if the soil moisture is above this critical threshold, then we need to check another condition.
If the wind speed is above a certain level, say 8 m/s, we might need to fetch the `lastIrrigationPauseTime` attribute.
This additional information can help us understand when the last time the irrigation system was paused due to high wind conditions, 
which might help to make more informed decisions about when and how to irrigate, considering both current and historical weather conditions.

Consider that you write a script that depending on a conditions written above will add to the message metadata additional key: `keyToFetch` with one of the next values:

- `lastIrrigationTime`
- `lastIrrigationPauseTime`

In order to fetch value of one of the following keys for the further message processing you can define next node configuration:

![image](${helpBaseUrl}/help/images/rulenode/examples/related-device-attributes-ft.png)

- message definition that match first condition after processing in the script node:

```json
{
  "msg": {
    "temperature": 26.5,
    "humidity": 75.2,
    "soilMoisture": 28.9,
    "windSpeed": 8.2,
    "windDirection": "NNE"
  },
  "metadata": {
    "deviceType": "default",
    "deviceName": "MM-001",
    "ts": "1685379440000",
    "keyToFetch": "lastIrrigationTime"
  }
}
```

<br>

- message definition that match second condition after processing in the script node:

```json
{
  "msg": {
    "temperature": 26.5,
    "humidity": 75.2,
    "soilMoisture": 32.5,
    "windSpeed": 10.4,
    "windDirection": "NNE"
  },
  "metadata": {
    "deviceType": "default",
    "deviceName": "MM-001",
    "ts": "1685379440000",
    "keyToFetch": "lastIrrigationPauseTime"
  }
}
```

<br>

Rule node configuration set to fetch data to the message metadata. In the following way:

- outgoing message for the first condition would be updated to:

```json
{
  "msg": {
    "temperature": 26.5,
    "humidity": 75.2,
    "soilMoisture": 28.9,
    "windSpeed": 8.2,
    "windDirection": "NNE"
  },
  "metadata": {
    "deviceType": "default",
    "deviceName": "MM-001",
    "ts": "1685379440000",
    "keyToFetch": "lastIrrigationTime",
    "ss_lastIrrigationTime": "1685369440000"
  }
}
```

<br>

- outgoing message the second condition would be updated to:

```json
{
  "msg": {
    "temperature": 26.5,
    "humidity": 75.2,
    "soilMoisture": 32.5,
    "windSpeed": 10.4,
    "windDirection": "NNE"
  },
  "metadata": {
    "deviceType": "default",
    "deviceName": "MM-001",
    "ts": "1685379440000",
    "keyToFetch": "lastIrrigationPauseTime",
    "ss_lastIrrigationPauseTime": "1685359440000"
  }
}
```

<br>

These examples showcases using the **related device attributes** node with dynamic configuration based on the substitution of metadata fields.

<br>
<br>

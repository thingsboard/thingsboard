#### Fields templatization

<div class="divider"></div>
<br/>

{% include rulenode/common_node_fields_templatization %}

##### Examples

Let's assume that we have a customer-based solution where customer manage two type of devices: `temperature` and `humidity` sensors.
Additionally, let's assume that customer configured the thresholds settings for each device type.
Threshold settings stored as an attributes on a customer level:

- *temperatureMinThreshold* and *temperatureMaxThreshold* for temperature sensor with values set to *10* and *30* accordingly.
- *humidityMinThreshold* and *humidityMaxThreshold* for humidity sensor with values set to *70* and *85* accordingly.

Each message received from device includes `deviceType` property in the message metadata
with either `temperature` or `humidity` value according to the sensor type.

In order to fetch the threshold value for the further message processing you can define next node configuration:

![image](${helpBaseUrl}/help/images/rulenode/examples/customer-attributes-ft.png)

Imagine that you receive message defined below from the `temperature` sensor
and forwarded it to the **customer attributes** node with configuration added above.

- incoming message definition:

```json
{
  "msg": {
    "temperature": 32
  },
  "metadata": {
    "deviceType": "temperature",
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
    "deviceType": "humidity",
    "deviceName": "HM-001",
    "ts": "1685379440000"
  }
}
```

<br>

Rule node configuration set to fetch data to the message metadata. In the following way:

- outgoing message for the `temperature` sensor would be updated to:

```json
{
  "msg": {
    "temperature": 32
  },
  "metadata": {
    "deviceType": "temperature",
    "deviceName": "TH-001",
    "ts": "1685379440000",
    "minThreshold": "10",
    "maxThreshold": "30"
  }
}
```

<br>

- outgoing message for the `humidity` sensor would be updated to:

```json
{
  "msg": {
    "humidity": 77
  },
  "metadata": {
    "deviceType": "humidity",
    "deviceName": "HM-001",
    "ts": "1685379440000",
    "minThreshold": "70",
    "maxThreshold": "85"
  }
}
```

<br>

These examples showcases using the **customer attributes** node with dynamic configuration based on the substitution of metadata fields.

<br>
<br>


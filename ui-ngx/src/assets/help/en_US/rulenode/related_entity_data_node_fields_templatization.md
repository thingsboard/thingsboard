#### Fields templatization

<div class="divider"></div>
<br/>

{% include rulenode/common_node_fields_templatization %}

##### Examples

Let's consider a scenario where we possess an asset that serves as a warehouse 
and is responsible for overseeing two categories of devices: 

- sensors measuring `temperature`. 
- sensors measuring `humidity`.

Additionally, let's assume that this asset has configured thresholds set as attributes for each device type:

- *temperature_min_threshold* and *temperature_max_threshold* for temperature sensor with values set to *10* and *30* accordingly.
- *humidity_min_threshold* and *humidity_max_threshold* for humidity sensor with values set to *70* and *85* accordingly.

Each message received from device includes `deviceType` property in the message metadata
with either `temperature` or `humidity` value according to the sensor type.

In order to fetch the threshold value for the further message processing you can define next node configuration:

![image](${helpBaseUrl}/help/images/rulenode/examples/related-entity-data-ft.png)
![image](${helpBaseUrl}/help/images/rulenode/examples/related-entity-data-ft-2.png)

Imagine that you receive message defined below from the `temperature` sensor
and forwarded it to the **related entity data** node with configuration added above.

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
    "min_threshold": "10",
    "max_threshold": "30"
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
    "min_threshold": "70",
    "max_threshold": "85"
  }
}
```

<br>

These examples showcases using the **related entity data** node with dynamic configuration based on the substitution of metadata fields.

<br>
<br>

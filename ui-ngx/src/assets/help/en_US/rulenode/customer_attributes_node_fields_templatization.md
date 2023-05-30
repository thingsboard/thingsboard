#### Fields templatization

<div class="divider"></div>
<br/>

{% include rulenode/common_node_fields_templatization %}

##### Examples

Let's assume that we have a customer-based solution where customer manage two type of devices: `temperature` and `humidity` sensors. 
Additionally, let's assume that customer configured the thresholds settings for each device type. 
Threshold settings stored as an attributes on a customer level:

 - *temperature_min_threshold* and *temperature_max_threshold* for temperature sensor with values set to *10* and *30* accordingly.
 - *humidity_min_threshold* and *humidity_max_threshold* for humidity sensor with values set to *70* and *85* accordingly.

Each message received from device includes `deviceType` property in the message metadata 
with either `temperature` or `humidity` value according to the sensor type.

In order to fetch the threshold value for the further message processing you can define next mapping in the configuration:

![image](${helpBaseUrl}/help/images/rulenode/examples/customer-attributes-ft.png)

Imagine that you receive message defined below from the `temperature` sensor 
and forwarded it to the **customer attributes** node with configuration added above.

 - incoming message definition:

```json
    {
      "msg":{
        "temperature":32
      },
      "metadata":{
        "deviceType":"temperature",
        "deviceName":"TH-001",
        "ts":1685379440000
      }
    }
```

Rule node configuration set to fetch data to the message metadata so the outgoing message would be updated to:

```json
    {
      "msg":{
        "temperature":32
      },
      "metadata":{
        "deviceType":"temperature",
        "deviceName":"TH-001",
        "ts":1685379440000,
        "min_threshold":"10", 
        "max_threshold":"30"
      }
    }
```

<br>

The same example for the `humidity` sensor:

- incoming message definition:

```json
    {
      "msg":{
        "humidity":77
      },
      "metadata":{
        "deviceType":"humidity",
        "deviceName":"HM-001",
        "ts":1685379440000
      }
    }
```

Rule node configuration wasn't changed so the outgoing message would be updated to:

```json
    {
      "msg":{
        "humidity":77
      },
      "metadata":{
        "deviceType":"humidity",
        "deviceName":"HM-001",
        "ts":1685379440000,
        "min_threshold":"70",
        "max_threshold":"85"
      }
    }
```

<br>

These examples showcases using the **customer attributes** node with dynamic configuration based on the substitution of metadata fields.

<br>
<br>


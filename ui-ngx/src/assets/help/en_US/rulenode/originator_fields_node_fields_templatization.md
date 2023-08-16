#### Fields templatization

<div class="divider"></div>
<br/>

{% include rulenode/common_node_fields_templatization %}

##### Examples

Let's assume that we have two device types in our use case:

- `smartDoorLock`
- `motionDetector`

Let's assume that device of type `smartDoorLock` and name `SDL-001` publish next type of messages to the system:

```json
{
  "msg": {
    "status": "locked"
  },
  "metadata": {
    "deviceName": "SDL-001",
    "deviceType": "smartDoorLock",
    "ts": "1685379440000"
  }
}
```

<br>

and device of type `motionDetector` and name `MD-001` publish next type of messages to the system:

```json
{
  "msg": {
    "motionDetected": "true"
  },
  "metadata": {
    "deviceName": "MD-001",
    "deviceType": "motionDetector",
    "ts": "1685379440000"
  }
}
```

<br>

Imagine that you send the messages received from the devices to the external systems
and depending on the device type you need add to the message the human-readable label
of the device with field name equal to the `deviceType` value from the message metadata. 

Let's assume that devices have next labels: 

- *Grocery warehouse door* for `SDL-001` device.
- *Grocery Warehouse motion detector* for `MD-001` device.

In order to fetch labels and add them to the message with the appropriate field name 
you can define the next node configuration:

![image](${helpBaseUrl}/help/images/rulenode/examples/originator-fields-ft.png)

<br>

Rule node configuration set to fetch data to the message. In the following way:

- outgoing message for the `SDL-001` device would be updated to:

```json
{
  "msg": {
    "status": "locked",
    "smartDoorLock": "Grocery warehouse door"
  },
  "metadata": {
    "deviceName": "SDL-001",
    "deviceType": "smartDoorLock",
    "ts": "1685379440000"
  }
}
```

<br>

- outgoing message for the `MD-001` device would be updated to:

```json
{
  "msg": {
    "motionDetected": "true",
    "motionDetector": "Grocery Warehouse motion detector"
  },
  "metadata": {
    "deviceName": "MD-001",
    "deviceType": "motionDetector",
    "ts": "1685379440000"
  }
}
```

<br>

These examples showcases using the **originator fields** node with dynamic configuration based on the substitution of metadata fields.

<br>
<br>

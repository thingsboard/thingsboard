#### Fields templatization

<div class="divider"></div>
<br/>

{% include rulenode/common_node_fields_templatization %}

##### Examples

Originator telemetry node support templatization for multiple configuration fields. Namely, you can specify the template in the
*Timeseries keys* list, and also there is an option to use the templatization for the fetch interval start and end if you are using *dynamic interval*. 

###### Example 1

Let's start with an example of using templatization for the *Timeseries keys* list. 
Imagine that you have a GPS tracker device(message originator) that publishes a telemetry messages that includes the following data readings:

- `latitude` - current device latitude value.
- `longitude` - current device longitude value.
- `event` - parameter that specifies the current state of the device. The value might be *parked* or *motion*.

Additionally let's imagine that devices periodically publishes other telemetry messages that includes additional information such as:

- `speed` - current speed value.
- `direction` - compass direction in which the device is moving.
- `acceleration` - how quickly the speed of the device is changing.
- `fuel_level` - current fuel level.
- `battery_level` - current battery level.
- `parked_location` - precise location where the device is parked.
- `parked_duration` - current park duration value.
- `parked_time` - timestamp when the device was parked.

Let's imagine that we need to make some historical analysis by fetching 3 latest telemetry readings for the keys listed below if the `event` value is set to *motion*: 

- `speed` 
- `direction` 
- `acceleration`
- `fuel_level`
- `battery_level`

Otherwise, if the `event` value is set to *parked* value we need to fetch 3 latest telemetry readings for the following data keys: 

- `parked_location`
- `parked_duration`
- `parked_time`
- `fuel_level`
- `battery_level`

Imagine that you created a script node that depending on the `event` value adds to the message metadata appropriate keyToFetch fields.

- message definition that match condition when `event` is set to *motion* value after processing in the script node:

```json
{
  "msg": {
    "latitude": "40.730610",
    "longitude": "-73.935242",
    "event": "motion"
  },
  "metadata": {
    "deviceName": "GPS-001",
    "deviceType": "GPS Tracker",
    "ts": "1685479440000",
    "keyToFetch1": "speed",
    "keyToFetch2": "direction",
    "keyToFetch3": "acceleration"
  }
}
```

<br>

- message definition that match condition when `event` is set to *parked* value after processing in the script node:

```json
{
  "msg": {
    "latitude": "40.730610",
    "longitude": "-73.935242",
    "event": "parked"
  },
  "metadata": {
    "deviceName": "GPS-001",
    "deviceType": "GPS Tracker",
    "ts": "1685379440000",
    "keyToFetch1": "parked_location",
    "keyToFetch2": "parked_duration",
    "keyToFetch3": "parked_time"
  }
}
```

<br>

In order to fetch the additional telemetry key values to make some historical analysis of the tracker's state you can define the next node configuration:

![image](${helpBaseUrl}/help/images/rulenode/examples/originator-telemetry-ft.png)

![image](${helpBaseUrl}/help/images/rulenode/examples/originator-telemetry-ft-2.png)

<br>

Rule node configuration is set to retrieve the telemetry from the fetch interval with configurable query parameters that you can check above. 
So let's imagine that 3 latest values for the keys that we are going to fetch are:

- `speed` - 5.2, 15.7, 30.2 (mph).
- `direction` - N(North), NE(North-East), E(East).
- `acceleration` - 2.2, 2.4, 2.5 (m/s²).
- `fuel_level` - 61.5, 57.4, 55.6 (%).
- `battery_level` - 88.1, 87.8, 87.2 (%).
- `parked_location` - dr5rtwceb (geohash). Same value for 3 latest data readings.
- `parked_duration` - 6300000, 7300000, 8300000 (ms).
- `parked_time` - 1685339240000 (ms). Same value for 3 latest data readings.

In the following way:

- outgoing message for the case when the `event` has value *motion* would be look like this:

```json
{
  "msg": {
    "latitude": "40.730610",
    "longitude": "-73.935242",
    "event": "motion"
  },
  "metadata": {
    "deviceName": "GPS-001",
    "deviceType": "GPS Tracker",
    "ts": "1685479440000",
    "keyToFetch1": "speed",
    "keyToFetch2": "direction",
    "keyToFetch3": "acceleration",
    "speed": "[{\"ts\":1685476840000,\"value\":5.2},{\"ts\":1685477840000,\"value\":15.7},{\"ts\":1685478840000,\"value\":30.2}]",
    "direction": "[{\"ts\":1685476840000,\"value\":\"N\"},{\"ts\":1685477840000,\"value\":\"NE\"},{\"ts\":1685478840000,\"value\":\"N\"}]",
    "acceleration": "[{\"ts\":1685476840000,\"value\":2.2},{\"ts\":1685477840000,\"value\":2.4},{\"ts\":1685478840000,\"value\":2.5}]",
    "fuel_level": "[{\"ts\":1685476840000,\"value\":61.5},{\"ts\":1685477840000,\"value\":57.4},{\"ts\":1685478840000,\"value\":55.6}]",
    "battery_level": "[{\"ts\":1685476840000,\"value\":88.1},{\"ts\":1685477840000,\"value\":87.8},{\"ts\":1685478840000,\"value\":87.2}]"
  }
}
```

<br>

- outgoing message for the case when the `event` has value *parked* would be look like this:

```json
{
  "msg": {
    "latitude": "40.730610",
    "longitude": "-73.935242",
    "event": "parked"
  },
  "metadata": {
    "deviceName": "GPS-001",
    "deviceType": "GPS Tracker",
    "ts": "1685379440000",
    "keyToFetch1": "parked_location",
    "keyToFetch2": "parked_duration",
    "keyToFetch3": "parked_time",
    "parked_location": "[{\"ts\":1685376840000,\"value\":\"dr5rtwceb\"},{\"ts\":1685377840000,\"value\":\"dr5rtwceb\"},{\"ts\":1685378840000,\"value\":\"dr5rtwceb\"}]",
    "parked_duration": "[{\"ts\":1685376840000,\"value\":6300000},{\"ts\":1685377840000,\"value\":7300000},{\"ts\":1685378840000,\"value\":8300000}]",
    "parked_time": "[{\"ts\":1685376840000,\"value\":1685376840000},{\"ts\":1685377840000,\"value\":1685377840000},{\"ts\":1685378840000,\"value\":1685378840000}]",
    "fuel_level": "[{\"ts\":1685376840000,\"value\":61.5},{\"ts\":1685377840000,\"value\":57.4},{\"ts\":1685378840000,\"value\":55.6}]",
    "battery_level": "[{\"ts\":1685376840000,\"value\":88.1},{\"ts\":1685377840000,\"value\":87.8},{\"ts\":1685378840000,\"value\":87.2}]"
  }
}
```

###### Example 2

This example will extend the previous example with additional condition: 

Imagine that you need to specify the fetch interval dynamically from the 1 hour ago till the current time. 
Additionally let's assume that the current time can be extracted from `ts` field that we have in the message metadata on each message received.
While the value of (1 hour ago) can be calculated in the script node that we use for adding keyToFetch fields into metadata.

In the following way:

- message definition that match condition when `event` is set to *motion* value after processing in the script node:

```json
{
  "msg": {
    "latitude": "40.730610",
    "longitude": "-73.935242",
    "event": "motion"
  },
  "metadata": {
    "deviceName": "GPS-001",
    "deviceType": "GPS Tracker",
    "ts": "1685479440000",
    "keyToFetch1": "speed",
    "keyToFetch2": "direction",
    "keyToFetch3": "acceleration",
    "dynamicIntervalStart": "1685475840000"
  }
}
```

- message definition that match condition when `event` is set to *parked* value after processing in the script node:

```json
{
  "msg": {
    "latitude": "40.730610",
    "longitude": "-73.935242",
    "event": "parked"
  },
  "metadata": {
    "deviceName": "GPS-001",
    "deviceType": "GPS Tracker",
    "ts": "1685379440000",
    "keyToFetch1": "parked_location",
    "keyToFetch2": "parked_duration",
    "keyToFetch3": "parked_time",
    "dynamicIntervalStart": "1685375840000"
  }
}
```

<br>

In order to fetch the data using dynamic interval we need enable *Use dynamic interval* option in the rule node configuration and specify the templates for the *Interval start* and *Interval end*: 


![image](${helpBaseUrl}/help/images/rulenode/examples/originator-telemetry-ft-3.png)

<br>

Other configuration wasn't change from our previous example. 
In the following way:

- outgoing message for the case when the `event` has value *motion* would be look like this:

```json
{
  "msg": {
    "latitude": "40.730610",
    "longitude": "-73.935242",
    "event": "motion"
  },
  "metadata": {
    "deviceName": "GPS-001",
    "deviceType": "GPS Tracker",
    "ts": "1685479440000",
    "keyToFetch1": "speed",
    "keyToFetch2": "direction",
    "keyToFetch3": "acceleration",
    "dynamicIntervalStart": "1685475840000",
    "speed": "[{\"ts\":1685476840000,\"value\":5.2},{\"ts\":1685477840000,\"value\":15.7},{\"ts\":1685478840000,\"value\":30.2}]",
    "direction": "[{\"ts\":1685476840000,\"value\":\"N\"},{\"ts\":1685477840000,\"value\":\"NE\"},{\"ts\":1685478840000,\"value\":\"N\"}]",
    "acceleration": "[{\"ts\":1685476840000,\"value\":2.2},{\"ts\":1685477840000,\"value\":2.4},{\"ts\":1685478840000,\"value\":2.5}]",
    "fuel_level": "[{\"ts\":1685476840000,\"value\":61.5},{\"ts\":1685477840000,\"value\":57.4},{\"ts\":1685478840000,\"value\":55.6}]",
    "battery_level": "[{\"ts\":1685476840000,\"value\":88.1},{\"ts\":1685477840000,\"value\":87.8},{\"ts\":1685478840000,\"value\":87.2}]"
  }
}
```

<br>

- outgoing message for the case when the `event` has value *parked* would be look like this:

```json
{
  "msg": {
    "latitude": "40.730610",
    "longitude": "-73.935242",
    "event": "parked"
  },
  "metadata": {
    "deviceName": "GPS-001",
    "deviceType": "GPS Tracker",
    "ts": "1685379440000",
    "keyToFetch1": "parked_location",
    "keyToFetch2": "parked_duration",
    "keyToFetch3": "parked_time",
    "dynamicIntervalStart": "1685375840000",
    "parked_location": "[{\"ts\":1685376840000,\"value\":\"dr5rtwceb\"},{\"ts\":1685377840000,\"value\":\"dr5rtwceb\"},{\"ts\":1685378840000,\"value\":\"dr5rtwceb\"}]",
    "parked_duration": "[{\"ts\":1685376840000,\"value\":6300000},{\"ts\":1685377840000,\"value\":7300000},{\"ts\":1685378840000,\"value\":8300000}]",
    "parked_time": "[{\"ts\":1685376840000,\"value\":1685376840000},{\"ts\":1685377840000,\"value\":1685377840000},{\"ts\":1685378840000,\"value\":1685378840000}]",
    "fuel_level": "[{\"ts\":1685376840000,\"value\":61.5},{\"ts\":1685377840000,\"value\":57.4},{\"ts\":1685378840000,\"value\":55.6}]",
    "battery_level": "[{\"ts\":1685376840000,\"value\":88.1},{\"ts\":1685377840000,\"value\":87.8},{\"ts\":1685378840000,\"value\":87.2}]"
  }
}
```

<br>

These examples showcases using the **originator telemetry** node with dynamic configuration based on the substitution of metadata fields.

<br>
<br>

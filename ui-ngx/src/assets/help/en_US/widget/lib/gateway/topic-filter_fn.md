### Topic filter

The `topicFilter` supports special symbols: `#` and `+`, allowing to subscribe to multiple topics.

Also, the MQTT connector supports shared subscriptions. To create a shared subscription you need to add `$share/` 
as a prefix for the topic filter and shared subscription group name. For example to subscribe to the my-shared-topic
in group **my-group-name** you can set the topic filter to “$share/**my-group-name**/my-shared-topic”.


#### Examples:

| Topic                  | Topic filter                     | Payload                                                                                                       | Description                                                                              |
|:-----------------------|----------------------------------|---------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------|
|   sensor/data          |  sensor/data                     |  `{"serialNumber": "SN-001", "sensorType": "Thermometer", "sensorModel": "T1000", "temp": 42, "hum": 58}`     | Device Name is part of the payload, to extract it you need to use JSON Path expression   |
|   sensor/SN-001/data   |  sensor/+/data                   |  `{"sensorType": "Thermometer", "sensorModel": "T1000", "temp": 42, "hum": 58}`                               | Device Name is part of the topic, to extract it you need to use regular expression.      |

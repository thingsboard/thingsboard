#### Fields templatization

<div class="divider"></div>
<br/>

{% include rulenode/common_node_fields_templatization %}

##### Example

Let's assume that a tenant manages temperature sensors.
When a temperature sensor reports a high temperature, the platform creates an alarm.

In addition, let's assume that each sensor has a group of alarm notification receivers
associated with it and a primary user that responsible for updating the alarm status.

Imagine this scenario: After creating an alarm, we use enrichment rule nodes to fetch information about notification
recipients and primary users. Following this, we format the necessary message using transformation nodes for delivery
via Twilio. Once this process is complete, our message is structured as follows:

```json
{
  "msg": "Thermostat TH-001 has a critical temperature of 32°C",
  "metadata": {
    "deviceType": "Thermostat",
    "deviceName": "TH-001",
    "ts": "1685379440000",
    "sender": "+12025550193",
    "receivers": "+12025550172,+12025550178,+12025550185"
  }
}
```
<br>

Here is a node configuration:

![image](${helpBaseUrl}/help/images/rulenode/examples/twilio-sms-phone-ft.png)

By using patterns such as ```${sender}``` and ```${receivers}```, you can dynamically send precise configuration values from
metadata to Twilio. This approach enhances flexibility, allowing for various senders and receivers to be specified.
Despite these dynamic elements, the format of the outgoing message remains consistent.

<br>
<br>

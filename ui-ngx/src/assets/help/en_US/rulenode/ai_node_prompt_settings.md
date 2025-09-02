#### Example Usage: AI-Powered Predictive Maintenance

This example demonstrates how to use the AI request node to analyze telemetry from rotating equipment for a predictive maintenance use case.

##### Scenario

Assume you’re monitoring a centrifugal pump that streams vibration, temperature, and acoustic readings.
To catch problems early and avoid downtime, you can use AI to analyze the telemetry for signs of **Bearing Wear**, **Misalignment**, **Overheating**, or **Imbalance** and return an alarm object if found. Downstream nodes can use it to create a ThingsBoard alarm and notify the maintenance team.

1. **Incoming message structure**

First, we need to collect telemetry readings. This can be achieved either by configuring a Calculated Field with the “Time series rolling” arguments to gather recent samples,
or by running a periodic check using nodes like "generator" and "originator telemetry" to fetch the latest samples and assemble the payload.

Message payload (shortened for brevity):

```json
{
  "acousticDeviation": {
    "timeWindow": { "startTs": 1755093373000, "endTs": 1755093414551 },
    "values": [
      { "ts": 1755093373000, "value": 5.0 },
      { "ts": 1755093373100, "value": 18.0 },
      { "ts": 1755093373200, "value": 17.0 },
      { "ts": 1755093414380, "value": 5.0 },
      { "ts": 1755093414551, "value": 17.0 }
    ]
  },
  "temperature": {
    "timeWindow": { "startTs": 1755093373000, "endTs": 1755093414551 },
    "values": [
      { "ts": 1755093373000, "value": 70.0 },
      { "ts": 1755093373120, "value": 86.0 },
      { "ts": 1755093373200, "value": 84.0 },
      { "ts": 1755093414380, "value": 70.0 },
      { "ts": 1755093414551, "value": 84.0 }
    ]
  },
  "vibration": {
    "timeWindow": { "startTs": 1755093373000, "endTs": 1755093414551 },
    "values": [
      { "ts": 1755093373000, "value": 4.2 },
      { "ts": 1755093373120, "value": 7.4 },
      { "ts": 1755093373182, "value": 8.0 },
      { "ts": 1755093414437, "value": 6.2 },
      { "ts": 1755093414551, "value": 7.2 }
    ]
  }
}
```

Message metadata:

```json
{
  "deviceName": "Pump-103",
  "deviceType": "CentrifugalPump"
}
```

2. **Prompt configuration**

As a second step, we need to explain the task to AI model. Describe the context of your device and the desired response format (in this case, minimal ThingsBoard alarm JSON object) in the system prompt. We will also put the task description in the system prompt since it does not change depending on a message. In the user prompt, we will use templates to dynamically inject telemetry data produced by the device.

**System prompt**

```
You are an AI predictive maintenance assistant that detects alarm conditions in telemetry data of industrial devices based on incident patterns.

Output JSON only. If an alarm condition is detected, output:
{
  "type": "Bearing Wear | Misalignment | Overheating | Imbalance",
  "severity": "CRITICAL | MAJOR | MINOR | WARNING",
  "details": { "summary": "2–3 sentences in plain English of concise, plain-language summary for maintenance teams; include units when citing values." }
}
If no alarm condition is detected, output: {}

Inputs: time-stamped vibration (mm/s), temperature (°C), acoustic spectrum deviation (%).

Telemetry thresholds:
- Vibration (mm/s): ≤4.5 normal; 4.5–5.0 WARNING; 5.0–6.0 MINOR; 6.0–7.1 MAJOR; >7.1 CRITICAL
- Temperature (°C): ≤75 normal; 75–80 WARNING; 80–85 MAJOR; >85 CRITICAL
- Acoustic deviation (%): ≤15 normal; 15–25 WARNING; 25–40 MINOR; 40–60 MAJOR; >60 CRITICAL

Incident patterns:
- Bearing Wear: gradual vibration rise + temperature spike.
- Misalignment: sudden vibration spike without temperature change.
- Imbalance: rising vibration + irregular acoustics; temperature near normal.
- Overheating: temperature >85 °C ≥10 min or Δtemp ≥10 °C/10 min with Δvib <1.0 mm/s; or 75–85 °C ≥30 min with normal vib/acoustic.

Severity policy:
- Start with the max of per-signal severities; if ≥2 signals are abnormal, escalate one level (cap at CRITICAL).
- Ignore very brief blips (<2 samples just over a boundary) unless strongly pattern-matched.
- Be conservative; use input units.
```

**User prompt**

```
Analyze telemetry from a "${deviceType}" named "${deviceName}".

Data:
$[*]
```
3. **Response format** (optional)

In the previous step, we described desired response format in the system prompt, but it is possible to enforce format with JSON Schema if model you are using supports it. We recommend using JSON Schema if possible. Here is the example of response schema you can use (if using JSON Schema, in the system prompt just say that model output should be in JSON format):

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "title": "Alarm",
  "type": "object",
  "additionalProperties": false,
  "required": ["type", "severity", "details"],
  "properties": {
    "type": {
      "type": "string",
      "description": "Incident type",
      "enum": ["Bearing Wear", "Misalignment", "Imbalance", "Overheating"]
    },
    "severity": {
      "type": "string",
      "description": "Severity level of the incident",
      "enum": ["WARNING", "MINOR", "MAJOR", "CRITICAL"]
    },
    "details": {
      "type": "object",
      "additionalProperties": false,
      "required": ["summary"],
      "properties": {
        "summary": {
          "type": "string",
          "description": "2–3 sentences in plain English of concise, plain-language summary for maintenance teams; include units when citing values."
        }
      }
    }
  }
}
```

4. **How it works**

When the message containing sample data from **Pump-103** is processed, the templates are substituted:

* `${deviceName}` → `"Pump-103"`
* `${deviceType}` → `"CentrifugalPump"`
* `$[*]` → the entire message payload JSON (e.g. telemetry data we collected in step 1)

> **Tip:** `${*}` can substitute the entire metadata JSON if needed.

The final instruction sent to the model is the System prompt plus the substituted User prompt. AI response will be placed in outgoing message payload.

5. **Expected AI output**

Given the sample data from step 1, the AI will likely output something like this:

```json
{
  "type": "Bearing Wear",
  "severity": "CRITICAL",
  "details": {
      "summary": "Pump-103 showed a vibration spike from 4.2 to 8.0 mm/s with continued elevated levels (6.2–7.2 mm/s), along with a temperature spike to 86 °C and recurrent 84 °C. With acoustics only in the WARNING band (~17–18%), this pattern indicates bearing wear; inspect and replace the bearing promptly to prevent failure."
    }
}
```

6. **Next steps**

Check the AI response: if it’s a non-empty object, it’s ready-to-use alarm JSON. Route it directly to the "create alarm" node to create an alarm. If it is empty, just ignore the output as everything is normal.

#### Example Usage: AI-Powered Alarm Analysis
This example demonstrates how to use the AI node to automatically analyze a new device alarm, generate a human-readable summary, and suggest troubleshooting steps.

##### Scenario
An IoT freezer unit generates a "High Temperature" alarm. We want the AI to process this alarm data to create a clear summary and a recommended action plan for an operator.

1. **Incoming message structure**

Message body (represents an alarm, some alarm fields omitted for brevity):
```json
{
  "type": "High Temperature",
  "details": {
    "currentTemp_C": -5,
    "threshold_C": -18
  }
}
```

Message metadata:
```json
{
  "deviceName": "Freezer-B7",
  "deviceType": "CommercialFreezer"
}
```

2. **Prompt configuration**

**System prompt**

Here, we set the AI's role and enforce a strict JSON output format. This ensures the output is always machine-parsable.
```
You are an expert AI assistant for IoT operations. 
Your task is to analyze device data and respond with a single, valid JSON object. 
Do not include any text, explanations, or markdown formatting before or after the JSON output.
```

**User prompt**

This prompt defines the specific task, using templates to dynamically insert data from the incoming alarm message.
```
Analyze the following alarm from a "${deviceType}" unit named "${deviceName}".

Alarm Data:
$[*]

Based on the alarm data, generate a JSON object with two keys:
1. "summary": A brief, human-readable summary of the event.
2. "action": A concrete, recommended next step for an operator.
```

3. **How it works**

When the alarm message from "Freezer-B7" is processed by the AI node, the templates are substituted with the actual data:
- `${deviceName}` becomes "Freezer-B7"
- `${deviceType}` becomes "CommercialFreezer"
- `$[*]` is replaced by the entire message body JSON: `{"type":"High Temperature","details":{"currentTemp_C":-5,"threshold_C":-18}}`
> **Note:** You can also use `${*}`. In this case, it will be replaced with the entire message metadata JSON.

The final instruction sent to the AI is a combination of the system and the substituted user prompt.

4. **Expected AI output**

Given the combined instructions, the AI would generate the following structured JSON output, which can then be used in subsequent rule nodes (e.g., to send an enriched email).

```json
{
  "summary": "Critical high temperature alert on freezer unit Freezer-B7. The current temperature is -5°C, which is significantly above the required threshold of -18°C.",
  "action": "Dispatch technician immediately to inspect the unit's cooling system and ensure the door is properly sealed. Investigate for potential power issues."
}
```

> **Note:** The scenario above is a hypothetical example designed to illustrate the functionality of the node and its templating capabilities.
> The specific details, such as freezer alarms, are used for demonstration purposes and are not intended to suggest or limit the potential use cases.

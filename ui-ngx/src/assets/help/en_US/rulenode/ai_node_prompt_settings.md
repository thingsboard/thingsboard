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

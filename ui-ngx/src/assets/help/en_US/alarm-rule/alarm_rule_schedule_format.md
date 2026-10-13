#### Active-all-the-time schedule format

For a schedule that is always active, the argument can be an empty JSON object or a JSON object in the following format:

```javascript
{
  "type": "ANY_TIME"
}
```

#### Specific time schedule format

The argument value for a specific time schedule must be a JSON object in the following format (the `type` field is optional):

```javascript
{
  "type": "SPECIFIC_TIME",
  "daysOfWeek": [
    2,
    4
  ],
  "endsOn": 0,
  "startsOn": 0,
  "timezone": "Europe/Kiev"
}
```

<ul>
<li>
<b>timezone:</b> the name of the timezone.
</li>
<li>
<b>daysOfWeek:</b> days of the week (Monday = 1, Tuesday = 2, ..., Sunday = 7) when the schedule should be active.
</li>
<li>
<b>startsOn:</b> time of day in milliseconds from the start of the day (00:00) when the schedule becomes active on the selected days.
</li>
<li>
<b>endsOn:</b> time of day in milliseconds from the start of the day (00:00) when the schedule stops being active on the selected days.
If this value is not provided or equals 0, it defaults to the full day (24 hours in milliseconds).
</li>
</ul>

If both <b>startsOn</b> and <b>endsOn</b> are 0, the schedule is active for the entire day.

#### Custom time schedule format

The argument value for a specific time schedule must be a JSON object in the following format (the `type` field is optional):

```javascript
{
  "type": "CUSTOM",
  "timezone": "Europe/Kiev",
  "items": [
    {
      "dayOfWeek": 1,
      "enabled": true,
      "endsOn": 0,
      "startsOn": 0
    },
    {
      "dayOfWeek": 2,
      "enabled": true,
      "endsOn": 0,
      "startsOn": 0
    },
    {
      "dayOfWeek": 3,
      "enabled": true,
      "endsOn": 0,
      "startsOn": 0
    },
    {
      "dayOfWeek": 4,
      "enabled": true,
      "endsOn": 0,
      "startsOn": 0
    },
    {
      "dayOfWeek": 5,
      "enabled": true,
      "endsOn": 0,
      "startsOn": 0
    },
    {
      "dayOfWeek": 6,
      "enabled": true,
      "endsOn": 0,
      "startsOn": 0
    },
    {
      "dayOfWeek": 7,
      "enabled": true,
      "endsOn": 0,
      "startsOn": 0
    }
  ]
}
```

<ul>
<li>
<b>timezone:</b> the name of the timezone.
</li>
<li>
<b>items:</b> a list of day-specific schedule entries.
</li>
</ul>

Each item represents one day of the week and contains:
<ul>
<li>
<b>dayOfWeek:</b> the day number (Monday = 1, Tuesday = 2, ..., Sunday = 7)
</li>
<li>
<b>enabled:</b> a <code>boolean</code> value that defines whether this day is active in the schedule.
</li>
<li>
<b>startsOn:</b> time of day in milliseconds from the start of the day (00:00) when the schedule becomes active for that day.
</li>
<li>
<b>endsOn:</b> time of day in milliseconds from the start of the day (00:00) when the schedule stops being active for that day.
If this value is not provided or equals 0, it defaults to the full day (24 hours in milliseconds).
</li>
</ul>

If both <b>startsOn</b> and <b>endsOn</b> are 0, the schedule is active for the entire day.

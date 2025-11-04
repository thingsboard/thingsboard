#### Active all time schedule format

An attribute with a dynamic value for an active all-time schedule format must contain an empty JSON object or JSON in the following format:

```javascript
{
  "type": "ANY_TIME"
}
```

#### Specific time schedule format

An attribute with a dynamic value for a specific schedule format must have JSON in the following format:

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
<b>timezone:</b> this value is used to designate the timezone you are using.
</li>
<li>
<b>daysOfWeek:</b> this value is used to designate the days in numerical representation (Monday - 1, Tuesday 2, etc.) on which the schedule will be active.
</li>
<li>
<b>startsOn:</b> this value is used to designate the timestamp in milliseconds, from which the schedule will be active for the designated days.
</li>
<li>
<b>endsOn:</b> this value is used to designate the timestamp in milliseconds until which the schedule will be active for the specified days.
</li>
</ul>
When <b>startsOn</b> and <b>endsOn</b> equals 0 it's means that the schedule will be active the whole day.

#### Custom time schedule format

An attribute with a dynamic value for a custom schedule format must have JSON in the following format:

```javascript
{
  "type": "CUSTOM"
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
<b>timezone:</b> this value is used to designate the timezone you are using.
</li>
<li>
<b>items:</b> the array of values representing the days on which the schedule will be active.
</li>
</ul>

One array item contains such fields:
<ul>
<li>
<b>dayOfWeek:</b> this value is used to designate the specified day in numerical representation (Monday - 1, Tuesday 2, etc.) on which the schedule will be active.
</li>
<li>
<b>enabled:</b> this <code>boolean</code> value, used to designate that the specified day in the schedule will be enabled.
</li>
<li>
<b>startsOn:</b> this value is used to designate the timestamp in milliseconds, from which the schedule will be active for the designated day.
</li>
<li>
<b>endsOn:</b> this value is used to designate the timestamp in milliseconds until which the schedule will be active for the specified day.
</li>
</ul>
When <b>startsOn</b> and <b>endsOn</b> equals 0 it's means that the schedule will be active the whole day.

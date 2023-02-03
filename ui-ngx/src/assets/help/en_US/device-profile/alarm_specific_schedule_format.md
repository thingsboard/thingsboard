#### Specific schedule format

An attribute with a dynamic value for a specific schedule format must have JSON in the following format:

```javascript
{
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

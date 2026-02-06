## Release Note: TBEL Date Formatting Changes (Java 21+)

### Summary

When upgrading ThingsBoard to Java 21 or later, users may notice changes in locale-formatted date/time strings produced by the `TbDate` class in TBEL scripts. These changes are caused by updates to the Unicode CLDR (Common Locale Data Repository) in Java 21+.

### What Changed

| Format | Java 17 | Java 21+ |
|--------|---------|----------|
| Time with AM/PM | `9:04:05 PM` | `9:04:05 PM` (narrow no-break space before AM/PM) |
| Full datetime (English) | `Tuesday, September 5, 2023 at 9:04:05 PM` | `Tuesday, September 5, 2023, 9:04:05 PM` |
| Full datetime (Ukrainian) | `середа, 6 вересня 2023 р. о 04:04:05` | `середа, 6 вересня 2023 р., 04:04:05` |
| Short datetime (Arabic) | `5/9/2023, 9:04:05 م` | `5/9/2023، 9:04:05 م` (Arabic comma) |
| UTC timezone display | `Eastern European Time` | `Kyiv (+0)` |

### Impact

TBEL scripts that rely on exact string matching or parsing of locale-formatted dates may behave differently after upgrading. For example:

```javascript
// ⚠️ May break after upgrade - string comparison
var dateStr = new Date(ts).toLocaleString("en-US", "America/New_York");
if (dateStr == "9/5/23, 9:04:05 PM") {
    // Will fail - invisible character difference
}

// ⚠️ May break after upgrade - string splitting
var parts = new Date(ts).toLocaleTimeString("en-US", tz).split(" ");
// "PM" now preceded by narrow no-break space (U+202F), not regular space
```

### Recommendations

1. **Use ISO formats for comparisons and storage**
   ```javascript
   // ✅ Stable across Java versions
   var dateStr = new Date(ts).toISOString();  // "2023-09-05T21:04:05Z"
   var dateJson = new Date(ts).toJSON();      // "2023-09-05T21:04:05.000Z"
   ```

2. **Use explicit patterns for consistent formatting**
   ```javascript
   // ✅ Explicit pattern - stable output
   var dateStr = new Date(ts).toLocaleString("en-US", '{"pattern": "M/d/yyyy, h:mm:ss a"}');
   ```

3. **Use numeric getters for comparisons**
   ```javascript
   // ✅ Compare numeric values instead of strings
   var d = new Date(ts);
   if (d.getHours() == 21 && d.getMinutes() == 4) { ... }
   ```

4. **Avoid string equality checks on formatted dates**
   ```javascript
   // ❌ Avoid
   if (date.toLocaleString() == storedDateString) { ... }

   // ✅ Prefer
   if (date.getTime() == storedTimestamp) { ... }
   ```

### Affected Methods

The following `TbDate` methods may produce different output:
- `toLocaleString()`
- `toLocaleDateString()`
- `toLocaleTimeString()`
- `toString()`
- `toDateString()`
- `toTimeString()`
- `toUTCString()`

### Unaffected Methods

These methods produce consistent output across Java versions:
- `toISOString()`
- `toJSON()`
- `getTime()` / `valueOf()`
- All numeric getters (`getFullYear()`, `getMonth()`, `getDate()`, `getHours()`, etc.)

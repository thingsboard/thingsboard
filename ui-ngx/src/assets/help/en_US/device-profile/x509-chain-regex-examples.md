#### Examples of RegEx usage

* **Pattern:** <code>.*</code> - matches any character (until line terminators)
  <br>**CN sample:** <code>DeviceName\nAdditionalInfo</code>
  <br>**Pattern matches:** <code>DeviceName</code>

* **Pattern:** <code>^([^@]+)</code> - matches any string that starts with one or more characters that are not the <code>@</code> symbol (<code>@</code> could be replaced by any other symbol)
  <br>**CN sample:** <code>DeviceName@AdditionalInfo</code>
  <br>**Pattern matches:** <code>DeviceName</code>

* **Pattern:** <code>[\w]*$</code> (equivalent to <code>[a-zA-Z0-9_]\*$</code>) - matches zero or more occurences of any word character  (letter, digit or underscore) at the end of the string
  <br>**CN sample:** <code>AdditionalInfo2110#DeviceName_01</code>
  <br>**Pattern matches:** <code>DeviceName_01</code>

**Note:** Client will get error response in case regex is failed to match.

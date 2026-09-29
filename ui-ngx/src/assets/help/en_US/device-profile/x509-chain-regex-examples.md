#### Examples of RegEx usage

The regular expression is required to extract device name from the X509 certificate's common name.
The regular expression syntax is based on Java [Pattern](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/regex/Pattern.html).
You may also use this [resource](https://regex101.com/) to test your expressions but make sure you select Java 8 flavor.

* **Pattern:**<code>(.*)\.company.com</code>- matches any characters before the ".company.com".
  <br>**CN sample:**<code>DeviceA.company.com</code>
  <br>**Result:**<code>DeviceA</code>

* **Pattern:** <code>(.*)@company.com</code>- matches any characters before the "@company.com".
  <br>**CN sample:**<code>DeviceA@company.com</code>
  <br>**Result:**<code>DeviceA</code>

* **Pattern:** <code>prefix(.*)suffix@company.com</code>- matches characters between "prefix" and "suffix@company.com".
  <br>**CN sample:**<code>prefixDeviceAsuffix@company.com</code>
  <br>**Pattern matches:** <code>DeviceA</code>

* **Pattern:** <code>\\D+\\.(.*)\\.\\d+@company.com</code>- matches characters between not digits prefix followed by period and sequence of digits with "@company.com" ending.
  <br>**CN sample:**<code>region.DeviceA.220423@company.com</code>
  <br>**Pattern matches:** <code>DeviceA</code>

##### Routing products to different device profiles

When several device profiles of the same tenant share one issuing CA certificate, the expression also selects
which device profile the device belongs to. Capture the whole common name so that the device names stay distinct.

* **Device profile "Sensor" pattern:** <code>^(sensor-.*)$</code>
  <br>**CN sample:** <code>sensor-SN00042</code>
  <br>**Result:** <code>sensor-SN00042</code>

* **Device profile "Gateway" pattern:** <code>^(gateway-.*)$</code>
  <br>**CN sample:** <code>gateway-SN00042</code>
  <br>**Result:** <code>gateway-SN00042</code>

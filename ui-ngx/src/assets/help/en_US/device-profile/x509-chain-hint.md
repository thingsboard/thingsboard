##### X509 Certificate Chain info

X.509 certificates strategy is used to provision devices by client certificates in two-way TLS communication.

<b>This strategy can:</b>
* check for pre-provisioned devices
* update X.509 device credentials
* create new devices

<b>The user uploads</b> X.509 certificate to the device profile and sets a regular expression to fetch the device name from *Common Name (CN)*.

<b>Client certificates must</b> be signed by X.509 certificate, pre-uploaded for this device profile to provision devices by the strategy. 

<b>The client must</b> establish a TLS connection using the entire chain of certificates (this chain must include device profile X.509 certificate on the last level).

If a device already exists with outdated X.509 credentials, this strategy automatically updates it with the device certificate's credentials from the chain.

<b>Important:</b> Uploaded certificates should be neither root nor intermediate certificates that are provided by a well-known *Certificate Authority (CA)*.

##### Sharing one certificate between device profiles

The same X.509 certificate may be uploaded to **several device profiles of the same tenant**, which is useful when
one issuing *Certificate Authority (CA)* signs the certificates of different products. Each of those device
profiles must then declare a **different** CN regular expression: when a device connects, the profile whose
expression matches the certificate's *Common Name (CN)* is the one the device is provisioned into.

<b>The expressions must be mutually exclusive.</b> If a common name matches more than one of the device profiles
sharing the certificate, the device is rejected rather than assigned to an arbitrary profile. Note that the
default expression <code>(.*)</code> matches every common name, so an existing device profile must be given a
more specific expression before a second one can share its certificate.

<b>Capture the whole common name</b> when profiles share a certificate. The captured group also becomes the
device name, and device names must be unique per tenant - so with the common names <code>sensor-SN00042</code> and
<code>gateway-SN00042</code>, the expressions <code>^sensor-(.*)$</code> and <code>^gateway-(.*)$</code> would both
produce the device name <code>SN00042</code> and collide. Use <code>^(sensor-.*)$</code> and
<code>^(gateway-.*)$</code> instead.

A certificate can still be used by only one tenant.

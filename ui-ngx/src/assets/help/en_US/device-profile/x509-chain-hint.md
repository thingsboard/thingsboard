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

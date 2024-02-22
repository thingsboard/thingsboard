#### QR code text function

<div class="divider"></div>
<br/>

*function (data): string*

A JavaScript function used to calculate text to be displayed as QR code.

**Parameters:**

<ul>
  <li><b>data:</b> <code><a href="https://github.com/thingsboard/thingsboard/blob/5bb6403407aa4898084832d6698aa9ea6d484889/ui-ngx/src/app/modules/home/components/widget/lib/maps/map-models.ts#L108" target="_blank">FormattedData[]</a></code> - An array of <a href="https://github.com/thingsboard/thingsboard/blob/5bb6403407aa4898084832d6698aa9ea6d484889/ui-ngx/src/app/modules/home/components/widget/lib/maps/map-models.ts#L108" target="_blank">FormattedData</a> objects resolved from configured datasources.<br/>
     Each object represents basic entity properties (ex. <code>entityId</code>, <code>entityName</code>)<br/>and provides access to other entity attributes/timeseries declared in widget datasource configuration.
  </li>
</ul>

**Returns:**

Should return string value presenting text to be displayed as QR code.

<div class="divider"></div>

##### Examples

* Prepare QR code text from name of the first entity if present:

```javascript
return data[0] ? data[0]['entityName'] : '';
{:copy-code}
```

<ul>
<li>
Prepare QR code text to use as device claiming info (in this case <code>{deviceName: string, secretKey: string}</code>).<br>
Let's assume device has <code>claimingData</code> attribute with string JSON value containing <code>secretKey</code> field<br>
(see <a target="_blank" href="${siteBaseUrl}/docs/user-guide/claiming-devices/">Claiming devices</a>):
</li>
</ul>

```javascript
var entityData = data[0];
if (entityData) {
    return JSON.stringify({
        deviceName: entityData.entityName,
        secretKey: JSON.parse(entityData.claimingData).secretKey
    });
} else {
    return '';
}
{:copy-code}
```

<br>
<br>

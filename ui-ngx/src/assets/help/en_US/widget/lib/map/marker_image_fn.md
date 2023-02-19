#### Marker image function

<div class="divider"></div>
<br/>

*function (data, images, dsData, dsIndex): {url: string, size: number}*

A JavaScript function used to compute marker image.

**Parameters:**

<ul>
  {% include widget/lib/map/map_fn_args %}
</ul>

**Returns:**

Should return marker image data having the following structure:

```typescript
{ 
   url: string,
   size: number
}
```

- *url* - marker image url;
- *size* - marker image size;

In case no data is returned, default marker image will be used. 

<div class="divider"></div>

##### Examples

<ul>
<li>
Calculate image url depending on <code>temperature</code> telemetry value for <code>thermometer</code> device type.<br>
Let's assume 4 images are defined in <b>Marker images</b> section. Each image corresponds to particular temperature level:
</li>
</ul>

```javascript
var type = data['Type'];
if (type == 'thermometer') {
  var res = {
    url: images[0],
    size: 40
  }
  var temperature = data['temperature'];
  if (typeof temperature !== undefined) {
    var percent = (temperature + 60)/120;
    var index = Math.min(3, Math.floor(4 * percent));
    res.url = images[index];
  }
  return res;
}
{:copy-code}
```

<br>
<br>

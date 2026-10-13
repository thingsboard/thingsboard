#### Marker image function

<div class="divider"></div>
<br/>

*function (data, images, dsData): {url: string, size: number}*

A JavaScript function used to compute marker image.

**Parameters:**

<ul>
  {% include widget/lib/map/marker_image_fn_args %}
</ul>

**Returns:**

Should return marker image data having the following structure:

```typescript
{
  url: string;
  size: number;
  markerOffset?: [number, number];
  tooltipOffset?: [number, number];
}
```

- *url* - marker image url;
- *size* - marker image size;
- *markerOffset* - optional array of two numbers presenting relative horizontal and vertical offset of the marker image;
- *tooltipOffset* - optional array of two numbers presenting relative horizontal and vertical offset of the marker image tooltip;

In case no data is returned, default marker image will be used. 

<div class="divider"></div>

##### Examples

<ul>
<li>
Calculate image url depending on <code>temperature</code> telemetry value for <code>thermometer</code> device type.<br>
Let's assume 4 images are defined in <b>Marker images</b> section. Each image corresponds to particular temperature level.<br/>
Ensure that the <code>Type</code> and <code>temperature</code> keys are included in the <b>additional data keys</b> configuration:
</li>
</ul>

```javascript
var type = data.Type;
if (type == 'thermometer') {
  var res = {
    url: images[0],
    size: 40
  }
  var temperature = data.temperature;
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

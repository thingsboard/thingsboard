#### Shape fill image function

<div class="divider"></div>
<br/>

*function (data, images, dsData): {url: string}*

A JavaScript function used to compute shape fill image.

**Parameters:**

<ul>
  {% include widget/lib/map/shape_fill_image_fn_args %}
</ul>

**Returns:**

Should return shape fill image data having the following structure:

```typescript
{
  url: string;
  preserveAspectRatio?: boolean;
  opacity?: number;
  angle?: number;
  scale?: number;
}
```

- *url* - fill image url;
- *preserveAspectRatio* - optional property indicating whether to preserve image aspect ratio (`true` if not specified);
- *opacity* - optional image opacity, number value from 0 to 1;
- *angle* - optional image rotation angle, number value from 0 to 360;
- *scale* - optional image scale, number value (1 - original size, smaller value - scale down, bigger value - scale up);

In case no data is returned, default fill image will be used.

<div class="divider"></div>

##### Examples

<ul>
<li>
Calculate image URL and rotation angle depending on <code>windSpeed</code> and <code>windDirection</code> telemetry values for a <code>weather station</code> device type.<br/>
Let's assume 3 images are defined in the Shape fill images section. Each image corresponds to a particular wind speed level: low (e.g., <5 m/s), medium (e.g., 5-15 m/s), and high (e.g., >15 m/s).<br/>
Ensure that the  <code>Type</code>, <code>windSpeed</code> and <code>windDirection</code> keys are included in the <b>additional data keys</b> configuration.
</li>
</ul>

```javascript
const type = data.Type;
if (type === 'weather station') {
  const result = {
    url: images[0],
    opacity: 0.8,
    angle: 0
  };
  const windSpeed = data.windSpeed;
  const windDirection = data.windDirection;
  if (typeof windSpeed !== 'undefined' && typeof windDirection !== 'undefined') {
    if (windSpeed < 5) {
      result.url = images[0];
    } else if (windSpeed < 15) {
      result.url = images[1];
    } else {
      result.url = images[2];
    }
    result.angle = windDirection;
  }
  return result;
}
{:copy-code}
```

<ul>
<li>
Returns the image URL in the <code>image</code> attribute for a <code>weather station</code> device type.<br/> 
Ensure that the <code>Type</code> and <code>image</code> keys are included in <b>additional data keys</b> configuration.
</li>
</ul>

```javascript
const type = data.Type;
const image = data.image;
if (type === 'weather station' && image !== undefined) {
  return {
    url: image,
    opacity: 0.8
  };
}
{:copy-code}
```

<br>
<br>

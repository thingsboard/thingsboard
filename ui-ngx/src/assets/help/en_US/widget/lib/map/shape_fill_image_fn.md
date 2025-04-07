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
  opacity?: number;
  angle?: number;
  scale?: number;
}
```

- *url* - fill image url;
- *opacity* - optional image opacity, number value from 0 to 1;
- *angle* - optional image rotation angle, number value from 0 to 360;
- *scale* - optional image scale, number value (1 - original size, smaller value - scale down, bigger value - scale up);

In case no data is returned, default fill image will be used.

<div class="divider"></div>

##### Examples

<ul>
<li>
TODO:
</li>
</ul>

```javascript
TODO:
{:copy-code}
```

<br>
<br>

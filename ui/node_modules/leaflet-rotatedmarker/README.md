Leaflet Rotated Marker
===

Enables rotation of marker icons in Leaflet.

Compatible with versions 0.7.* and 1.* of Leaflet. Doesn't work on IE < 9.

```bash
npm install leaflet-rotatedmarker
```

Usage
---

```js
L.marker([48.8631169, 2.3708919], {
    rotationAngle: 45
}).addTo(map);
```

API
---

It simply extends the `L.Marker` class with two new options:

Option | Type | Default | Description  
-------|------|---------|------------
**`rotationAngle`** | `Number` | 0 | Rotation angle, in degrees, clockwise.
**`rotationOrigin`** | `String` | `'bottom center'` | The rotation center, as a [`transform-origin`](https://developer.mozilla.org/en-US/docs/Web/CSS/transform-origin) CSS rule.

and two new methods:

Method | Returns | Description
-------|---------|------------
**`setRotationAngle(newAngle)`** | `this` | Sets the rotation angle value.
**`setRotationOrigin(newOrigin)`** | `this` | Sets the rotation origin value.

The default `rotationOrigin` value will rotate around the bottom center point, corresponding to the "tip" of the marker for most commonly used icons. If your marker icon has no tip, or you want to rotate around its center, use `center center`.

Note
---

On purpose, it doesn't rotate marker icon shadows. Mainly because there is no way to make it look good with the perspective of classic, pin type shadows (anyway, these shadows are so 2005, right?).

So just disable icon shadows, or use simple ones which will work for all marker angles.

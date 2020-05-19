# Leaflet PolylineDecorator

[![CDNJS](https://img.shields.io/cdnjs/v/leaflet-polylinedecorator.svg)](https://cdnjs.com/libraries/leaflet-polylinedecorator)

A Leaflet plug-in to define and draw patterns on existing Polylines or along coordinate paths.
[Demo](http://bbecquet.github.io/Leaflet.PolylineDecorator/example/example.html).

## Compatibility with Leaflet versions

**The current version of the plugin (on the `master` branch) works only with versions 1.\* of Leaflet**.

For a version of the plugin compatible with the older 0.7.* Leaflet releases, use the `leaflet-0.7.2` branch. But this branch is not maintained anymore and Leaflet 1.* has been around for a while, so you should definitely update.

## npm / bower

```
npm install leaflet-polylinedecorator
```

```
bower install leaflet-polylinedecorator
```

## Features

* Dashed or dotted lines, arrow heads, markers following line
* Works on Polygons too! (easy, as Polygon extends Polyline)
* Multiple patterns can be applied to the same line
* New behaviors can be obtained by defining new symbols

## Screenshot

![screenshot](https://raw.github.com/bbecquet/Leaflet.PolylineDecorator/master/screenshot.png "Screenshot showing different applications of the library")

## Usage

To create a decorator and add it to the map: `L.polylineDecorator(latlngs, options).addTo(map);`

* `latlngs` can be one of the following types:

 * `L.Polyline`
 * `L.Polygon`
 * an array of `L.LatLng`, or with Leaflet's simplified syntax, an array of 2-cells arrays of coordinates (useful if you just want to draw patterns following coordinates, but not the line itself)
 * an array of any of these previous types, to apply the same patterns to multiple lines

* `options` has a single property `patterns`, which is an array of `Pattern` objects.

### `Pattern` definition

Property | Type | Required | Description
--- | --- | --- | ---
`offset`| *see below* | No | Offset of the first pattern symbol, from the start point of the line. Default: 0.
`endOffset`| *see below* | No | Minimum offset of the last pattern symbol, from the end point of the line. Default: 0.
`repeat`| *see below* | Yes | Repetition interval of the pattern symbols. Defines the distance between each consecutive symbol's anchor point.
`symbol`| Symbol factory | Yes | Instance of a symbol factory class.

`offset`, `endOffset` and `repeat` can be each defined as a number, in pixels, or in percentage of the line's length, as a string (ex: `'10%'`).

### Methods

Method | Description
--- | ---
`setPaths(latlngs)` | Changes the path(s) the decorator applies to. `latlngs` can be all the types supported by the constructor. Useful for example if you remove polyline from a set, or coordinates change.
`setPatterns(<Pattern[]> patterns)` | Changes the decorator's pattern definitions, and update the symbols accordingly.

## Example

```javascript
var polyline = L.polyline([...]).addTo(map);
var decorator = L.polylineDecorator(polyline, {
    patterns: [
        // defines a pattern of 10px-wide dashes, repeated every 20px on the line
        {offset: 0, repeat: 20, symbol: L.Symbol.dash({pixelSize: 10})}
    ]
}).addTo(map);
```

## Performance note/alternatives

This plugin creates actual `L.Layer` objects (markers, polyline, etc.) to draw the pattern symbols. This is extra customizable as you can define your own symbols, but it may have an impact on the responsiveness of your map if you have to draw a lot of symbols on many large polylines.

Here are two light-weight alternatives for simpler cases:
 - the [`dashArray` property of `L.Path`](http://leafletjs.com/reference-1.1.0.html#path-dasharray), if you only need to draw simple patterns (dashes, dots, etc.).
 - the [`Leaflet.TextPath`](https://github.com/makinacorpus/Leaflet.TextPath) plugin, which is based on SVG.

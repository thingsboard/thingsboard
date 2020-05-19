<p align="center">
<img alt="Split.js" title="Split.js" src="https://cdn.rawgit.com/nathancahill/Split.js/master/logo.svg" width="430">
<br><br>
<a href="https://travis-ci.org/nathancahill/Split.js"><img src="https://travis-ci.org/nathancahill/Split.js.svg" alt="Build Status"></a>
<a href="https://raw.githubusercontent.com/nathancahill/Split.js/master/split.min.js"><img src="https://badge-size.herokuapp.com/nathancahill/Split.js/master/split.min.js.svg?compression=gzip&label=size" alt="File Size"></a>
<img src="https://david-dm.org/nathancahill/Split.js/status.svg" alt="Dependencies">
</p>

# Split.js

> < 2kb unopinionated utility for resizeable split views. 

 - __Zero Deps__
 - __Tiny:__ Weights less than 2kb gzipped. 
 - __Fast:__ No overhead or attached window event listeners, uses pure CSS for resizing.
 - __Unopinionated:__ Plays nicely with `calc`, `flex` and `grid`.
 - __Compatible:__ Works great in IE9, and _even loads in IE8_ with polyfills. Early Firefox/Chrome/Safari/Opera supported too.

## Installation

Yarn:

```
$ yarn install --save split.js
```

npm:

```
$ npm install --save split.js
```

Bower:

```
$ bower install --save Split.js
```

Include with a module bundler like [rollup](http://rollupjs.org/) or [webpack](https://webpack.github.io/):

```js
// using ES6 modules
import Split from 'split.js'

// using CommonJS modules
var Split = require('split.js')
```

The [UMD](https://github.com/umdjs/umd) build is also available on [unpkg](http://unpkg.com/):

```html
<script src="https://unpkg.com/split.js/split.min.js"></script>
```

You can find the library on `window.Split`.

## Documentation

```js
var split = Split(<HTMLElement|selector[]> elements, <options> options?)
```

| Options | Type | Default | Description |
|---|---|---|---|
| sizes | Array | | Initial sizes of each element in percents or CSS values. |
| minSize | Number or Array | 100 | Minimum size of each element. |
| gutterSize | Number | 10 | Gutter size in pixels. |
| snapOffset | Number | 30 | Snap to minimum size offset in pixels. |
| direction | String | 'horizontal' | Direction to split: horizontal or vertical. |
| cursor | String | 'col-resize' | Cursor to display while dragging. |
| gutter | Function | | Called to create each gutter element |
| elementStyle | Function | | Called to set the style of each element. |
| gutterStyle | Function | | Called to set the style of the gutter. |
| onDrag | Function | | Callback on drag. |
| onDragStart | Function | | Callback on drag start. |
| onDragEnd | Function | | Callback on drag end. |

__Important Note__: Split.js does not set CSS beyond the minimum needed to manage the width and height of the elements.
This is by design. It makes Split.js flexible and useful in many different situations.
If you create a horizontal split, you are responsible for (likely) floating the elements and the gutter,
and setting their heights. See the [CSS](#css) section below.

## Options

#### sizes

An array of initial sizes of the elements, specified as percentage values. Example: Setting the initial sizes to 25% and 75%.

```js
Split(['#one', '#two'], {
    sizes: [25, 75]
})
```

#### minSize. Default: 100

An array of minimum sizes of the elements, specified as pixel values. Example: Setting the minimum sizes to 100px and 300px, respectively.

```js
Split(['#one', '#two'], {
    minSize: [100, 300]
})
```

If a number is passed instead of an array, all elements are set to the same minimum size:

```js
Split(['#one', '#two'], {
    minSize: 100
})
```

#### gutterSize. Default: 10

Gutter size in pixels. Example: Setting the gutter size to 20px.

```js
Split(['#one', '#two'], {
    gutterSize: 20
})
```

#### snapOffset. Default: 30

Snap to minimum size at this offset in pixels. Example: Set to 0 to disable to snap effect.

```js
Split(['#one', '#two'], {
    snapOffset: 0
})
```

#### direction. Default: 'horizontal'

Direction to split in. Can be 'vertical' or 'horizontal'. Determines which CSS properties are applied (ie. width/height) to each element and gutter. Example: split vertically:

```js
Split(['#one', '#two'], {
    direction: 'vertical'
})
```

#### cursor. Default: 'col-resize'

Cursor to show on the gutter (also applied to the two adjacent elements when dragging to prevent flickering). Defaults to 'col-resize', so should be switched to 'row-resize' when using direction: 'vertical':

```js
Split(['#one', '#two'], {
    direction: 'vertical',
    cursor: 'row-resize'
})
```

#### gutter

Optional function called to create each gutter element. The signature looks like this:

```js
(index, direction) => HTMLElement
```

Defaults to creating a `div` with `class="gutter gutter-horizontal"` or `class="gutter gutter-vertical"`, depending on the direction. The default gutter function looks like this:

```js
(index, direction) => {
    const gutter = document.createElement('div')
    gutter.className = `gutter gutter-${gutterDirection}`
    return gutter
}
```

The returned element is then inserted into the DOM, and it's width or height are set. This option can be used to clone an existing DOM element, or to create a new element with custom styles.

#### elementStyle

Optional function called setting the CSS style of the elements. The signature looks like this:

```js
(dimension, elementSize, gutterSize) => Object
```

Dimension will be a string, 'width' or 'height', and can be used in the return style. elementSize is the target percentage value of the element, and gutterSize is the target pixel value of the gutter.

It should return an object with CSS properties to apply to the element. For horizontal splits, the return object looks like this:

```js
{
    'width': 'calc(50% - 5px)'
}
```

A vertical split style would look like this:

```js
{
    'height': 'calc(50% - 5px)'
}
```

Use this function if you're using a different layout like flexbox or grid (see [Flexbox](#flexbox)). A flexbox style for a horizontal split would look like this:

```js
{
    'flex-basis': 'calc(50% - 5px)'
}
```

#### gutterStyle

Optional function called when setting the CSS style of the gutters. The signature looks like this:

```js
(dimension, gutterSize) => Object
```

Dimension is a string, either 'width' or 'height', and gutterSize is a pixel value representing the width of the gutter.

It should return a similar object as `elementStyle`, an object with CSS properties to apply to the gutter. Since gutters have fixed widths, it will generally look like this:

```js
{
    'width': '10px'
}
```

Both `elementStyle` and `gutterStyle` are called continously while dragging, so don't do anything besides return the style object in these functions.

#### onDrag, onDragStart, onDragEnd

Callbacks that can be added on drag (fired continously), drag start and drag end. If doing more than basic operations in `onDrag`, add a debounce function to rate limit the callback.

## Usage Examples

Reference HTML for examples. Gutters are inserted automatically:

```html
<div>
    <div id="one">content one</div>
    <div id="two">content two</div>
    <div id="three">content three</div>
</div>
```

A split with two elements, starting at 25% and 75% wide with 200px minimum width.

```js
Split(['#one', '#two'], {
    sizes: [25, 75],
    minSize: 200
});
```

A split with three elements, starting with even widths with 100px, 100px and 300px minimum widths, respectively.

```js
Split(['#one', '#two', '#three'], {
    minSize: [100, 100, 300]
});
```

A vertical split with two elements.

```js
Split(['#one', '#two'], {
    direction: 'vertical'
});
```

Specifying the initial widths with CSS values. Not recommended, the size/gutter calculations would have to be done before hand and won't scale on viewport resize.

```js
Split(['#one', '#two'], {
	sizes: ['200px', '500px']
});
```

JSFiddle style is also possible: [Demo](http://nathancahill.github.io/Split.js/examples/jsfiddle.html).

## Saving State

Use local storage to save the most recent state:

```js
var sizes = localStorage.getItem('split-sizes')

if (sizes) {
    sizes = JSON.parse(sizes)
} else {
    sizes = [50, 50]  // default sizes
}

var split = Split(['#one', '#two'], {
    sizes: sizes,
    onDragEnd: function () {
        localStorage.setItem('split-sizes', JSON.stringify(split.getSizes()));
    }
})
```

## Flexbox

Flexbox layout is supported by customizing the `elementStyle` and `gutterStyle` CSS. Given a layout like this:

```html
<div id="flex">
    <div id="flex-1"></div>
    <div id="flex-2"></div>
</div>
```

And CSS style like this:

```css
#flex {
    display: flex;
    flex-direction: row;
}
```

Then the `elementStyle` and `gutterStyle` can be used to set flex-basis:

```js
Split(['#flex-1', '#flex-2'], {
    elementStyle: function (dimension, size, gutterSize) {
        return {
            'flex-basis': 'calc(' + size + '% - ' + gutterSize + 'px)'
        }
    },
    gutterStyle: function (dimension, gutterSize) {
        return {
            'flex-basis':  gutterSize + 'px'
        }
    }
})
```

## API

Split.js returns an instance with a couple of functions. The instance is returned on creation:

```
var instance = Split([], ...)
```

#### .setSizes([])

setSizes behaves the same as the `sizes` configuration option, passing an array of percents or CSS values. It updates the sizes of the elements in the split. Added in v1.1.0:

```
instance.setSizes([25, 75])
```

#### .getSizes()

getSizes returns an array of percents, suitable for using with `setSizes` or creation. Not supported in IE8. Added in v1.1.2:

```
instance.getSizes()
> [25, 75]
```

#### .collapse(index)

collapse changes the size of element at `index` to 0. Every element except the last is collapsed towards the front (left or top). The last is collapsed towards the back. Not supported in IE8. Added in v1.1.0:

```
instance.collapse(0)
```

#### .destroy()

Destroy the instance. It removes the gutter elements, and the size CSS styles Split.js set. Added in v1.1.1.

```
instance.destroy()
```

## CSS

In being non-opionionated, the only CSS Split.js sets is the widths or heights of the elements. Everything else is left up to you. You must set the elements and gutter heights when using horizontal mode. The gutters will not be visible if their height is 0px. Here's some basic CSS to style the gutters with, although it's not required. Both grip images are included in this repo:

```css
.gutter {
    background-color: #eee;

    background-repeat: no-repeat;
    background-position: 50%;
}

.gutter.gutter-horizontal {
    background-image: url('grips/vertical.png');
    cursor: ew-resize;
}

.gutter.gutter-vertical {
    background-image: url('grips/horizontal.png');
    cursor: ns-resize;
}
```

The grip images are small files and can be included with base64 instead:

```css
.gutter.gutter-vertical {
    background-image:  url('data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAB4AAAAFAQMAAABo7865AAAABlBMVEVHcEzMzMzyAv2sAAAAAXRSTlMAQObYZgAAABBJREFUeF5jOAMEEAIEEFwAn3kMwcB6I2AAAAAASUVORK5CYII=')
}

.gutter.gutter-horizontal {
    background-image:  url('data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAUAAAAeCAYAAADkftS9AAAAIklEQVQoU2M4c+bMfxAGAgYYmwGrIIiDjrELjpo5aiZeMwF+yNnOs5KSvgAAAABJRU5ErkJggg==')
}
```

Split.js also works best when the elements are sized using `border-box`. The `split` class would have to be added manually to apply these styles:

```css
.split {
  -webkit-box-sizing: border-box;
     -moz-box-sizing: border-box;
          box-sizing: border-box;
}
```

And for horizontal splits, make sure the layout allows elements (including gutters) to be displayed side-by-side. Floating the elements is one option:

```css
.split, .gutter.gutter-horizontal {
    float: left;
}
```

If you use floats, set the height of the elements including the gutters. The gutters will not be visible otherwise if the height is set to 0px.

```css
.split, .gutter.gutter-horizontal {
    height: 300px;
}
```

Overflow can be handled as well, to get scrolling within the elements:

```css
.split {
    overflow-y: auto;
    overflow-x: hidden;
}
```

## Browser Support

This library uses [CSS calc()](https://developer.mozilla.org/en-US/docs/Web/CSS/calc#AutoCompatibilityTable), [CSS box-sizing](https://developer.mozilla.org/en-US/docs/Web/CSS/box-sizing#AutoCompatibilityTable) and [JS getBoundingClientRect()](https://developer.mozilla.org/en-US/docs/Web/API/Element/getBoundingClientRect#AutoCompatibilityTable). These features are supported in the following browsers:

| <img src="http://i.imgur.com/dJC1GUv.png" width="48px" height="48px" alt="Chrome logo"> | <img src="http://i.imgur.com/o1m5RcQ.png" width="48px" height="48px" alt="Firefox logo"> | <img src="http://i.imgur.com/8h3iz5H.png" width="48px" height="48px" alt="Internet Explorer logo"> | <img src="http://i.imgur.com/iQV4nmJ.png" width="48px" height="48px" alt="Opera logo"> | <img src="http://i.imgur.com/j3tgNKJ.png" width="48px" height="48px" alt="Safari logo"> | [<img src="http://i.imgur.com/70as3qf.png" height="48px" alt="BrowserStack logo">](http://browserstack.com/) |
|:---:|:---:|:---:|:---:|:---:|:----|
| 22+ ✔ | 6+ ✔ | 9+ ✔ | 15+ ✔ | 6.2+ ✔ | Sponsored ✔ |

Gracefully falls back in IE 8 and below to only setting the initial widths/heights and not allowing dragging. IE 8 requires polyfills for `Array.isArray()`, `Array.forEach`, `Array.map`, `Array.filter`, `Object.keys()` and `getComputedStyle`. This script from [Polyfill.io](https://polyfill.io/) includes all of these, adding 1.91 kb to the gzipped size.

This is __ONLY NEEDED__ if you are supporting __IE8:__

```
<script src="///polyfill.io/v2/polyfill.min.js?features=Array.isArray,Array.prototype.forEach,Array.prototype.map,Object.keys,Array.prototype.filter,getComputedStyle"></script>
```

This project's tests are run on multiple desktop and mobile browsers sponsored by [BrowserStack](http://browserstack.com/).

## License

Copyright (c) 2017 Nathan Cahill

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.

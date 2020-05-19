# AngularJS Touch Carousel

An AngularJS 1 carousel implementation optimised for mobile devices.

**This project is not maintained anymore, and looking for maintainers.**

Demo : http://revolunet.github.io/angular-carousel

Comments and contributions welcome :)

Proudly brought to you by the [@revolunet](http://twitter.com/revolunet) team.

## Install :

### Bower
`bower install angular-carousel`

### npm
`npm install angular-carousel`

If you don't use NPM or Bower, just download files [from the github repo](./dist)

## Usage :
 - Add `angular-touch.js` and `angular-carousel.js` to your code:
```html
<script src="angular.js"></script>
<script src="angular-touch.js"></script>
<script src="angular-carousel.js"></script>
```
 - Or just require the module the CommonJS way (if you use npm)
```js
var angular = require('angular');
var ngTouch = require('angular-touch');
var carousel  = require('angular-carousel');
```
 - Add `angular-carousel.css` to your code:
```html
<link href="angular-carousel.css" rel="stylesheet" type="text/css" />
```
 - Add a dependency to the `angular-carousel` module in your application.
```js
angular.module('MyApp', ['angular-carousel']);
```

 - Add a `rn-carousel` attribute to your `<ul>` block and your `<li>`'s become magically swipable ;)
```html
<ul rn-carousel class="image">
  <li ng-repeat="image in sportImages">
    <div class="layer">{{ image }}</div>
  </li>
</ul>
```

 - You can also use `rn-carousel` without ng-repeat ;)
```html
<ul rn-carousel class="image">
  <li>slide #1</li>
  <li>slide #2</li>
  <li>slide #3</li>
</ul>
```

## Directive options :
 - `rn-carousel-index` two way binding integer to control the carousel position (0-indexed)
 - `rn-rn-carousel-html-slides`: two way binding array to get slides if no ng-repeat was used (slides array)
 - `rn-carousel-buffered` add this attribute to enable the carousel buffering, good to minimize the DOM (5 slides)
 - `rn-carousel-controls` add this attribute to enable builtin prev/next buttons (you can override by CSS)
 - `rn-carousel-auto-slide` add this attribute to make the carousel slide automatically after given seconds (default=3)
 - `rn-carousel-transition` : transition type, can be one of `slide, zoom, hexagon, fadeAndSlide, none`. (default=slide)
 - `rn-carousel-locked`: two way binding boolean that lock/unlock the carousel
 - `rn-carousel-deep-watch`: Deep watch the collection which enable to dynamically add slides at beginning without corrupting position
 - `rn-carousel-easing`: add this attritube to specify a formula for easing, these can be found in the [shifty
 library](https://github.com/jeremyckahn/shifty/blob/master/src/shifty.formulas.js) (default=easeIn)
 - `rn-carousel-duration`: add this attribute to set the duration of the transition (default=300)
 - `rn-carousel-controls-allow-loop`: add this attribute to allow looping through slides from prev/next controls

## Indicators

You can add position indicators by adding this directive where you want :
```html
<div rn-carousel-indicators ng-if="slides.length > 1" slides="slides" rn-carousel-index="carouselIndex"></div>
```
 - `slides` is the same collection you use in the carousel ng-repeat

Note: If loading in slides from an async service, the `ng-if="slides.length > 1"` may not update as expected. In this case, try `ng-if="slides"`.

 - `carouselIndex` is the same index you've defined for the carousel

 Note: If the indicators don't seem to update with the slides, try binding to an object param i.e. `carousel.index`, set in the controller like `$scope.carousel.index = 0` [Explanation.](http://www.codelord.net/2014/05/10/understanding-angulars-magic-dont-bind-to-primitives/)

## Notes :
 - if you use IE<=9, iOS<7 or Android<4 please include the [requestAnimationFrame polyfill](https://github.com/darius/requestAnimationFrame/blob/master/requestAnimationFrame.js) in your application.
 - if you use IE<=8 include the [es5-shim polyfill](https://github.com/es-shims/es5-shim/blob/master/es5-shim.min.js) in your application.
 - don't set any style attribute to your li's. they would be overwritten by the carousel (use classes instead).
 - angular-carousel use the great [shifty.js](https://github.com/jeremyckahn/shifty) for the animations

## Todo :
 - delay autoslide on indicators click/move
 - customisable transitions
 - more transition types
 - infinite loop support

## Contributing
 - Please follow [AngularJS GIT conventions](https://docs.google.com/document/d/1QrDFcIiPjSLDn3EL15IJygNPiHORgU1_OOAqWjiDU5Y/edit#)
 - Please add tests
 - Please update the README and demo (index.html)

## Inspirations
 - https://github.com/ajoslin/angular-mobile-nav
 - http://mobile.smashingmagazine.com/2012/06/21/play-with-hardware-accelerated-css/
 - http://ariya.ofilabs.com/2013/08/javascript-kinetic-scrolling-part-1.html
 - Thanks to all angular folks for all the tips :)

## License
As AngularJS itself, this module is released under the permissive [MIT license](http://revolunet.mit-license.org). Your contributions are always welcome.

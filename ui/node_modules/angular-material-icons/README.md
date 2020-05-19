angular-material-icons
======================

AngularJS directive to use Material Design icons with custom fill-color and size.

This project encompasses all SVG icons from [Google's official Material Design Icon repository](https://github.com/google/material-design-icons) and few hand-picked icons from community-led [MaterialDesignIcons.com](http://materialdesignicons.com/) in form of angular directive that gives option to specify custom fill-color and size.

Changes to icon will go through delightful morphing if [SVG-Morpheus](https://github.com/alexk111/SVG-Morpheus) is also included in your application.

## Installation
This library is available via bower and npm
* `bower install angular-material-icons`
* `npm install angular-material-icons`

## Configure own icons
add many icons
```javascript
angular.module('moduleName').config(function (ngMdIconServiceProvider) {
    ngMdIconServiceProvider.addShapes({
        'signal_wifi_0_bar': '<path fill-opacity=".3" d="M12.01 21.49L23.64 7c-.45-.34-4.93-4-11.64-4C5.28 3 .81 6.66.36 7l11.63 14.49.01.01.01-.01z"/>',
        'signal_wifi_1_bar': '<path fill-opacity=".3" d="M12.01 21.49L23.64 7c-.45-.34-4.93-4-11.64-4C5.28 3 .81 6.66.36 7l11.63 14.49.01.01.01-.01z"/><path d="M6.67 14.86L12 21.49v.01l.01-.01 5.33-6.63C17.06 14.65 15.03 13 12 13s-5.06 1.65-5.33 1.86z"/>',
    });
});
```

optionally You can add viewBox for icon if not want to do it in templates
 * attribute view-box from template has higher priority so if given will override configured by service
 * if not given viewBox in config and template will use default "0 0 24 24"
```javascript
angular.module('moduleName').config(function (ngMdIconServiceProvider) {
    ngMdIconServiceProvider
        .addShape('evCross', '<polygon points="612,36.004 576.521,0.603 306,270.608 35.478,0.603 0,36.004 270.522,306.011 0,575.997 35.478,611.397 306,341.411 576.521,611.397 612,575.997 341.459,306.011"/>')
        .addViewBox('evCross', '0 0 612 612');
});
```

## Demo

Checkout the [live demo](https://klarsys.github.io/angular-material-icons/) for usage instructions and a preview of all icons included in the set.

## Licenses

This package is released under [MIT license](https://raw.githubusercontent.com/klarsys/angular-material-icons/master/LICENSE).

Icons are released under [Attribution 4.0 International](http://creativecommons.org/licenses/by/4.0/) license.
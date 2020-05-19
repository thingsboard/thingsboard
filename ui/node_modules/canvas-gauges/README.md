# HTML Canvas Gauges v2.1

[![Build Status](https://travis-ci.org/Mikhus/canvas-gauges.svg?branch=master)](https://travis-ci.org/Mikhus/canvas-gauges) ![Test Coverage](https://rawgit.com/Mikhus/canvas-gauges/master/test-coverage.svg) ![Documentation Coverage](https://rawgit.com/Mikhus/canvas-gauges/master/docs-coverage.svg) [![License](https://img.shields.io/badge/license-MIT-blue.svg)](https://rawgit.com/Mikhus/canvas-gauges/master/LICENSE)

[![Canvas Gauges](https://raw.githubusercontent.com/Mikhus/blob/master/gauges.png)](https://rawgit.com/Mikhus/canvas-gauges/master/examples/radial-component.html)

<!-- toc -->

- [Installation](#installation)
- [Documentation](#documentation)
- [Add-Ons](#add-ons)
- [Special Thanks](#special-thanks)
- [License](#license)

<!-- tocstop -->

This is tiny implementation of highly configurable gauge using pure JavaScript and HTML5 canvas.
No dependencies. Suitable for IoT devices because of minimum code base.

## Installation

Canvas gauges can be simply installed using npm package manager. Depending on your needs there is possibility to install whole gauge library or only that part you really need for your project.
To install the whole library, run:

    $ npm install canvas-gauges

If you only need the exact type of the gauge it can be installed using the appropriate npm tag. Currently the following gauges are supported: linear, radial.

To install only linear gauge, run:

    $ npm install canvas-gauges@linear

To install only radial gauge, run:

    $ npm install canvas-gauges@radial

This strategy useful only if you need to minimize your code base and plan to use ONLY a specific gauge type. If you need to use various gauge types in your project it is recommended to use whole gauge package.

[More...](http://canvas-gauges.com/documentation/user-guide/#installing)

## Documentation

You can find complete docs on the canvas gauges web-site:

 * [User Guide](http://canvas-gauges.com/documentation/user-guide/)
 * [Developer's Docs](http://canvas-gauges.com/documentation/api/)
 * [Examples](http://canvas-gauges.com/documentation/examples/)

## Add-Ons

Here are some third-party libraries which are developed and delivered to use canvas-gauges as components for a different popular frameworks:

 - [Angular gauge components](https://github.com/MeetmeLeave/ng-canvas-gauges)
 - [VueJs gauge components](https://github.com/vue-bulma/canvas-gauges)
 - [React gauge component](https://github.com/1995parham/react-canvas-gauges)

## Special Thanks

[![Lohika](http://www.lohika.com/wp-content/themes/gridalicious/images/lohika_full.svg)](http://www.lohika.com/)

For supporting development!

[![SauceLabs](http://info.saucelabs.com/rs/468-XBT-687/images/ink-logo.png)](http://saucelabs.com/)

For testing support!

And to all [contributors](https://github.com/Mikhus/canvas-gauges/graphs/contributors)!

## License

This code is subject to [MIT](https://rawgit.com/Mikhus/canvas-gauges/master/LICENSE) license.

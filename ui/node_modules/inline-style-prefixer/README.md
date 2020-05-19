# inline-style-prefixer

**inline-style-prefixer** adds required **vendor prefixes** to your style object. It only adds prefixes if they're actually required by evaluating the browser's `userAgent` against data from [caniuse.com](http://caniuse.com/).
<br>

Alternatively it ships a static version that adds all available vendor prefixes.

[![Build Status](https://travis-ci.org/rofrischmann/inline-style-prefixer.svg)](https://travis-ci.org/rofrischmann/inline-style-prefixer)
[![Test Coverage](https://codeclimate.com/github/rofrischmann/inline-style-prefixer/badges/coverage.svg)](https://codeclimate.com/github/rofrischmann/inline-style-prefixer/coverage)
[![npm downloads](https://img.shields.io/npm/dm/inline-style-prefixer.svg)](https://img.shields.io/npm/dm/inline-style-prefixer.svg)
![Dependencies](https://david-dm.org/rofrischmann/inline-style-prefixer.svg)
![Gzipped Size](https://img.shields.io/badge/gzipped-8.50kb-brightgreen.svg)

## Installation
```sh
npm i --save inline-style-prefixer
```
Assuming you are using [npm](https://www.npmjs.com) as your package mananger you can `npm install` all packages. <br>
Otherwise we also provide [UMD](https://github.com/umdjs/umd) builds for each package within the `dist` folder. You can easily use them via [unpkg](https://unpkg.com/).
```HTML
<!-- Unminified versions -->
<script src="https://unpkg.com/inline-style-prefixer@2.0.4/dist/inline-style-prefixer.js"></script>
<script src="https://unpkg.com/inline-style-prefixer@2.0.4/dist/inline-style-prefix-all.js"></script>
<!-- Minified versions -->
<script src="https://unpkg.com/inline-style-prefixer@2.0.4/dist/inline-style-prefixer.min.js"></script>
<script src="https://unpkg.com/inline-style-prefixer@2.0.4/dist/inline-style-prefix-all.min.js"></script>
```

# Browser Support
Supports the major browsers with the following versions. <br>For legacy support check [custom build](#custom-build--legacy-support). We do not officially support any other browsers.<br>
It will **only** add prefixes if a property still needs them in one of the following browser versions.This means *e.g. `border-radius`* will not be prefixed at all.

* Chrome: 30+
* Safari: 6+
* Firefox: 25+
* Opera: 13+
* IE: 9+
* Edge 12+
* iOS: 6+
* Android: 4+
* IE mobile: 9+
* Opera mini: 5+
* Android UC: 9+
* Android Chrome: 30+

### Fallback
If using an unsupported browser or even run without any `userAgent`, it will use [`inline-style-prefixer/static`](docs/API.md#pro-tip) as a fallback.


## Example
```javascript
import Prefixer from 'inline-style-prefixer'

const styles = {
  transition: '200ms all linear',
  userSelect: 'none',
  boxSizing: 'border-box',
  display: 'flex',
  color: 'blue'
}

const prefixer = new Prefixer({ userAgent: 'Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.2 (KHTML, like Gecko) Chrome/25.0.1216.0 Safari/537.2'})
const prefixedStyles = prefixer.prefix(styles)

// prefixedStyles === output
const output = {
  transition: '200ms all linear',
  WebkitUserSelect: 'none',
  boxSizing: 'border-box',
  display: '-webkit-flex',
  color: 'blue'
}
```
`inline-style-prefixer/static`

![Gzipped Size](https://img.shields.io/badge/gzipped-2.40kb-brightgreen.svg)

If you only want to use the static version, you can import it directly to reduce file size. It was once shipped as a several package [inline-style-prefix-all](https://github.com/rofrischmann/inline-style-prefix-all).
```javascript
import prefixAll from 'inline-style-prefixer/static'

const styles = {
  transition: '200ms all linear',
  boxSizing: 'border-box',
  display: 'flex',
  color: 'blue'
}

const prefixedStyles = prefixAll(styles)

// prefixedStyles === output
const output = {
  WebkitTransition: '200ms all linear',
  // Firefox dropped prefixed transition with version 16
  // IE never supported prefixed transitions
  transition: '200ms all linear',
  MozBoxSizing: 'border-box',
  // Firefox up to version 28 needs a prefix
  // Others dropped prefixes out of scope
  boxSizing: 'border-box',
  // Fallback/prefixed values get grouped in arrays
  // The prefixer does not resolve those
  display: [ '-webkit-box', '-moz-box', '-ms-flexbox', '-webkit-flex', 'flex' ]
  color: 'blue'
}
```

## Documentation
If you got any issue using this prefixer, please first check the FAQ's. Most cases are already covered and provide a solid solution.

* [API Reference](docs/API.md)
* [Supported Properties](docs/Properties.md)
* [Special Plugins](docs/Plugins.md)
* [FAQ](docs/FAQ.md)

# Custom Build & Legacy Support
You may have to create a custom build if you need older browser versions. Just modify the [config.js](config.js) file which includes all the browser version specifications.
```sh
npm install
npm run build
```

# License
**inline-style-prefixer** is licensed under the [MIT License](http://opensource.org/licenses/MIT).<br>
Documentation is licensed under [Creative Common License](http://creativecommons.org/licenses/by/4.0/).<br>
Created with ♥ by [@rofrischmann](http://rofrischmann.de).

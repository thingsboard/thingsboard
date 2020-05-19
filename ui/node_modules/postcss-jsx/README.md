PostCSS JSX Syntax
====

[![NPM version](https://img.shields.io/npm/v/postcss-jsx.svg?style=flat-square)](https://www.npmjs.com/package/postcss-jsx)
[![Travis](https://img.shields.io/travis/gucong3000/postcss-jsx.svg)](https://travis-ci.org/gucong3000/postcss-jsx)
[![Travis](https://img.shields.io/travis/gucong3000/postcss-syntaxes.svg?label=integration)](https://travis-ci.org/gucong3000/postcss-syntaxes)
[![Codecov](https://img.shields.io/codecov/c/github/gucong3000/postcss-jsx.svg)](https://codecov.io/gh/gucong3000/postcss-jsx)
[![David](https://img.shields.io/david/gucong3000/postcss-jsx.svg)](https://david-dm.org/gucong3000/postcss-jsx)

<img align="right" width="95" height="95"
	title="Philosopher’s stone, logo of PostCSS"
	src="http://postcss.github.io/postcss/logo.svg">

[PostCSS](https://github.com/postcss/postcss) syntax for parsing [CSS in JS](https://github.com/MicheleBertoli/css-in-js) literals:

- [aphrodite](https://github.com/Khan/aphrodite)
- [astroturf](https://github.com/4Catalyzer/astroturf)
- [csjs](https://github.com/rtsao/csjs)
- [css-light](https://github.com/streamich/css-light)
- [cssobj](https://github.com/cssobj/cssobj)
- [electron-css](https://github.com/azukaar/electron-css)
- [emotion](https://github.com/emotion-js/emotion)
- [freestyler](https://github.com/streamich/freestyler)
- [glamor](https://github.com/threepointone/glamor)
- [glamorous](https://github.com/paypal/glamorous)
- [j2c](https://github.com/j2css/j2c)
- [linaria](https://github.com/callstack/linaria)
- [lit-css](https://github.com/bashmish/lit-css)
- [react-native](https://github.com/necolas/react-native-web)
- [react-style](https://github.com/js-next/react-style)
- [reactcss](https://github.com/casesandberg/reactcss)
- [styled-components](https://github.com/styled-components/styled-components)
- [styletron-react](https://github.com/rtsao/styletron)
- [styling](https://github.com/andreypopp/styling)
- [typestyle](https://github.com/typestyle/typestyle)

## Getting Started

First thing's first, install the module:

```
npm install postcss-syntax postcss-jsx --save-dev
```

## Use Cases

```js
const postcss = require('postcss');
const stylelint = require('stylelint');
const syntax = require('postcss-syntax');
postcss([stylelint({ fix: true })]).process(source, { syntax: syntax }).then(function (result) {
	// An alias for the result.css property. Use it with syntaxes that generate non-CSS output.
	result.content
});
```

input:
```javascript
import glm from 'glamorous';
const Component1 = glm.a({
	flexDirectionn: 'row',
	display: 'inline-block',
	color: '#fff',
});
```

output:
```javascript
import glm from 'glamorous';
const Component1 = glm.a({
	color: '#fff',
	display: 'inline-block',
	flexDirectionn: 'row',
});
```

## Advanced Use Cases

Add support for more `css-in-js` package:
```js
const syntax = require('postcss-syntax')({
	"i-css": (index, namespace) => namespace[index + 1] === "addStyles",
	"styled-components": true,
});
```

See: [postcss-syntax](https://github.com/gucong3000/postcss-syntax)

## Style Transformations

The main use case of this plugin is to apply PostCSS transformations to CSS code in template literals & styles as object literals.

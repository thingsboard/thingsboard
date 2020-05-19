# import/no-webpack-loader-syntax

Forbid Webpack loader syntax in imports.

[Webpack](http://webpack.github.io) allows specifying the [loaders](http://webpack.github.io/docs/loaders.html) to use in the import source string using a special syntax like this:
```js
var moduleWithOneLoader = require("my-loader!./my-awesome-module");
```

This syntax is non-standard, so it couples the code to Webpack. The recommended way to specify Webpack loader configuration is in a [Webpack configuration file](http://webpack.github.io/docs/loaders.html#loaders-by-config).

## Rule Details

### Fail

```js
import myModule from 'my-loader!my-module';
import theme from 'style!css!./theme.css';

var myModule = require('my-loader!./my-module');
var theme = require('style!css!./theme.css');
```

### Pass

```js
import myModule from 'my-module';
import theme from './theme.css';

var myModule = require('my-module');
var theme = require('./theme.css');
```

## When Not To Use It

If you have a project that doesn't use Webpack you can safely disable this rule.

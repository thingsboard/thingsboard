<div align="center">
  <a href="https://github.com/webpack/webpack">
    <img width="200" height="200" src="https://webpack.js.org/assets/icon-square-big.svg">
  </a>
</div>

[![npm][npm]][npm-url]
[![node][node]][node-url]
[![deps][deps]][deps-url]
[![tests][tests]][tests-url]
[![cover][cover]][cover-url]
[![chat][chat]][chat-url]
[![size][size]][size-url]

# UglifyJS Webpack Plugin

This plugin uses [uglify-js](https://github.com/mishoo/UglifyJS2) to minify your JavaScript.

## Requirements

This module requires a minimum of Node v6.9.0 and Webpack v4.0.0.

## Getting Started

To begin, you'll need to install `uglifyjs-webpack-plugin`:

```console
$ npm install uglifyjs-webpack-plugin --save-dev
```

Then add the plugin to your `webpack` config. For example:

**webpack.config.js**

```js
const UglifyJsPlugin = require('uglifyjs-webpack-plugin');

module.exports = {
  optimization: {
    minimizer: [new UglifyJsPlugin()],
  },
};
```

And run `webpack` via your preferred method.

## Options

### `test`

Type: `String|RegExp|Array<String|RegExp>`
Default: `/\.js(\?.*)?$/i`

Test to match files against.

**webpack.config.js**

```js
module.exports = {
  optimization: {
    minimizer: [
      new UglifyJsPlugin({
        test: /\.js(\?.*)?$/i,
      }),
    ],
  },
};
```

### `include`

Type: `String|RegExp|Array<String|RegExp>`
Default: `undefined`

Files to include.

**webpack.config.js**

```js
module.exports = {
  optimization: {
    minimizer: [
      new UglifyJsPlugin({
        include: /\/includes/,
      }),
    ],
  },
};
```

### `exclude`

Type: `String|RegExp|Array<String|RegExp>`
Default: `undefined`

Files to exclude.

**webpack.config.js**

```js
module.exports = {
  optimization: {
    minimizer: [
      new UglifyJsPlugin({
        exclude: /\/excludes/,
      }),
    ],
  },
};
```

### `chunkFilter`

Type: `Function<(chunk) -> boolean>`
Default: `() => true`

Allowing to filter which chunks should be uglified (by default all chunks are uglified).
Return `true` to uglify the chunk, `false` otherwise.

**webpack.config.js**

```js
module.exports = {
  optimization: {
    minimizer: [
      new UglifyJsPlugin({
        chunkFilter: (chunk) => {
          // Exclude uglification for the `vendor` chunk
          if (chunk.name === 'vendor') {
            return false;
          }

          return true;
        },
      }),
    ],
  },
};
```

### `cache`

Type: `Boolean|String`
Default: `false`

Enable file caching.
Default path to cache directory: `node_modules/.cache/uglifyjs-webpack-plugin`.

> ℹ️ If you use your own `minify` function please read the `minify` section for cache invalidation correctly.

#### `Boolean`

Enable/disable file caching.

**webpack.config.js**

```js
module.exports = {
  optimization: {
    minimizer: [
      new UglifyJsPlugin({
        cache: true,
      }),
    ],
  },
};
```

#### `String`

Enable file caching and set path to cache directory.

**webpack.config.js**

**webpack.config.js**

```js
module.exports = {
  optimization: {
    minimizer: [
      new UglifyJsPlugin({
        cache: 'path/to/cache',
      }),
    ],
  },
};
```

### `cacheKeys`

Type: `Function<(defaultCacheKeys, file) -> Object>`
Default: `defaultCacheKeys => defaultCacheKeys`

Allows you to override default cache keys.

Default cache keys:

```js
({
  'uglify-js': require('uglify-js/package.json').version, // uglify version
  'uglifyjs-webpack-plugin': require('../package.json').version, // plugin version
  'uglifyjs-webpack-plugin-options': this.options, // plugin options
  path: compiler.outputPath ? `${compiler.outputPath}/${file}` : file, // asset path
  hash: crypto
    .createHash('md4')
    .update(input)
    .digest('hex'), // source file hash
});
```

**webpack.config.js**

```js
module.exports = {
  optimization: {
    minimizer: [
      new UglifyJsPlugin({
        cache: true,
        cacheKeys: (defaultCacheKeys, file) => {
          defaultCacheKeys.myCacheKey = 'myCacheKeyValue';

          return defaultCacheKeys;
        },
      }),
    ],
  },
};
```

### `parallel`

Type: `Boolean|Number`
Default: `false`

Use multi-process parallel running to improve the build speed.
Default number of concurrent runs: `os.cpus().length - 1`.

> ℹ️ Parallelization can speedup your build significantly and is therefore **highly recommended**.

#### `Boolean`

Enable/disable multi-process parallel running.

**webpack.config.js**

```js
module.exports = {
  optimization: {
    minimizer: [
      new UglifyJsPlugin({
        parallel: true,
      }),
    ],
  },
};
```

#### `Number`

Enable multi-process parallel running and set number of concurrent runs.

**webpack.config.js**

```js
module.exports = {
  optimization: {
    minimizer: [
      new UglifyJsPlugin({
        parallel: 4,
      }),
    ],
  },
};
```

### `sourceMap`

Type: `Boolean`
Default: `false`

Use source maps to map error message locations to modules (this slows down the compilation).
If you use your own `minify` function please read the `minify` section for handling source maps correctly.

> ⚠️ **`cheap-source-map` options don't work with this plugin**.

**webpack.config.js**

```js
module.exports = {
  optimization: {
    minimizer: [
      new UglifyJsPlugin({
        sourceMap: true,
      }),
    ],
  },
};
```

### `minify`

Type: `Function`
Default: `undefined`

Allows you to override default minify function.
By default plugin uses [uglify-js](https://github.com/mishoo/UglifyJS2) package.
Useful for using and testing unpublished versions or forks.

> ⚠️ **Always use `require` inside `minify` function when `parallel` option enabled**.

**webpack.config.js**

```js
module.exports = {
  optimization: {
    minimizer: [
      new UglifyJsPlugin({
        minify(file, sourceMap) {
          const extractedComments = [];

          // Custom logic for extract comments

          const { error, map, code, warnings } = require('uglify-module') // Or require('./path/to/uglify-module')
            .minify(file, {
              /* Your options for minification */
            });

          return { error, map, code, warnings, extractedComments };
        },
      }),
    ],
  },
};
```

### `uglifyOptions`

Type: `Object`
Default: [default](https://github.com/mishoo/UglifyJS2#minify-options)

UglifyJS minify [options](https://github.com/mishoo/UglifyJS2#minify-options).

**webpack.config.js**

```js
module.exports = {
  optimization: {
    minimizer: [
      new UglifyJsPlugin({
        uglifyOptions: {
          warnings: false,
          parse: {},
          compress: {},
          mangle: true, // Note `mangle.properties` is `false` by default.
          output: null,
          toplevel: false,
          nameCache: null,
          ie8: false,
          keep_fnames: false,
        },
      }),
    ],
  },
};
```

### `extractComments`

Type: `Boolean|String|RegExp|Function<(node, comment) -> Boolean|Object>`
Default: `false`

Whether comments shall be extracted to a separate file, (see [details](https://github.com/webpack/webpack/commit/71933e979e51c533b432658d5e37917f9e71595a)).
By default extract only comments using `/^\**!|@preserve|@license|@cc_on/i` regexp condition and remove remaining comments.
If the original file is named `foo.js`, then the comments will be stored to `foo.js.LICENSE`.
The `uglifyOptions.output.comments` option specifies whether the comment will be preserved, i.e. it is possible to preserve some comments (e.g. annotations) while extracting others or even preserving comments that have been extracted.

#### `Boolean`

Enable/disable extracting comments.

**webpack.config.js**

```js
module.exports = {
  optimization: {
    minimizer: [
      new UglifyJsPlugin({
        extractComments: true,
      }),
    ],
  },
};
```

#### `String`

Extract `all` or `some` (use `/^\**!|@preserve|@license|@cc_on/i` RegExp) comments.

**webpack.config.js**

```js
module.exports = {
  optimization: {
    minimizer: [
      new UglifyJsPlugin({
        extractComments: 'all',
      }),
    ],
  },
};
```

#### `RegExp`

All comments that match the given expression will be extracted to the separate file.

**webpack.config.js**

```js
module.exports = {
  optimization: {
    minimizer: [
      new UglifyJsPlugin({
        extractComments: /@extract/i,
      }),
    ],
  },
};
```

#### `Function<(node, comment) -> Boolean>`

All comments that match the given expression will be extracted to the separate file.

**webpack.config.js**

```js
module.exports = {
  optimization: {
    minimizer: [
      new UglifyJsPlugin({
        extractComments: function(astNode, comment) {
          if (/@extract/i.test(comment.value)) {
            return true;
          }

          return false;
        },
      }),
    ],
  },
};
```

#### `Object`

Allow to customize condition for extract comments, specify extracted file name and banner.

**webpack.config.js**

```js
module.exports = {
  optimization: {
    minimizer: [
      new UglifyJsPlugin({
        extractComments: {
          condition: /^\**!|@preserve|@license|@cc_on/i,
          filename(file) {
            return `${file}.LICENSE`;
          },
          banner(licenseFile) {
            return `License information can be found in ${licenseFile}`;
          },
        },
      }),
    ],
  },
};
```

##### `condition`

Type: `Boolean|String|RegExp|Function<(node, comment) -> Boolean|Object>`

Condition what comments you need extract.

**webpack.config.js**

```js
module.exports = {
  optimization: {
    minimizer: [
      new UglifyJsPlugin({
        extractComments: {
          condition: 'some',
          filename(file) {
            return `${file}.LICENSE`;
          },
          banner(licenseFile) {
            return `License information can be found in ${licenseFile}`;
          },
        },
      }),
    ],
  },
};
```

##### `filename`

Type: `Regex|Function<(string) -> String>`
Default: `${file}.LICENSE`

The file where the extracted comments will be stored.
Default is to append the suffix `.LICENSE` to the original filename.

**webpack.config.js**

```js
module.exports = {
  optimization: {
    minimizer: [
      new UglifyJsPlugin({
        extractComments: {
          condition: /^\**!|@preserve|@license|@cc_on/i,
          filename: 'extracted-comments.js',
          banner(licenseFile) {
            return `License information can be found in ${licenseFile}`;
          },
        },
      }),
    ],
  },
};
```

##### `banner`

Type: `Boolean|String|Function<(string) -> String>`
Default: `/*! For license information please see ${commentsFile} */`

The banner text that points to the extracted file and will be added on top of the original file.
Can be `false` (no banner), a `String`, or a `Function<(string) -> String>` that will be called with the filename where extracted comments have been stored.
Will be wrapped into comment.

**webpack.config.js**

```js
module.exports = {
  optimization: {
    minimizer: [
      new UglifyJsPlugin({
        extractComments: {
          condition: true,
          filename(file) {
            return `${file}.LICENSE`;
          },
          banner(commentsFile) {
            return `My custom banner about license information ${commentsFile}`;
          },
        },
      }),
    ],
  },
};
```

### `warningsFilter`

Type: `Function<(warning, source) -> Boolean>`
Default: `() => true`

Allow to filter [uglify-js](https://github.com/mishoo/UglifyJS2) warnings.
Return `true` to keep the warning, `false` otherwise.

**webpack.config.js**

```js
module.exports = {
  optimization: {
    minimizer: [
      new UglifyJsPlugin({
        warningsFilter: (warning, source) => {
          if (/Dropping unreachable code/i.test(warning)) {
            return true;
          }

          if (/filename\.js/i.test(source)) {
            return true;
          }

          return false;
        },
      }),
    ],
  },
};
```

## Examples

### Cache And Parallel

Enable cache and multi-process parallel running.

**webpack.config.js**

```js
module.exports = {
  optimization: {
    minimizer: [
      new UglifyJsPlugin({
        cache: true,
        parallel: true,
      }),
    ],
  },
};
```

### Preserve Comments

Extract all legal comments (i.e. `/^\**!|@preserve|@license|@cc_on/i`) and preserve `/@license/i` comments.

**webpack.config.js**

```js
module.exports = {
  optimization: {
    minimizer: [
      new UglifyJsPlugin({
        uglifyOptions: {
          output: {
            comments: /@license/i,
          },
        },
        extractComments: true,
      }),
    ],
  },
};
```

### Remove Comments

If you avoid building with comments, set **uglifyOptions.output.comments** to **false** as in this config:

**webpack.config.js**

```js
module.exports = {
  optimization: {
    minimizer: [
      new UglifyJsPlugin({
        uglifyOptions: {
          output: {
            comments: false,
          },
        },
      }),
    ],
  },
};
```

### Custom Minify Function

Override default minify function - use [terser](https://github.com/fabiosantoscode/terser) for minification.

**webpack.config.js**

```js
module.exports = {
  optimization: {
    minimizer: [
      new UglifyJsPlugin({
        // Uncomment lines below for cache invalidation correctly
        // cache: true,
        // cacheKeys(defaultCacheKeys) {
        //   delete defaultCacheKeys['uglify-js'];
        //
        //   return Object.assign(
        //     {},
        //     defaultCacheKeys,
        //     { 'uglify-js': require('uglify-js/package.json').version },
        //   );
        // },
        minify(file, sourceMap) {
          // https://github.com/mishoo/UglifyJS2#minify-options
          const uglifyJsOptions = {
            /* your `uglify-js` package options */
          };

          if (sourceMap) {
            uglifyJsOptions.sourceMap = {
              content: sourceMap,
            };
          }

          return require('terser').minify(file, uglifyJsOptions);
        },
      }),
    ],
  },
};
```

## Contributing

Please take a moment to read our contributing guidelines if you haven't yet done so.

[CONTRIBUTING](./.github/CONTRIBUTING.md)

## License

[MIT](./LICENSE)

[npm]: https://img.shields.io/npm/v/uglifyjs-webpack-plugin.svg
[npm-url]: https://npmjs.com/package/uglifyjs-webpack-plugin
[node]: https://img.shields.io/node/v/uglifyjs-webpack-plugin.svg
[node-url]: https://nodejs.org
[deps]: https://david-dm.org/webpack-contrib/uglifyjs-webpack-plugin.svg
[deps-url]: https://david-dm.org/webpack-contrib/uglifyjs-webpack-plugin
[tests]: https://dev.azure.com/webpack-contrib/uglifyjs-webpack-plugin/_apis/build/status/webpack-contrib.uglifyjs-webpack-plugin?branchName=master
[tests-url]: https://dev.azure.com/webpack-contrib/uglifyjs-webpack-plugin/_build/latest?definitionId=8&branchName=master
[cover]: https://codecov.io/gh/webpack-contrib/uglifyjs-webpack-plugin/branch/master/graph/badge.svg
[cover-url]: https://codecov.io/gh/webpack-contrib/uglifyjs-webpack-plugin
[chat]: https://img.shields.io/badge/gitter-webpack%2Fwebpack-brightgreen.svg
[chat-url]: https://gitter.im/webpack/webpack
[size]: https://packagephobia.now.sh/badge?p=uglifyjs-webpack-plugin
[size-url]: https://packagephobia.now.sh/result?p=uglifyjs-webpack-plugin

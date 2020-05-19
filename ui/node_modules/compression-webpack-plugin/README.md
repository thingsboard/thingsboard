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

# compression-webpack-plugin

Prepare compressed versions of assets to serve them with Content-Encoding.

## Requirements

This module requires a minimum of Node v6.9.0 and Webpack v4.0.0.

## Getting Started

To begin, you'll need to install `compression-webpack-plugin`:

```console
$ npm install compression-webpack-plugin --save-dev
```

Then add the plugin to your `webpack` config. For example:

**webpack.config.js**

```js
const CompressionPlugin = require('compression-webpack-plugin');

module.exports = {
  plugins: [new CompressionPlugin()],
};
```

And run `webpack` via your preferred method.

## Options

### `test`

Type: `String|RegExp|Array<String|RegExp>`
Default: `undefined`

Test to match files against.

```js
// in your webpack.config.js
new CompressionPlugin({
  test: /\.js(\?.*)?$/i,
});
```

### `include`

Type: `String|RegExp|Array<String|RegExp>`
Default: `undefined`

Files to include.

```js
// in your webpack.config.js
new CompressionPlugin({
  include: /\/includes/,
});
```

### `exclude`

Type: `String|RegExp|Array<String|RegExp>`
Default: `undefined`

Files to exclude.

```js
// in your webpack.config.js
new CompressionPlugin({
  exclude: /\/excludes/,
});
```

### `cache`

Type: `Boolean|String`
Default: `false`

Enable file caching.
The default path to cache directory: `node_modules/.cache/compression-webpack-plugin`.

#### `Boolean`

Enable/disable file caching.

```js
// in your webpack.config.js
new CompressionPlugin({
  cache: true,
});
```

#### `String`

Enable file caching and set path to cache directory.

```js
// in your webpack.config.js
new CompressionPlugin({
  cache: 'path/to/cache',
});
```

### `filename`

Type: `String|Function`
Default: `[path].gz[query]`

The target asset filename.

#### `String`

`[file]` is replaced with the original asset filename.
`[path]` is replaced with the path of the original asset.
`[dir]` is replaced with the directory of the original asset.
`[name]` is replaced with the filename of the original asset.
`[ext]` is replaced with the extension of the original asset.
`[query]` is replaced with the query.

```js
// in your webpack.config.js
new CompressionPlugin({
  filename: '[path].gz[query]',
});

new CompressionPlugin({
  filename: '[dir][name].gz[ext][query]',
});
```

#### `Function`

```js
// in your webpack.config.js
new CompressionPlugin({
  filename(info) {
    // info.file is the original asset filename
    // info.path is the path of the original asset
    // info.query is the query
    return `${info.path}.gz${info.query}`;
  },
});
```

### `algorithm`

Type: `String|Function`
Default: `gzip`

The compression algorithm/function.

#### `String`

The algorithm is taken from [zlib](https://nodejs.org/api/zlib.html).

```js
// in your webpack.config.js
new CompressionPlugin({
  algorithm: 'gzip',
});
```

#### `Function`

Allow to specify a custom compression function.

```js
// in your webpack.config.js
new CompressionPlugin({
  algorithm(input, compressionOptions, callback) {
    return compressionFunction(input, compressionOptions, callback);
  },
});
```

### `compressionOptions`

Type: `Object`
Default: `{ level: 9 }`

If you use custom function for the `algorithm` option, the default value is `{}`.

Compression options.
You can find all options here [zlib](https://nodejs.org/api/zlib.html#zlib_class_options).

```js
// in your webpack.config.js
new CompressionPlugin({
  compressionOptions: { level: 1 },
});
```

### `threshold`

Type: `Number`
Default: `0`

Only assets bigger than this size are processed. In bytes.

```js
// in your webpack.config.js
new CompressionPlugin({
  threshold: 8192,
});
```

### `minRatio`

Type: `Number`
Default: `0.8`

Only assets that compress better than this ratio are processed (`minRatio = Compressed Size / Original Size`).
Example: you have `image.png` file with 1024b size, compressed version of file has 768b size, so `minRatio` equal `0.75`.
In other words assets will be processed when the `Compressed Size / Original Size` value less `minRatio` value.
You can use `1` value to process assets that are smaller than the original. Use a value of Number.MAX_SAFE_INTEGER to process all assets even if they are larger than the original (useful when you are pre-zipping all assets for AWS)

```js
// in your webpack.config.js
new CompressionPlugin({
  minRatio: 0.8,
});
```

### `deleteOriginalAssets`

Type: `Boolean`
Default: `false`

Whether to delete the original assets or not.

```js
// in your webpack.config.js
new CompressionPlugin({
  deleteOriginalAssets: true,
});
```

## Examples

### Using Zopfli

Prepare compressed versions of assets using `zopfli` library.

> ℹ️ `@gfx/zopfli` require minimum `8` version of `node`.

To begin, you'll need to install `@gfx/zopfli`:

```console
$ npm install @gfx/zopfli --save-dev
```

**webpack.config.js**

```js
const zopfli = require('@gfx/zopfli');

module.exports = {
  plugins: [
    new CompressionPlugin({
      compressionOptions: {
        numiterations: 15,
      },
      algorithm(input, compressionOptions, callback) {
        return zopfli.gzip(input, compressionOptions, callback);
      },
    }),
  ],
};
```

### Using Brotli

[Brotli](https://en.wikipedia.org/wiki/Brotli) is a compression algorithm originally developed by Google, and offers compression superior to gzip.

Node 11.7.0 and later has [native support](https://nodejs.org/api/zlib.html#zlib_zlib_createbrotlicompress_options) for Brotli compression in its zlib module.

We can take advantage of this built-in support for Brotli in Node 11.7.0 and later by just passing in the appropriate `algorithm` to the CompressionPlugin:

**webpack.config.js**

```js
module.exports = {
  plugins: [
    new CompressionPlugin({
      filename: '[path].br[query]',
      algorithm: 'brotliCompress',
      test: /\.(js|css|html|svg)$/,
      compressionOptions: { level: 11 },
      threshold: 10240,
      minRatio: 0.8,
      deleteOriginalAssets: false,
    }),
  ],
};
```

**N.B.:** The `level` option matches `BROTLI_PARAM_QUALITY` [for Brotli-based streams](https://nodejs.org/api/zlib.html#zlib_for_brotli_based_streams)

### Multiple compressed versions of assets for different algorithm

**webpack.config.js**

```js
module.exports = {
  plugins: [
    new CompressionPlugin({
      filename: '[path].gz[query]',
      algorithm: 'gzip',
      test: /\.js$|\.css$|\.html$/,
      threshold: 10240,
      minRatio: 0.8,
    }),
    new CompressionPlugin({
      filename: '[path].br[query]',
      algorithm: 'brotliCompress',
      test: /\.(js|css|html|svg)$/,
      compressionOptions: { level: 11 },
      threshold: 10240,
      minRatio: 0.8,
    }),
  ],
};
```

## Contributing

Please take a moment to read our contributing guidelines if you haven't yet done so.

[CONTRIBUTING](./.github/CONTRIBUTING.md)

## License

[MIT](./LICENSE)

[npm]: https://img.shields.io/npm/v/compression-webpack-plugin.svg
[npm-url]: https://npmjs.com/package/compression-webpack-plugin
[node]: https://img.shields.io/node/v/compression-webpack-plugin.svg
[node-url]: https://nodejs.org
[deps]: https://david-dm.org/webpack-contrib/compression-webpack-plugin.svg
[deps-url]: https://david-dm.org/webpack-contrib/compression-webpack-plugin
[tests]: https://dev.azure.com/webpack-contrib/compression-webpack-plugin/_apis/build/status/webpack-contrib.compression-webpack-plugin?branchName=master
[tests-url]: https://dev.azure.com/webpack-contrib/compression-webpack-plugin/_build/latest?definitionId=2&branchName=master
[cover]: https://codecov.io/gh/webpack-contrib/compression-webpack-plugin/branch/master/graph/badge.svg
[cover-url]: https://codecov.io/gh/webpack-contrib/compression-webpack-plugin
[chat]: https://img.shields.io/badge/gitter-webpack%2Fwebpack-brightgreen.svg
[chat-url]: https://gitter.im/webpack/webpack
[size]: https://packagephobia.now.sh/badge?p=compression-webpack-plugin
[size-url]: https://packagephobia.now.sh/result?p=compression-webpack-plugin

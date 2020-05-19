<div align="center">
  <img height="100"
    src="https://worldvectorlogo.com/logos/sass-1.svg">
  <a href="https://github.com/webpack/webpack">
    <img width="200" height="200"
      src="https://webpack.js.org/assets/icon-square-big.svg">
  </a>
</div>

[![npm][npm]][npm-url]
[![node][node]][node-url]
[![deps][deps]][deps-url]
[![tests][tests]][tests-url]
[![coverage][cover]][cover-url]
[![chat][chat]][chat-url]
[![size][size]][size-url]

# sass-loader

Loads a Sass/SCSS file and compiles it to CSS.

## Getting Started

To begin, you'll need to install `sass-loader`:

```console
npm install sass-loader node-sass webpack --save-dev
```

The sass-loader requires you to install either [Node Sass](https://github.com/sass/node-sass) or [Dart Sass](https://github.com/sass/dart-sass) on your own (more documentation you can find below).
This allows you to control the versions of all your dependencies, and to choose which Sass implementation to use.

- [node sass](https://github.com/sass/node-sass)
- [dart sass](http://sass-lang.com/dart-sass)

Chain the sass-loader with the [css-loader](https://github.com/webpack-contrib/css-loader) and the [style-loader](https://github.com/webpack-contrib/style-loader) to immediately apply all styles to the DOM or the [mini-css-extract-plugin](https://github.com/webpack-contrib/mini-css-extract-plugin) to extract it into a separate file.

Then add the loader to your `webpack` config. For example:

**file.js**

```js
import style from './style.scss';
```

**file.scss**

```scss
$body-color: red;

body {
  color: $body-color;
}
```

**webpack.config.js**

```js
module.exports = {
  module: {
    rules: [
      {
        test: /\.s[ac]ss$/i,
        use: [
          // Creates `style` nodes from JS strings
          'style-loader',
          // Translates CSS into CommonJS
          'css-loader',
          // Compiles Sass to CSS
          'sass-loader',
        ],
      },
    ],
  },
};
```

And run `webpack` via your preferred method.

### Resolving `import` at-rules

The webpack provides an [advanced mechanism to resolve files](https://webpack.js.org/concepts/module-resolution/).

The sass-loader uses Sass's custom importer feature to pass all queries to the webpack resolving engine. Thus you can import your Sass modules from `node_modules`. Just prepend them with a `~` to tell webpack that this is not a relative import:

```css
@import '~bootstrap';
```

It's important to only prepend it with `~`, because `~/` resolves to the home directory.
The webpack needs to distinguish between `bootstrap` and `~bootstrap` because CSS and Sass files have no special syntax for importing relative files.
Writing `@import "file"` is the same as `@import "./file";`

### Problems with `url(...)`

Since sass implementations don't provide [url rewriting](https://github.com/sass/libsass/issues/532), all linked assets must be relative to the output.

- If you pass the generated CSS on to the css-loader, all urls must be relative to the entry-file (e.g. `main.scss`).
- If you're just generating CSS without passing it to the css-loader, it must be relative to your web root.

You will be disrupted by this first issue. It is natural to expect relative references to be resolved against the `.sass`/`.scss` file in which they are specified (like in regular `.css` files).

Thankfully there are a two solutions to this problem:

- Add the missing url rewriting using the [resolve-url-loader](https://github.com/bholloway/resolve-url-loader). Place it before the sass-loader in the loader chain.
- Library authors usually provide a variable to modify the asset path. [bootstrap-sass](https://github.com/twbs/bootstrap-sass) for example has an `$icon-font-path`.

## Options

By default all options passed to loader also passed to to [Node Sass](https://github.com/sass/node-sass) or [Dart Sass](http://sass-lang.com/dart-sass)

> ℹ️ The `indentedSyntax` option has `true` value for the `sass` extension.
> ℹ️ Options such as `file` and `outFile` are unavailable.
> ℹ️ Only the "expanded" and "compressed" values of outputStyle are supported for `dart-sass`.
> ℹ We recommend don't use `sourceMapContents`, `sourceMapEmbed`, `sourceMapRoot` options because loader automatically setup this options.

There is a slight difference between the `node-sass` and `sass` options. We recommend look documentation before used them:

- [the Node Sass documentation](https://github.com/sass/node-sass/#options) for all available `node-sass` options.
- [the Dart Sass documentation](https://github.com/sass/dart-sass#javascript-api) for all available `sass` options.

**webpack.config.js**

```js
module.exports = {
  module: {
    rules: [
      {
        test: /\.s[ac]ss$/i,
        use: [
          'style-loader',
          'css-loader',
          {
            loader: 'sass-loader',
            options: {
              indentWidth: 4,
              includePaths: ['absolute/path/a', 'absolute/path/b'],
            },
          },
        ],
      },
    ],
  },
};
```

### `implementation`

The special `implementation` option determines which implementation of Sass to use.

By default the loader resolve the implementation based on your dependencies.
Just add required implementation to `package.json` (`node-sass` or `sass` package) and install dependencies.

Example where the `sass-loader` loader uses the `sass` (`dart-sass`) implementation:

**package.json**

```json
{
  "devDependencies": {
    "sass-loader": "^7.2.0",
    "sass": "^1.22.10"
  }
}
```

Example where the `sass-loader` loader uses the `node-sass` implementation:

**package.json**

```json
{
  "devDependencies": {
    "sass-loader": "^7.2.0",
    "node-sass": "^4.0.0"
  }
}
```

Beware the situation when `node-sass` and `sass` was installed, by default the `sass-loader` prefers `node-sass`, to avoid this situation use the `implementation` option.

It takes either `node-sass` or `sass` (`Dart Sass`) module.

For example, to use Dart Sass, you'd pass:

```js
module.exports = {
  module: {
    rules: [
      {
        test: /\.s[ac]ss$/i,
        use: [
          'style-loader',
          'css-loader',
          {
            loader: 'sass-loader',
            options: {
              // Prefer `dart-sass`
              implementation: require('sass'),
            },
          },
        ],
      },
    ],
  },
};
```

Note that when using `sass` (`Dart Sass`), **synchronous compilation is twice as fast as asynchronous compilation** by default, due to the overhead of asynchronous callbacks.
To avoid this overhead, you can use the [fibers](https://www.npmjs.com/package/fibers) package to call asynchronous importers from the synchronous code path.

To enable this, pass the `Fiber` class to the `fiber` option:

**webpack.config.js**

```js
module.exports = {
  module: {
    rules: [
      {
        test: /\.s[ac]ss$/i,
        use: [
          'style-loader',
          'css-loader',
          {
            loader: 'sass-loader',
            options: {
              implementation: require('sass'),
              fiber: require('fibers'),
            },
          },
        ],
      },
    ],
  },
};
```

### `data`

Type: `String|Function`
Default: `undefined`

Prepends `Sass`/`SCSS` code before the actual entry file.
In this case, the `sass-loader` will not override the `data` option but just append the entry's content.

This is especially useful when some of your Sass variables depend on the environment:

> ℹ Since you're injecting code, this will break the source mappings in your entry file. Often there's a simpler solution than this, like multiple Sass entry files.

#### `String`

```js
module.exports = {
  module: {
    rules: [
      {
        test: /\.s[ac]ss$/i,
        use: [
          'style-loader',
          'css-loader',
          {
            loader: 'sass-loader',
            options: {
              data: '$env: ' + process.env.NODE_ENV + ';',
            },
          },
        ],
      },
    ],
  },
};
```

#### `Function`

```js
module.exports = {
  module: {
    rules: [
      {
        test: /\.s[ac]ss$/i,
        use: [
          'style-loader',
          'css-loader',
          {
            loader: 'sass-loader',
            options: {
              data: (loaderContext) => {
                // More information about avalaible options https://webpack.js.org/api/loaders/
                const { resourcePath, rootContext } = loaderContext;
                const relativePath = path.relative(rootContext, resourcePath);

                if (relativePath === 'styles/foo.scss') {
                  return '$value: 100px;';
                }

                return '$value: 200px;';
              },
            },
          },
        ],
      },
    ],
  },
};
```

### `sourceMap`

Type: `Boolean`
Default: `false`

Enables/Disables generation of source maps.

They are not enabled by default because they expose a runtime overhead and increase in bundle size (JS source maps do not).

**webpack.config.js**

```js
module.exports = {
  module: {
    rules: [
      {
        test: /\.s[ac]ss$/i,
        use: [
          'style-loader',
          {
            loader: 'css-loader',
            options: {
              sourceMap: true,
            },
          },
          {
            loader: 'sass-loader',
            options: {
              sourceMap: true,
            },
          },
        ],
      },
    ],
  },
};
```

> ℹ In some rare case `node-sass` can output invalid source maps (it is `node-sass` bug), to avoid try to update node-sass to latest version or you can try to set the `outputStyle` option to `compressed` value.

### `webpackImporter`

Type: `Boolean`
Default: `true`

Allows to disable default `webpack` importer.

This can improve performance in some cases. Use it with caution because aliases and `@import` at-rules starts with `~` will not work, but you can pass own `importer` to solve this (see [`importer docs`](https://github.com/sass/node-sass#importer--v200---experimental)).

**webpack.config.js**

```js
module.exports = {
  module: {
    rules: [
      {
        test: /\.s[ac]ss$/i,
        use: [
          'style-loader',
          'css-loader',
          {
            loader: 'sass-loader',
            options: {
              webpackImporter: false,
            },
          },
        ],
      },
    ],
  },
};
```

## Examples

### Extracts CSS into separate files

For production builds it's recommended to extract the CSS from your bundle being able to use parallel loading of CSS/JS resources later on.

There are two possibilities to extract a style sheet from the bundle:

- [mini-css-extract-plugin](https://github.com/webpack-contrib/mini-css-extract-plugin) (use this, when using webpack 4 configuration. Works in all use-cases)
- [extract-loader](https://github.com/peerigon/extract-loader) (simpler, but specialized on the css-loader's output)

**webpack.config.js**

```js
const MiniCssExtractPlugin = require('mini-css-extract-plugin');

module.exports = {
  module: {
    rules: [
      {
        test: /\.s[ac]ss$/i,
        use: [
          // fallback to style-loader in development
          process.env.NODE_ENV !== 'production'
            ? 'style-loader'
            : MiniCssExtractPlugin.loader,
          'css-loader',
          'sass-loader',
        ],
      },
    ],
  },
  plugins: [
    new MiniCssExtractPlugin({
      // Options similar to the same options in webpackOptions.output
      // both options are optional
      filename: '[name].css',
      chunkFilename: '[id].css',
    }),
  ],
};
```

### Source maps

To enable CSS source maps, you'll need to pass the `sourceMap` option to the sass-loader _and_ the css-loader.

**webpack.config.js**

```javascript
module.exports = {
  devtool: 'source-map', // any "source-map"-like devtool is possible
  module: {
    rules: [
      {
        test: /\.scss$/,
        use: [
          'style-loader',
          {
            loader: 'css-loader',
            options: {
              sourceMap: true,
            },
          },
          {
            loader: 'sass-loader',
            options: {
              sourceMap: true,
            },
          },
        ],
      },
    ],
  },
};
```

If you want to edit the original Sass files inside Chrome, [there's a good blog post](https://medium.com/@toolmantim/getting-started-with-css-sourcemaps-and-in-browser-sass-editing-b4daab987fb0). Checkout [test/sourceMap](https://github.com/webpack-contrib/sass-loader/tree/master/test) for a running example.

## Contributing

Please take a moment to read our contributing guidelines if you haven't yet done so.

[CONTRIBUTING](./.github/CONTRIBUTING.md)

## License

[MIT](./LICENSE)

[npm]: https://img.shields.io/npm/v/sass-loader.svg
[npm-url]: https://npmjs.com/package/sass-loader
[node]: https://img.shields.io/node/v/sass-loader.svg
[node-url]: https://nodejs.org
[deps]: https://david-dm.org/webpack-contrib/sass-loader.svg
[deps-url]: https://david-dm.org/webpack-contrib/sass-loader
[tests]: https://dev.azure.com/webpack-contrib/sass-loader/_apis/build/status/webpack-contrib.sass-loader?branchName=master
[tests-url]: https://dev.azure.com/webpack-contrib/sass-loader/_build/latest?definitionId=21&branchName=master
[cover]: https://codecov.io/gh/webpack-contrib/sass-loader/branch/master/graph/badge.svg
[cover-url]: https://codecov.io/gh/webpack-contrib/sass-loader
[chat]: https://badges.gitter.im/webpack/webpack.svg
[chat-url]: https://gitter.im/webpack/webpack
[size]: https://packagephobia.now.sh/badge?p=css-loader
[size-url]: https://packagephobia.now.sh/result?p=css-loader

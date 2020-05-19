# ng-annotate-loader [![Build Status](https://img.shields.io/travis/huston007/ng-annotate-loader.svg?style=flat-square)](https://travis-ci.org/huston007/ng-annotate-loader)

Webpack loader to annotate angular applications. Generates a sourcemaps as well.

## Installation

```
npm install --save-dev ng-annotate-loader
```

## Usage:

```js
module: {
  loaders: [
    {
      test: /src.*\.js$/,
      use: [{ loader: 'ng-annotate-loader' }],
    }
  ]
}
```

#### Passing parameters:

```js
{
  test: /src.*\.js$/,
  use: [
    {
      loader: 'ng-annotate-loader',
      options: {
        add: false,
        map: false,
      }
    }
  ]
}
```

[More about `ng-annotate` parameters](https://github.com/olov/ng-annotate/blob/master/OPTIONS.md)

#### Using ng-annotate plugins:

```js
{
  test: /src.*\.js$/,
  use: [
    {
      loader: 'ng-annotate-loader',
      options: {
        plugin: ['ng-annotate-adf-plugin']
      }
    }
  ]
}
```

#### Using a fork of ng-annotate:

```js
{
  test: /src.*\.js$/,
  use: [
    {
      loader: 'ng-annotate-loader',
      options: {
        ngAnnotate: 'my-ng-annotate-fork'
      }
    }
  ]
}
```

#### Works great with js compilers, `babel` for example:

```js
{
  test: /src.*\.js$/,
  use: [
    { loader: 'ng-annotate-loader' },
    { loader: 'babel-loader' },
  ]
},
```

## Contributing

#### Compiling examples and run acceptance test

Run on the root folder:

```
npm install
npm test
```

[Using loaders](https://webpack.js.org/concepts/loaders/)

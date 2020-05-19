# img-loader

[![npm Version][npm-image]][npm]
[![Greenkeeper badge][greenkeeper-image]][greenkeeper]
[![Build Status][build-image]][build]

[![JS Standard Style][style-image]][style]
[![MIT License][license-image]][LICENSE]

Image minimizing loader for webpack 4, meant to be used with [url-loader](https://github.com/webpack/url-loader), [file-loader](https://github.com/webpack/file-loader), or [raw-loader](https://github.com/webpack/raw-loader)

> Minify PNG, JPEG, GIF and SVG images with [imagemin](https://github.com/imagemin/imagemin) [plugins](https://www.npmjs.com/search?q=keywords:imageminplugin)

img-loader has a peer dependency on `imagemin`, so you will need to make sure to include that, along with any imagemin plugins you will use.


## Install

```sh
$ npm install img-loader --save-dev
```


## Usage

[Documentation: Using loaders](https://webpack.js.org/concepts/loaders/)

```javascript
module: {
  rules: [
    {
      test: /\.(jpe?g|png|gif|svg)$/i,
      use: [
        'url-loader?limit=10000',
        'img-loader'
      ]
    }
  ]
}
```

By default the loader simply passes along the image unmodified.


### Options

Options are forwarded to `imagemin.buffer(image, options)`, so any plugins you would like to use for optimizing the images are passed as the `plugins` property.

For more details on each plugin's options, see their documentation on [Github](https://github.com/imagemin).

```js
{
  module: {
    rules: [
      {
        test: /\.(jpe?g|png|gif|svg)$/i,
        use: [
          'url-loader?limit=10000',
          {
            loader: 'img-loader',
            options: {
              plugins: [
                require('imagemin-gifsicle')({
                  interlaced: false
                }),
                require('imagemin-mozjpeg')({
                  progressive: true,
                  arithmetic: false
                }),
                require('imagemin-pngquant')({
                  floyd: 0.5,
                  speed: 2
                }),
                require('imagemin-svgo')({
                  plugins: [
                    { removeTitle: true },
                    { convertPathData: false }
                  ]
                })
              ]
            }
          }
        ]
      }
    ]
  }
}
```

`plugins` can also be a function, which will receive the [webpack loader context](https://webpack.js.org/api/loaders/#the-loader-context) and should return the plugins array.
```js
{
  module: {
    rules: [
      {
        test: /\.(jpe?g|png|gif|svg)$/i,
        use: [
          'url-loader?limit=10000',
          {
            loader: 'img-loader',
            options: {
              plugins (context) {
                if (process.env.NODE_ENV === 'production') return []
                return [
                  require('imagemin-svgo')({
                    plugins: [
                      { cleanupIDs: false },
                      {
                        prefixIds: {
                          prefix: path.basename(context.resourcePath, 'svg')
                        }
                      }
                    ]
                  })
                ]
              }
            }
          }
        ]
      }
    ]
  }
}
```

If you only want to run imagemin in production builds, you can omit the `img-loader` or leave plugins empty in your production configuration file. If you don't keep a separate configuration for prod builds, something like the following also works:

```js
{
  loader: 'img-loader',
  options: {
    plugins: process.env.NODE_ENV === 'production' && [
      require('imagemin-svgo')({})
      // etc.
    ]
  }
}
```


## Migrating from 2.x

To get the default behavior from version `2.0.1`, you'll need to install these imagemin plugins:

* [imagemin-gifsicle](https://github.com/imagemin/imagemin-gifsicle)
* [imagemin-mozjpeg](https://github.com/imagemin/imagemin-mozjpeg)
* [imagemin-optipng](https://github.com/imagemin/imagemin-optipng)
* [imagemin-svgo](https://github.com/imagemin/imagemin-svgo)

Then use this loader setup in your webpack configuration file:

```js
{
  loader: 'img-loader',
  options: {
    plugins: [
      require('imagemin-gifsicle')({}),
      require('imagemin-mozjpeg')({}),
      require('imagemin-optipng')({}),
      require('imagemin-svgo')({})
    ]
  }
}
```

The options object you had under a plugin's name property, should instead be passed directly to the plugin after you require it.

If you used the optional `pngquant` settings, then you will additionally need to install [imagemin-pngquant](https://github.com/imagemin/imagemin-pngquant), and add it to your plugins array as any other imagemin plugin.


## License

This software is free to use under the MIT license. See the [LICENSE-MIT file][LICENSE] for license text and copyright information.

[npm]: https://www.npmjs.org/package/img-loader
[npm-image]: https://img.shields.io/npm/v/img-loader.svg
[greenkeeper-image]: https://badges.greenkeeper.io/thetalecrafter/img-loader.svg
[greenkeeper]: https://greenkeeper.io/
[build]: https://travis-ci.org/thetalecrafter/img-loader
[build-image]: https://img.shields.io/travis/thetalecrafter/img-loader.svg
[style]: https://github.com/feross/standard
[style-image]: https://img.shields.io/badge/code%20style-standard-brightgreen.svg
[license-image]: https://img.shields.io/npm/l/img-loader.svg
[LICENSE]: https://github.com/thetalecrafter/img-loader/blob/master/LICENSE-MIT

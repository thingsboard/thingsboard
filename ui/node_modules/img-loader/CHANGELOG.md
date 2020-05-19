# Changelog

## 3.0.1

Updated dependencies

## 3.0.0

**Breaking Change** By default no image optimizing is done. The options for the loader are passed directly to `imagemin.buffer`, so `options.plugins` should be passed as an array of configured imagemin plugins. If `plugins` is a function it will be called with the webpack loader context, and the plugin array should be returned.

## 2.0.1

Updated dependencies

## 2.0.0

Require Node 4. Support only webpack 2. Switch from jpegtran to mozjpeg.
Switch to `enabled` option instead of detecting `minimize` from UglifyJSPlugin.

## 1.3.1

Updated dependencies

## 1.3.0

Support `?config=otherConfig` to specify advanced options

## 1.2.2

Make sure that the webpack callback is only called once

## 1.2.1

Updated dependencies

## 1.2.0

New: Allow using pngquant plugin via advanced options

Match default `optimizationLevel` 2 in plugin in this loader's defaults

## 1.1.0

New: Allow options in an `imagemin` property on the webpack config

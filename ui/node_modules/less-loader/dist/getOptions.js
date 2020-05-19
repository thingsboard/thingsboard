"use strict";

function _objectSpread(target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i] != null ? arguments[i] : {}; var ownKeys = Object.keys(source); if (typeof Object.getOwnPropertySymbols === 'function') { ownKeys = ownKeys.concat(Object.getOwnPropertySymbols(source).filter(function (sym) { return Object.getOwnPropertyDescriptor(source, sym).enumerable; })); } ownKeys.forEach(function (key) { _defineProperty(target, key, source[key]); }); } return target; }

function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

const loaderUtils = require('loader-utils');

const clone = require('clone');

const createWebpackLessPlugin = require('./createWebpackLessPlugin');
/**
 * Retrieves the options from the loaderContext, makes a deep copy of it and normalizes it for further consumption.
 *
 * @param {LoaderContext} loaderContext
 */


function getOptions(loaderContext) {
  const options = _objectSpread({
    plugins: [],
    relativeUrls: true
  }, clone(loaderUtils.getOptions(loaderContext))); // We need to set the filename because otherwise our WebpackFileManager will receive an undefined path for the entry


  options.filename = loaderContext.resource; // When no paths are given, we use the webpack resolver

  if ('paths' in options === false) {
    // It's safe to mutate the array now because it has already been cloned
    options.plugins.push(createWebpackLessPlugin(loaderContext));
  }

  if (options.sourceMap) {
    if (typeof options.sourceMap === 'boolean') {
      options.sourceMap = {};
    }

    if ('outputSourceFiles' in options.sourceMap === false) {
      // Include source files as `sourceContents` as sane default since this makes source maps "just work" in most cases
      options.sourceMap.outputSourceFiles = true;
    }
  }

  return options;
}

module.exports = getOptions;
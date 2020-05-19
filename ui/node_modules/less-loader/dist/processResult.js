"use strict";

const removeSourceMappingUrl = require('./removeSourceMappingUrl');

const formatLessError = require('./formatLessError');
/**
 * Removes the sourceMappingURL from the generated CSS, parses the source map and calls the next loader.
 *
 * @param {loaderContext} loaderContext
 * @param {Promise<LessResult>} resultPromise
 */


function processResult(loaderContext, resultPromise) {
  const {
    callback
  } = loaderContext;
  resultPromise.then(({
    css,
    map,
    imports
  }) => {
    imports.forEach(loaderContext.addDependency, loaderContext);
    return {
      // Removing the sourceMappingURL comment.
      // See removeSourceMappingUrl.js for the reasoning behind this.
      css: removeSourceMappingUrl(css),
      map: typeof map === 'string' ? JSON.parse(map) : map
    };
  }, lessError => {
    if (lessError.filename) {
      loaderContext.addDependency(lessError.filename);
    }

    throw formatLessError(lessError);
  }).then(({
    css,
    map
  }) => {
    callback(null, css, map);
  }, callback);
}

module.exports = processResult;
var utils = require('loader-utils');
var clone = require('clone');
var SourceMapConsumer = require('source-map').SourceMapConsumer;
var SourceMapGenerator = require('source-map').SourceMapGenerator;
var normalizePath = require('normalize-path');

function loadPlugins(pluginNames) {
  pluginNames = pluginNames || [];
  return pluginNames.map(function(name) {
    return require(name);
  });
}

function getOptions(sourceMapEnabled, filename) {
  //  options object may be re-used across multiple invocations.
  var options = clone(utils.getOptions(this) || {});

  //"add" should be a default option if not overrided in query
  if (options.add === undefined) {
    options.add = true;
  }

  if (options.ngAnnotate === undefined) {
    options.ngAnnotate = 'ng-annotate';
  }

  if (sourceMapEnabled && options.map === undefined) {
    options.map = {
      inline: false,
      inFile: filename,
    };
  }

  if (options.plugin) {
    options.plugin = loadPlugins(options.plugin);
  }

  return options;
}

function mergeSourceMaps(inputSourceMap, annotateMap) {
  var outputSourceMap;
  var sourceMapEnabled = this.sourceMap;
  var filename = normalizePath(this.resourcePath);
  this.cacheable && this.cacheable();

  if (sourceMapEnabled && !inputSourceMap && annotateMap) {
    outputSourceMap = annotateMap;
  }

  // Using BabelJS as an example,
  //   https://github.com/babel/babel/blob/d3a73b87e9007104cb4fec343f0cfb9e1c67a4ec/packages/babel/src/transformation/file/index.js#L465
  // See also vinyl-sourcemaps-apply (used by gulp-ng-annotate) - https://github.com/floridoo/vinyl-sourcemaps-apply/blob/master/index.js
  if (sourceMapEnabled && inputSourceMap) {
    inputSourceMap.sourceRoot = '';
    inputSourceMap.sources[0] = filename;

    if (annotateMap) {
      var generator = SourceMapGenerator.fromSourceMap(new SourceMapConsumer(annotateMap));
      generator.applySourceMap(new SourceMapConsumer(inputSourceMap), filename);

      outputSourceMap = generator.toJSON();

      //Should be set to avoid '../../file is not in SourceMap error https://github.com/huston007/ng-annotate-loader/pull/11'
      outputSourceMap.sourceRoot = '';
      //Copy file name from incoming file because it is empty by some unknown reaon
      outputSourceMap.file = normalizePath(this.resourcePath);
    } else {
      outputSourceMap = inputSourceMap;
    }
  }

  return outputSourceMap;
}

module.exports = function(source, inputSourceMap) {
  var sourceMapEnabled = this.sourceMap;
  var filename = normalizePath(this.resourcePath);
  this.cacheable && this.cacheable();

  var options = getOptions.call(this, sourceMapEnabled, filename);

  var ngAnnotate = require(options.ngAnnotate);
  var annotateResult = ngAnnotate(source, options);

  if (annotateResult.errors) {
    this.callback(annotateResult.errors);
  } else if (annotateResult.src !== source) {
    var outputSourceMap = mergeSourceMaps.call(this, inputSourceMap, annotateResult.map);
    this.callback(null, annotateResult.src || source, outputSourceMap);
  } else {
    // if ngAnnotate did nothing, return map and result untouched
    this.callback(null, source, inputSourceMap);
  }
};

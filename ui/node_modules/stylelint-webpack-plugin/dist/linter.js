"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.default = linter;

var _StylelintError = _interopRequireDefault(require("./StylelintError"));

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

function linter(lint, options, compiler, callback) {
  let errors = [];
  let warnings = [];
  lint(options).then(({
    results
  }) => {
    ({
      errors,
      warnings
    } = parseResults(options, results));
    compiler.hooks.afterEmit.tapAsync('StylelintWebpackPlugin', (compilation, next) => {
      if (warnings.length) {
        compilation.warnings.push(_StylelintError.default.format(options, warnings));
        warnings = [];
      }

      if (errors.length) {
        compilation.errors.push(_StylelintError.default.format(options, errors));
        errors = [];
      }

      next();
    });

    if (options.failOnError && errors.length) {
      callback(_StylelintError.default.format(options, errors));
    } else if (options.failOnWarning && warnings.length) {
      callback(_StylelintError.default.format(options, warnings));
    } else {
      callback();
    }
  }).catch(e => {
    compiler.hooks.afterEmit.tapAsync('StylelintWebpackPlugin', (compilation, next) => {
      compilation.errors.push(new _StylelintError.default(e.message));
      next();
    });
    callback();
  });
}

function parseResults(options, results) {
  let errors = [];
  let warnings = [];

  if (options.emitError) {
    errors = results.filter(file => fileHasErrors(file) || fileHasWarnings(file));
  } else if (options.emitWarning) {
    warnings = results.filter(file => fileHasErrors(file) || fileHasWarnings(file));
  } else {
    warnings = results.filter(file => !fileHasErrors(file) && fileHasWarnings(file));
    errors = results.filter(fileHasErrors);
  }

  if (options.quiet && warnings.length) {
    warnings = [];
  }

  return {
    errors,
    warnings
  };
}

function fileHasErrors(file) {
  return file.errored;
}

function fileHasWarnings(file) {
  return file.warnings && file.warnings.length;
}
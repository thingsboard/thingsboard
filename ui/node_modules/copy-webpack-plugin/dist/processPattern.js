"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.default = processPattern;

var _path = _interopRequireDefault(require("path"));

var _globby = _interopRequireDefault(require("globby"));

var _pLimit = _interopRequireDefault(require("p-limit"));

var _minimatch = _interopRequireDefault(require("minimatch"));

var _isObject = _interopRequireDefault(require("./utils/isObject"));

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

function processPattern(globalRef, pattern) {
  const {
    logger,
    output,
    concurrency,
    compilation
  } = globalRef;
  const globOptions = Object.assign({
    cwd: pattern.context,
    follow: true // Todo in next major release
    // dot: false

  }, pattern.globOptions || {});

  if (pattern.fromType === 'nonexistent') {
    return Promise.resolve();
  }

  const limit = (0, _pLimit.default)(concurrency || 100);
  logger.info(`begin globbing '${pattern.glob}' with a context of '${pattern.context}'`);
  return (0, _globby.default)(pattern.glob, globOptions).then(paths => Promise.all(paths.map(from => limit(() => {
    const file = {
      force: pattern.force,
      absoluteFrom: _path.default.resolve(pattern.context, from)
    };
    file.relativeFrom = _path.default.relative(pattern.context, file.absoluteFrom);

    if (pattern.flatten) {
      file.relativeFrom = _path.default.basename(file.relativeFrom);
    }

    logger.debug(`found ${from}`); // Check the ignore list

    let il = pattern.ignore.length; // eslint-disable-next-line no-plusplus

    while (il--) {
      const ignoreGlob = pattern.ignore[il];
      let globParams = {
        dot: true,
        matchBase: true
      };
      let glob;

      if (typeof ignoreGlob === 'string') {
        glob = ignoreGlob;
      } else if ((0, _isObject.default)(ignoreGlob)) {
        glob = ignoreGlob.glob || '';
        const ignoreGlobParams = Object.assign({}, ignoreGlob);
        delete ignoreGlobParams.glob; // Overwrite minimatch defaults

        globParams = Object.assign(globParams, ignoreGlobParams);
      } else {
        glob = '';
      }

      logger.debug(`testing ${glob} against ${file.relativeFrom}`);

      if ((0, _minimatch.default)(file.relativeFrom, glob, globParams)) {
        logger.info(`ignoring '${file.relativeFrom}', because it matches the ignore glob '${glob}'`);
        return Promise.resolve();
      }

      logger.debug(`${glob} doesn't match ${file.relativeFrom}`);
    } // Change the to path to be relative for webpack


    if (pattern.toType === 'dir') {
      file.webpackTo = _path.default.join(pattern.to, file.relativeFrom);
    } else if (pattern.toType === 'file') {
      file.webpackTo = pattern.to || file.relativeFrom;
    } else if (pattern.toType === 'template') {
      file.webpackTo = pattern.to;
      file.webpackToRegExp = pattern.test;
    }

    if (_path.default.isAbsolute(file.webpackTo)) {
      if (output === '/') {
        const message = 'using older versions of webpack-dev-server, devServer.outputPath must be defined to write to absolute paths';
        logger.error(message);
        compilation.errors.push(new Error(message));
      }

      file.webpackTo = _path.default.relative(output, file.webpackTo);
    }

    logger.info(`determined that '${from}' should write to '${file.webpackTo}'`);
    return file;
  }))));
}
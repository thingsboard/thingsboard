"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.default = postProcessPattern;

var _path = _interopRequireDefault(require("path"));

var _os = _interopRequireDefault(require("os"));

var _crypto = _interopRequireDefault(require("crypto"));

var _loaderUtils = _interopRequireDefault(require("loader-utils"));

var _cacache = _interopRequireDefault(require("cacache"));

var _serializeJavascript = _interopRequireDefault(require("serialize-javascript"));

var _findCacheDir = _interopRequireDefault(require("find-cache-dir"));

var _normalizePath = _interopRequireDefault(require("normalize-path"));

var _package = require("../package.json");

var _promisify = require("./utils/promisify");

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

/* eslint-disable no-param-reassign */
function postProcessPattern(globalRef, pattern, file) {
  const {
    logger,
    compilation,
    fileDependencies,
    written,
    inputFileSystem,
    copyUnmodified
  } = globalRef;
  logger.debug(`getting stats for '${file.absoluteFrom}' to write to assets`);
  const getStats = pattern.stats ? Promise.resolve().then(() => pattern.stats) : (0, _promisify.stat)(inputFileSystem, file.absoluteFrom);
  return getStats.then(stats => {
    // We don't write empty directories
    if (stats.isDirectory()) {
      logger.debug(`skipping '${file.absoluteFrom}' because it is empty directory`);
      return Promise.resolve();
    } // If this came from a glob, add it to the file watchlist


    if (pattern.fromType === 'glob') {
      fileDependencies.add(file.absoluteFrom);
    }

    logger.debug(`reading '${file.absoluteFrom}' to write to assets`);
    return (0, _promisify.readFile)(inputFileSystem, file.absoluteFrom).then(content => {
      if (pattern.transform) {
        logger.info(`transforming content for '${file.absoluteFrom}'`); // eslint-disable-next-line no-shadow

        const transform = (content, absoluteFrom) => pattern.transform(content, absoluteFrom);

        if (pattern.cache) {
          if (!globalRef.cacheDir) {
            globalRef.cacheDir = (0, _findCacheDir.default)({
              name: 'copy-webpack-plugin'
            }) || _os.default.tmpdir();
          }

          const cacheKey = pattern.cache.key ? pattern.cache.key : (0, _serializeJavascript.default)({
            name: _package.name,
            version: _package.version,
            pattern,
            hash: _crypto.default.createHash('md4').update(content).digest('hex')
          });
          return _cacache.default.get(globalRef.cacheDir, cacheKey).then(result => {
            logger.debug(`getting cached transformation for '${file.absoluteFrom}'`);
            return result.data;
          }, () => Promise.resolve().then(() => transform(content, file.absoluteFrom)) // eslint-disable-next-line no-shadow
          .then(content => {
            logger.debug(`caching transformation for '${file.absoluteFrom}'`);
            return _cacache.default.put(globalRef.cacheDir, cacheKey, content).then(() => content);
          }));
        }

        content = transform(content, file.absoluteFrom);
      }

      return content;
    }).then(content => {
      if (pattern.toType === 'template') {
        logger.info(`interpolating template '${file.webpackTo}' for '${file.relativeFrom}'`); // If it doesn't have an extension, remove it from the pattern
        // ie. [name].[ext] or [name][ext] both become [name]

        if (!_path.default.extname(file.relativeFrom)) {
          file.webpackTo = file.webpackTo.replace(/\.?\[ext\]/g, '');
        }

        file.webpackTo = _loaderUtils.default.interpolateName({
          resourcePath: file.absoluteFrom
        }, file.webpackTo, {
          content,
          regExp: file.webpackToRegExp,
          context: pattern.context
        }); // Bug in `loader-utils`, package convert `\\` to `/`, need fix in loader-utils

        file.webpackTo = _path.default.normalize(file.webpackTo);
      }

      return content;
    }).then(content => {
      if (pattern.transformPath) {
        logger.info(`transforming path '${file.webpackTo}' for '${file.absoluteFrom}'`);
        return Promise.resolve().then(() => pattern.transformPath(file.webpackTo, file.absoluteFrom)).then(newPath => {
          file.webpackTo = newPath;
          return content;
        });
      }

      return content;
    }).then(content => {
      const hash = _loaderUtils.default.getHashDigest(content);

      const targetPath = (0, _normalizePath.default)(file.webpackTo);
      const targetAbsolutePath = (0, _normalizePath.default)(file.absoluteFrom);

      if (!copyUnmodified && written[targetPath] && written[targetPath][targetAbsolutePath] && written[targetPath][targetAbsolutePath] === hash) {
        logger.info(`skipping '${file.webpackTo}', because content hasn't changed`);
        return;
      }

      logger.debug(`adding '${file.webpackTo}' for tracking content changes`);

      if (!written[targetPath]) {
        written[targetPath] = {};
      }

      written[targetPath][targetAbsolutePath] = hash;

      if (compilation.assets[targetPath] && !file.force) {
        logger.info(`skipping '${file.webpackTo}', because it already exists`);
        return;
      }

      logger.info(`writing '${file.webpackTo}' to compilation assets from '${file.absoluteFrom}'`);
      compilation.assets[targetPath] = {
        size() {
          return stats.size;
        },

        source() {
          return content;
        }

      };
    });
  });
}
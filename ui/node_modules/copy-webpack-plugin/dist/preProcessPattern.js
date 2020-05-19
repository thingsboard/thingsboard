"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.default = preProcessPattern;

var _path = _interopRequireDefault(require("path"));

var _isGlob = _interopRequireDefault(require("is-glob"));

var _globParent = _interopRequireDefault(require("glob-parent"));

var _normalize = _interopRequireDefault(require("./utils/normalize"));

var _isTemplateLike = _interopRequireDefault(require("./utils/isTemplateLike"));

var _isObject = _interopRequireDefault(require("./utils/isObject"));

var _promisify = require("./utils/promisify");

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

/* eslint-disable no-param-reassign */
function preProcessPattern(globalRef, pattern) {
  const {
    logger,
    context,
    inputFileSystem,
    fileDependencies,
    contextDependencies,
    compilation
  } = globalRef;
  pattern = typeof pattern === 'string' ? {
    from: pattern
  } : Object.assign({}, pattern);
  pattern.to = pattern.to || '';
  pattern.context = pattern.context || context;

  if (!_path.default.isAbsolute(pattern.context)) {
    pattern.context = _path.default.join(context, pattern.context);
  }

  const isFromGlobPatten = (0, _isObject.default)(pattern.from) && pattern.from.glob || pattern.globOptions; // Todo remove this in next major

  const isToDirectory = _path.default.extname(pattern.to) === '' || pattern.to.slice(-1) === _path.default.sep; // Normalize paths


  pattern.from = isFromGlobPatten ? pattern.from : _path.default.normalize(pattern.from);
  pattern.context = _path.default.normalize(pattern.context);
  pattern.to = _path.default.normalize(pattern.to);
  pattern.ignore = globalRef.ignore.concat(pattern.ignore || []);
  logger.debug(`processing from: '${pattern.from}' to: '${pattern.to}'`);

  switch (true) {
    // if toType already exists
    case !!pattern.toType:
      break;

    case (0, _isTemplateLike.default)(pattern.to):
      pattern.toType = 'template';
      break;

    case isToDirectory:
      pattern.toType = 'dir';
      break;

    default:
      pattern.toType = 'file';
  } // If we know it's a glob, then bail early


  if (isFromGlobPatten) {
    logger.debug(`determined '${pattern.absoluteFrom}' is a glob`);
    pattern.fromType = 'glob';
    const globOptions = Object.assign({}, pattern.globOptions ? pattern.globOptions : pattern.from);
    delete globOptions.glob;
    pattern.absoluteFrom = _path.default.resolve(pattern.context, pattern.globOptions ? pattern.from : pattern.from.glob);
    pattern.glob = (0, _normalize.default)(pattern.context, pattern.globOptions ? pattern.from : pattern.from.glob);
    pattern.globOptions = globOptions;
    return Promise.resolve(pattern);
  }

  if (_path.default.isAbsolute(pattern.from)) {
    pattern.absoluteFrom = pattern.from;
  } else {
    pattern.absoluteFrom = _path.default.resolve(pattern.context, pattern.from);
  }

  logger.debug(`determined '${pattern.from}' to be read from '${pattern.absoluteFrom}'`);

  const noStatsHandler = () => {
    // If from doesn't appear to be a glob, then log a warning
    if ((0, _isGlob.default)(pattern.from) || pattern.from.includes('*')) {
      logger.debug(`determined '${pattern.absoluteFrom}' is a glob`);
      pattern.fromType = 'glob';
      pattern.glob = (0, _normalize.default)(pattern.context, pattern.from); // We need to add context directory as dependencies to avoid problems when new files added in directories
      // when we already in watch mode and this directories are not in context dependencies
      // `glob-parent` always return `/` we need normalize path

      contextDependencies.add(_path.default.normalize((0, _globParent.default)(pattern.absoluteFrom)));
    } else {
      const newWarning = new Error(`unable to locate '${pattern.from}' at '${pattern.absoluteFrom}'`);
      const hasWarning = compilation.warnings.some( // eslint-disable-next-line no-shadow
      warning => warning.message === newWarning.message); // Only display the same message once

      if (!hasWarning) {
        logger.warn(newWarning.message);
        compilation.warnings.push(newWarning);
      }

      pattern.fromType = 'nonexistent';
    }
  };

  logger.debug(`getting stats for '${pattern.absoluteFrom}' to determinate 'fromType'`);
  return (0, _promisify.stat)(inputFileSystem, pattern.absoluteFrom).catch(() => noStatsHandler()).then(stats => {
    if (!stats) {
      noStatsHandler();
      return pattern;
    }

    if (stats.isDirectory()) {
      logger.debug(`determined '${pattern.absoluteFrom}' is a directory`);
      contextDependencies.add(pattern.absoluteFrom);
      pattern.fromType = 'dir';
      pattern.context = pattern.absoluteFrom;
      pattern.glob = (0, _normalize.default)(pattern.absoluteFrom, '**/*');
      pattern.absoluteFrom = _path.default.join(pattern.absoluteFrom, '**/*');
      pattern.globOptions = {
        dot: true
      };
    } else if (stats.isFile()) {
      logger.debug(`determined '${pattern.absoluteFrom}' is a file`);
      fileDependencies.add(pattern.absoluteFrom);
      pattern.stats = stats;
      pattern.fromType = 'file';
      pattern.context = _path.default.dirname(pattern.absoluteFrom);
      pattern.glob = (0, _normalize.default)(pattern.absoluteFrom);
      pattern.globOptions = {
        dot: true
      };
    } else if (!pattern.fromType) {
      logger.warn(`unrecognized file type for ${pattern.from}`);
    }

    return pattern;
  });
}
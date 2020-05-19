"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.default = void 0;

var _path = _interopRequireDefault(require("path"));

var _schemaUtils = _interopRequireDefault(require("schema-utils"));

var _webpackLog = _interopRequireDefault(require("webpack-log"));

var _options = _interopRequireDefault(require("./options.json"));

var _preProcessPattern = _interopRequireDefault(require("./preProcessPattern"));

var _processPattern = _interopRequireDefault(require("./processPattern"));

var _postProcessPattern = _interopRequireDefault(require("./postProcessPattern"));

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

class CopyPlugin {
  constructor(patterns = [], options = {}) {
    (0, _schemaUtils.default)(_options.default, patterns, this.constructor.name);
    this.patterns = patterns;
    this.options = options;
  }

  apply(compiler) {
    const fileDependencies = new Set();
    const contextDependencies = new Set();
    const written = {};
    let context;

    if (!this.options.context) {
      ({
        context
      } = compiler.options);
    } else if (!_path.default.isAbsolute(this.options.context)) {
      context = _path.default.join(compiler.options.context, this.options.context);
    } else {
      ({
        context
      } = this.options);
    }

    const logger = (0, _webpackLog.default)({
      name: 'copy-webpack-plugin',
      level: this.options.logLevel || 'warn'
    });
    const plugin = {
      name: 'CopyPlugin'
    };
    compiler.hooks.emit.tapAsync(plugin, (compilation, callback) => {
      logger.debug('starting emit');
      const globalRef = {
        logger,
        compilation,
        written,
        fileDependencies,
        contextDependencies,
        context,
        inputFileSystem: compiler.inputFileSystem,
        output: compiler.options.output.path,
        ignore: this.options.ignore || [],
        copyUnmodified: this.options.copyUnmodified,
        concurrency: this.options.concurrency
      };

      if (globalRef.output === '/' && compiler.options.devServer && compiler.options.devServer.outputPath) {
        globalRef.output = compiler.options.devServer.outputPath;
      }

      const {
        patterns
      } = this;
      Promise.all(patterns.map(pattern => Promise.resolve().then(() => (0, _preProcessPattern.default)(globalRef, pattern)) // Every source (from) is assumed to exist here
      // eslint-disable-next-line no-shadow
      .then(pattern => (0, _processPattern.default)(globalRef, pattern).then(files => {
        if (!files) {
          return Promise.resolve();
        }

        return Promise.all(files.filter(Boolean).map(file => (0, _postProcessPattern.default)(globalRef, pattern, file)));
      })))).catch(error => {
        compilation.errors.push(error);
      }).then(() => {
        logger.debug('finishing emit');
        callback();
      });
    });
    compiler.hooks.afterEmit.tapAsync(plugin, (compilation, callback) => {
      logger.debug('starting after-emit'); // Add file dependencies

      if ('addAll' in compilation.fileDependencies) {
        compilation.fileDependencies.addAll(fileDependencies);
      } else {
        for (const fileDependency of fileDependencies) {
          compilation.fileDependencies.add(fileDependency);
        }
      } // Add context dependencies


      if ('addAll' in compilation.contextDependencies) {
        compilation.contextDependencies.addAll(contextDependencies);
      } else {
        for (const contextDependency of contextDependencies) {
          compilation.contextDependencies.add(contextDependency);
        }
      }

      logger.debug('finishing after-emit');
      callback();
    });
  }

}

var _default = CopyPlugin;
exports.default = _default;
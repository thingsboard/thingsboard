"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.default = void 0;

var _path = require("path");

var _getOptions = _interopRequireDefault(require("./getOptions"));

var _LintDirtyModulesPlugin = _interopRequireDefault(require("./LintDirtyModulesPlugin"));

var _linter = _interopRequireDefault(require("./linter"));

var _utils = require("./utils");

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

class StylelintWebpackPlugin {
  constructor(options = {}) {
    this.options = (0, _getOptions.default)(options);
  }

  apply(compiler) {
    const options = { ...this.options,
      files: (0, _utils.parseFiles)(this.options.files, this.getContext(compiler))
    }; // eslint-disable-next-line

    const {
      lint
    } = require(options.stylelintPath);

    const plugin = {
      name: this.constructor.name
    };

    if (options.lintDirtyModulesOnly) {
      const lintDirty = new _LintDirtyModulesPlugin.default(lint, compiler, options);
      /* istanbul ignore next */

      compiler.hooks.watchRun.tapAsync(plugin, (compilation, callback) => {
        lintDirty.apply(compilation, callback);
      });
    } else {
      compiler.hooks.run.tapAsync(plugin, (compilation, callback) => {
        (0, _linter.default)(lint, options, compilation, callback);
      });
      /* istanbul ignore next */

      compiler.hooks.watchRun.tapAsync(plugin, (compilation, callback) => {
        (0, _linter.default)(lint, options, compilation, callback);
      });
    }
  }

  getContext(compiler) {
    if (!this.options.context) {
      return compiler.options.context;
    }

    if (!(0, _path.isAbsolute)(this.options.context)) {
      return (0, _path.join)(compiler.options.context, this.options.context);
    }

    return this.options.context;
  }

}

var _default = StylelintWebpackPlugin;
exports.default = _default;
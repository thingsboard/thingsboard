"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.default = loader;

var _schemaUtils = _interopRequireDefault(require("schema-utils"));

var _postcss = _interopRequireDefault(require("postcss"));

var _package = _interopRequireDefault(require("postcss/package.json"));

var _loaderUtils = require("loader-utils");

var _options = _interopRequireDefault(require("./options.json"));

var _plugins = require("./plugins");

var _utils = require("./utils");

var _Warning = _interopRequireDefault(require("./Warning"));

var _CssSyntaxError = _interopRequireDefault(require("./CssSyntaxError"));

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

/*
  MIT License http://www.opensource.org/licenses/mit-license.php
  Author Tobias Koppers @sokra
*/
function loader(content, map, meta) {
  const options = (0, _loaderUtils.getOptions)(this) || {};
  (0, _schemaUtils.default)(_options.default, options, {
    name: 'CSS Loader',
    baseDataPath: 'options'
  });
  const callback = this.async();
  const sourceMap = options.sourceMap || false;
  const plugins = [];

  if (options.modules) {
    plugins.push(...(0, _utils.getModulesPlugins)(options, this));
  }

  const exportType = options.onlyLocals ? 'locals' : 'full';
  plugins.push((0, _plugins.icssParser)());

  if (options.import !== false && exportType === 'full') {
    plugins.push((0, _plugins.importParser)({
      filter: (0, _utils.getFilter)(options.import, this.resourcePath)
    }));
  }

  if (options.url !== false && exportType === 'full') {
    plugins.push((0, _plugins.urlParser)({
      filter: (0, _utils.getFilter)(options.url, this.resourcePath, value => (0, _loaderUtils.isUrlRequest)(value))
    }));
  } // Reuse CSS AST (PostCSS AST e.g 'postcss-loader') to avoid reparsing


  if (meta) {
    const {
      ast
    } = meta;

    if (ast && ast.type === 'postcss' && ast.version === _package.default.version) {
      // eslint-disable-next-line no-param-reassign
      content = ast.root;
    }
  }

  (0, _postcss.default)(plugins).process(content, {
    from: this.remainingRequest.split('!').pop(),
    to: this.currentRequest.split('!').pop(),
    map: options.sourceMap ? {
      // Some loaders (example `"postcss-loader": "1.x.x"`) always generates source map, we should remove it
      prev: sourceMap && map ? (0, _utils.normalizeSourceMap)(map) : null,
      inline: false,
      annotation: false
    } : false
  }).then(result => {
    result.warnings().forEach(warning => this.emitWarning(new _Warning.default(warning)));
    const imports = [];
    const exports = [];
    const replacers = [];

    for (const message of result.messages) {
      // eslint-disable-next-line default-case
      switch (message.type) {
        case 'import':
          imports.push(message.value);
          break;

        case 'export':
          exports.push(message.value);
          break;

        case 'replacer':
          replacers.push(message.value);
          break;
      }
    }

    const {
      importLoaders,
      localsConvention
    } = options;
    const esModule = typeof options.esModule !== 'undefined' ? options.esModule : false;
    const importCode = (0, _utils.getImportCode)(this, imports, exportType, sourceMap, importLoaders, esModule);
    const moduleCode = (0, _utils.getModuleCode)(this, result, exportType, sourceMap, replacers);
    const exportCode = (0, _utils.getExportCode)(this, exports, exportType, replacers, localsConvention, esModule);
    return callback(null, [importCode, moduleCode, exportCode].join(''));
  }).catch(error => {
    callback(error.name === 'CssSyntaxError' ? new _CssSyntaxError.default(error) : error);
  });
}
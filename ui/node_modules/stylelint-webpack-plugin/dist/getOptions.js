"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.default = getOptions;

var _schemaUtils = _interopRequireDefault(require("schema-utils"));

var _options = _interopRequireDefault(require("./options.json"));

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

function getOptions(pluginOptions) {
  const options = {
    files: '**/*.s?(c|a)ss',
    formatter: 'string',
    stylelintPath: 'stylelint',
    ...pluginOptions
  };
  (0, _schemaUtils.default)(_options.default, options, {
    name: 'Stylelint Webpack Plugin',
    baseDataPath: 'options'
  }); // eslint-disable-next-line

  const stylelint = require(options.stylelintPath);

  options.formatter = getFormatter(stylelint, options.formatter);
  return options;
}

function getFormatter({
  formatters
}, formatter) {
  if (typeof formatter === 'function') {
    return formatter;
  } // Try to get oficial formatter


  if (typeof formatter === 'string' && typeof formatters[formatter] === 'function') {
    return formatters[formatter];
  }

  return formatters.string;
}
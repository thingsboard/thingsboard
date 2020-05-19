"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.default = void 0;

var _less = _interopRequireDefault(require("less"));

var _pify = _interopRequireDefault(require("pify"));

var _processResult = _interopRequireDefault(require("./processResult"));

var _getOptions = _interopRequireDefault(require("./getOptions"));

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

const render = (0, _pify.default)(_less.default.render.bind(_less.default));

function lessLoader(source) {
  const loaderContext = this;
  const options = (0, _getOptions.default)(loaderContext);
  const done = loaderContext.async();
  const isSync = typeof done !== 'function';

  if (isSync) {
    throw new Error('Synchronous compilation is not supported anymore. See https://github.com/webpack-contrib/less-loader/issues/84');
  }

  (0, _processResult.default)(loaderContext, render(source, options));
}

var _default = lessLoader;
exports.default = _default;
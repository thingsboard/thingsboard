"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports["default"] = void 0;

var _stylelint = require("stylelint");

var _utils = require("./utils");

var _rules = _interopRequireDefault(require("./rules"));

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { "default": obj }; }

var rulesPlugins = Object.keys(_rules["default"]).map(function (ruleName) {
  return (0, _stylelint.createPlugin)((0, _utils.namespace)(ruleName), _rules["default"][ruleName]);
});
var _default = rulesPlugins;
exports["default"] = _default;
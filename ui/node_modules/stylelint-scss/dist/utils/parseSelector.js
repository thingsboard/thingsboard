"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports["default"] = _default;

var _postcssSelectorParser = _interopRequireDefault(require("postcss-selector-parser"));

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { "default": obj }; }

function _default(selector, result, node, cb) {
  try {
    (0, _postcssSelectorParser["default"])(cb).process(selector);
  } catch (e) {
    result.warn("Cannot parse selector", {
      node: node
    });
  }
}
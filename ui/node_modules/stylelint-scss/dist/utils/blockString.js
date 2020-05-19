"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports["default"] = _default;

var _beforeBlockString = _interopRequireDefault(require("./beforeBlockString"));

var _hasBlock = _interopRequireDefault(require("./hasBlock"));

var _rawNodeString = _interopRequireDefault(require("./rawNodeString"));

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { "default": obj }; }

/**
 * Return a CSS statement's block -- the string that starts with `{` and ends with `}`.
 *
 * If the statement has no block (e.g. `@import url(foo.css);`),
 * return undefined.
 *
 * @param {Rule|AtRule} statement - postcss rule or at-rule node
 * @return {string|undefined}
 */
function _default(statement) {
  if (!(0, _hasBlock["default"])(statement)) {
    return;
  }

  return (0, _rawNodeString["default"])(statement).slice((0, _beforeBlockString["default"])(statement).length);
}
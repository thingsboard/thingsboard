"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports["default"] = _default;

var _isStandardSyntaxProperty = _interopRequireDefault(require("stylelint/lib/utils/isStandardSyntaxProperty"));

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { "default": obj }; }

/**
 * Check whether a property is standard
 *
 * @param {string} property
 * @return {boolean} If `true`, the property is standard
 */
function _default(property) {
  return (0, _isStandardSyntaxProperty["default"])(property);
}
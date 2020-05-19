"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports["default"] = _default;

var _isStandardSyntaxRule = _interopRequireDefault(require("stylelint/lib/utils/isStandardSyntaxRule"));

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { "default": obj }; }

/**
 * Check whether a rule is standard
 *
 * @param {Rule} postcss rule node
 * @return {boolean} If `true`, the rule is standard
 */
function _default(rule) {
  return (0, _isStandardSyntaxRule["default"])(rule);
}
"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports["default"] = _default;

var _isStandardSyntaxSelector = _interopRequireDefault(require("stylelint/lib/utils/isStandardSyntaxSelector"));

var _hasInterpolation = _interopRequireDefault(require("stylelint/lib/utils/hasInterpolation"));

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { "default": obj }; }

/**
 * Check whether a selector is standard
 *
 * @param {string} selector
 * @return {boolean} If `true`, the selector is standard
 */
function _default(selector) {
  var standardSyntaxSelector = (0, _isStandardSyntaxSelector["default"])(selector); // SCSS placeholder selectors

  if (!standardSyntaxSelector) {
    if (selector.indexOf("%") === 0 && !(0, _hasInterpolation["default"])(selector)) {
      return true;
    }
  }

  return standardSyntaxSelector;
}
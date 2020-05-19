"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports["default"] = _default;

/**
 * Check if a string is a single line (i.e. does not contain
 * any newline characters).
 *
 * @param {string} input
 * @return {boolean}
 */
function _default(input) {
  return !/[\n\r]/.test(input);
}
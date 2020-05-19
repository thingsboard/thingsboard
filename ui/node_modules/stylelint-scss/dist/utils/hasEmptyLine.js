"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports["default"] = _default;

/**
 * Check if a string contains at least one empty line
 *
 * @param {string} input
 * @return {boolean}
 */
function _default(string) {
  return string && (string.indexOf("\n\n") !== -1 || string.indexOf("\n\r\n") !== -1);
}
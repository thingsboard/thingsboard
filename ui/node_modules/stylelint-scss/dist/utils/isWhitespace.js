"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports["default"] = _default;

/**
 * Check if a character is whitespace.
 *
 * @param {string} char - A single character
 * @return {boolean}
 */
function _default(_char) {
  return [" ", "\n", "\t", "\r", "\f"].indexOf(_char) !== -1;
}
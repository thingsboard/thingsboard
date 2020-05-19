"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports["default"] = _default;

/**
 * Check if an options object contains a certain `except` keyword.
 * It will look for an `except` property whose value should
 * be an array of keywords.
 *
 * @param {object} options
 * @param {string} exceptionName
 * @return {boolean}
 */
function _default(options, exceptionName) {
  return options && options.except && options.except.indexOf(exceptionName) !== -1;
}
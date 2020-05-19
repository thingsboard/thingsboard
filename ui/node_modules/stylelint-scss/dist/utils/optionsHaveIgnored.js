"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports["default"] = _default;

/**
 * Check if an options object contains a certain `ignore` keyword.
 * It will look for an `ignore` property whose value should
 * be an array of keywords.
 *
 * @param {object} options
 * @param {string} ignoredName
 * @return {boolean}
 */
function _default(options, ignoredName) {
  return options && options.ignore && options.ignore.indexOf(ignoredName) !== -1;
}
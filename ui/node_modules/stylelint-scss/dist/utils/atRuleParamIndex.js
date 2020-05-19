"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports["default"] = _default;

/**
 * Get the index of a media query's params
 *
 * @param {AtRule} atRule
 * @return {int} The index
 */
function _default(atRule) {
  // Initial 1 is for the `@`
  var index = 1 + atRule.name.length;

  if (atRule.raw("afterName")) {
    index += atRule.raw("afterName").length;
  }

  return index;
}
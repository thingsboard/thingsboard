"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports["default"] = _default;

/**
 * Get an at rule's base name
 *
 * @param {AtRule} atRule
 * @return {string} The name
 */
function _default(atRule) {
  return atRule.params.replace(/\([^)]*\)/, "").trim();
}
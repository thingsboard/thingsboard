"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports["default"] = _default;

/**
 * Check if a statement has an block (empty or otherwise).
 *
 * @param {Rule|AtRule} statement - postcss rule or at-rule node
 * @return {boolean} True if `statement` has a block (empty or otherwise)
 */
function _default(statement) {
  return statement.nodes !== undefined;
}
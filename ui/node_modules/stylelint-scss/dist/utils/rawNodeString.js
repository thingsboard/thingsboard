"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports["default"] = _default;

/**
 * Stringify PostCSS node including its raw "before" string.
 *
 * @param {Node} node - Any PostCSS node
 * @return {string}
 */
function _default(node) {
  var result = "";

  if (node.raws.before) {
    result += node.raws.before;
  }

  result += node.toString();
  return result;
}
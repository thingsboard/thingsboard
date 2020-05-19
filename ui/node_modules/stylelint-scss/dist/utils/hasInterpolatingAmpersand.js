"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports["default"] = _default;

/**
 * Check whether a selector has an interpolating ampersand
 * An "interpolating ampersand" is an "&" used to interpolate within another
 * simple selector (e.g. `&-modifier`), rather than an "&" that stands
 * on its own as a simple selector (e.g. `& .child`)
 *
 * @param {string} selector
 * @return {boolean} If `true`, the selector has an interpolating ampersand
 */
function _default(selector) {
  for (var i = 0; i < selector.length; i++) {
    if (selector[i] !== "&") {
      continue;
    }

    if (selector[i - 1] !== undefined && !isCombinator(selector[i - 1])) {
      return true;
    }

    if (selector[i + 1] !== undefined && !isCombinator(selector[i + 1])) {
      return true;
    }
  }

  return false;
}

function isCombinator(x) {
  return /[\s+>~]/.test(x);
}
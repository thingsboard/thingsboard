"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports["default"] = _default;

/**
 * Create configurationError from text and set CLI exit code
 *
 * @param {string} text
 * @return {Error} - The error, with text and exit code
 */
function _default(text) {
  var err = new Error(text);
  err.code = 78;
  return err;
}
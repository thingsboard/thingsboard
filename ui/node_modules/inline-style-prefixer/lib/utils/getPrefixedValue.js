"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});

exports.default = function (prefixedValue, value, keepUnprefixed) {
  return keepUnprefixed ? [prefixedValue, value] : prefixedValue;
};

module.exports = exports["default"];
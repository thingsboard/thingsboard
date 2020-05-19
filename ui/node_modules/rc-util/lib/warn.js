"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.default = warn;

function warn(msg) {
  if (process.env.NODE_ENV !== 'production') {
    if (typeof console !== 'undefined' && console.warn) {
      console.warn(msg);
    }
  }
}
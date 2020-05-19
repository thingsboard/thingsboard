'use strict';

Object.defineProperty(exports, "__esModule", {
  value: true
});

exports.default = function () {
  return process.platform === 'win32' || /^(msys|cygwin)$/.test(process.env.OSTYPE);
};
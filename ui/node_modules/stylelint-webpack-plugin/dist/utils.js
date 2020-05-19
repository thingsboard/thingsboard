"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.parseFiles = parseFiles;
exports.replaceBackslashes = replaceBackslashes;

var _path = require("path");

var _arrify = _interopRequireDefault(require("arrify"));

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

function parseFiles(files, context) {
  return (0, _arrify.default)(files).map(file => replaceBackslashes((0, _path.join)(context, '/', file)));
}

function replaceBackslashes(str) {
  return str.replace(/\\/g, '/');
}
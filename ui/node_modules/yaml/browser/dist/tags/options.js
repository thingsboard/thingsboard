"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.strOptions = exports.nullOptions = exports.boolOptions = exports.binaryOptions = void 0;

var _constants = require("../constants");

var binaryOptions = {
  defaultType: _constants.Type.BLOCK_LITERAL,
  lineWidth: 76
};
exports.binaryOptions = binaryOptions;
var boolOptions = {
  trueStr: 'true',
  falseStr: 'false'
};
exports.boolOptions = boolOptions;
var nullOptions = {
  nullStr: 'null'
};
exports.nullOptions = nullOptions;
var strOptions = {
  defaultType: _constants.Type.PLAIN,
  doubleQuoted: {
    jsonEncoding: false,
    minMultiLineLength: 40
  },
  fold: {
    lineWidth: 80,
    minContentWidth: 20
  }
};
exports.strOptions = strOptions;
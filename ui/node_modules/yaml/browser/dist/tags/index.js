"use strict";

var _interopRequireDefault = require("@babel/runtime/helpers/interopRequireDefault");

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.tags = exports.schemas = void 0;

var _core = _interopRequireDefault(require("./core"));

var _failsafe = _interopRequireDefault(require("./failsafe"));

var _json = _interopRequireDefault(require("./json"));

var _yaml = _interopRequireDefault(require("./yaml-1.1"));

var _map = _interopRequireDefault(require("./failsafe/map"));

var _seq = _interopRequireDefault(require("./failsafe/seq"));

var _binary = _interopRequireDefault(require("./yaml-1.1/binary"));

var _omap = _interopRequireDefault(require("./yaml-1.1/omap"));

var _pairs = _interopRequireDefault(require("./yaml-1.1/pairs"));

var _set = _interopRequireDefault(require("./yaml-1.1/set"));

var _timestamp = require("./yaml-1.1/timestamp");

var schemas = {
  core: _core.default,
  failsafe: _failsafe.default,
  json: _json.default,
  yaml11: _yaml.default
};
exports.schemas = schemas;
var tags = {
  binary: _binary.default,
  floatTime: _timestamp.floatTime,
  intTime: _timestamp.intTime,
  map: _map.default,
  omap: _omap.default,
  pairs: _pairs.default,
  seq: _seq.default,
  set: _set.default,
  timestamp: _timestamp.timestamp
};
exports.tags = tags;
"use strict";

var _interopRequireDefault = require("@babel/runtime/helpers/interopRequireDefault");

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.default = void 0;

var _Scalar = _interopRequireDefault(require("../../schema/Scalar"));

var _stringify = require("../../stringify");

var _failsafe = _interopRequireDefault(require("../failsafe"));

var _options = require("../options");

var _binary = _interopRequireDefault(require("./binary"));

var _omap = _interopRequireDefault(require("./omap"));

var _pairs = _interopRequireDefault(require("./pairs"));

var _set = _interopRequireDefault(require("./set"));

var _timestamp = require("./timestamp");

var _default = _failsafe.default.concat([{
  identify: function identify(value) {
    return value == null;
  },
  createNode: function createNode(schema, value, ctx) {
    return ctx.wrapScalars ? new _Scalar.default(null) : null;
  },
  default: true,
  tag: 'tag:yaml.org,2002:null',
  test: /^(?:~|[Nn]ull|NULL)?$/,
  resolve: function resolve() {
    return null;
  },
  options: _options.nullOptions,
  stringify: function stringify() {
    return _options.nullOptions.nullStr;
  }
}, {
  identify: function identify(value) {
    return typeof value === 'boolean';
  },
  default: true,
  tag: 'tag:yaml.org,2002:bool',
  test: /^(?:Y|y|[Yy]es|YES|[Tt]rue|TRUE|[Oo]n|ON)$/,
  resolve: function resolve() {
    return true;
  },
  options: _options.boolOptions,
  stringify: function stringify(_ref) {
    var value = _ref.value;
    return value ? _options.boolOptions.trueStr : _options.boolOptions.falseStr;
  }
}, {
  identify: function identify(value) {
    return typeof value === 'boolean';
  },
  default: true,
  tag: 'tag:yaml.org,2002:bool',
  test: /^(?:N|n|[Nn]o|NO|[Ff]alse|FALSE|[Oo]ff|OFF)$/i,
  resolve: function resolve() {
    return false;
  },
  options: _options.boolOptions,
  stringify: function stringify(_ref2) {
    var value = _ref2.value;
    return value ? _options.boolOptions.trueStr : _options.boolOptions.falseStr;
  }
}, {
  identify: function identify(value) {
    return typeof value === 'number';
  },
  default: true,
  tag: 'tag:yaml.org,2002:int',
  format: 'BIN',
  test: /^0b([0-1_]+)$/,
  resolve: function resolve(str, bin) {
    return parseInt(bin.replace(/_/g, ''), 2);
  },
  stringify: function stringify(_ref3) {
    var value = _ref3.value;
    return '0b' + value.toString(2);
  }
}, {
  identify: function identify(value) {
    return typeof value === 'number';
  },
  default: true,
  tag: 'tag:yaml.org,2002:int',
  format: 'OCT',
  test: /^[-+]?0([0-7_]+)$/,
  resolve: function resolve(str, oct) {
    return parseInt(oct.replace(/_/g, ''), 8);
  },
  stringify: function stringify(_ref4) {
    var value = _ref4.value;
    return (value < 0 ? '-0' : '0') + value.toString(8);
  }
}, {
  identify: function identify(value) {
    return typeof value === 'number';
  },
  default: true,
  tag: 'tag:yaml.org,2002:int',
  test: /^[-+]?[0-9][0-9_]*$/,
  resolve: function resolve(str) {
    return parseInt(str.replace(/_/g, ''), 10);
  },
  stringify: _stringify.stringifyNumber
}, {
  identify: function identify(value) {
    return typeof value === 'number';
  },
  default: true,
  tag: 'tag:yaml.org,2002:int',
  format: 'HEX',
  test: /^0x([0-9a-fA-F_]+)$/,
  resolve: function resolve(str, hex) {
    return parseInt(hex.replace(/_/g, ''), 16);
  },
  stringify: function stringify(_ref5) {
    var value = _ref5.value;
    return (value < 0 ? '-0x' : '0x') + value.toString(16);
  }
}, {
  identify: function identify(value) {
    return typeof value === 'number';
  },
  default: true,
  tag: 'tag:yaml.org,2002:float',
  test: /^(?:[-+]?\.inf|(\.nan))$/i,
  resolve: function resolve(str, nan) {
    return nan ? NaN : str[0] === '-' ? Number.NEGATIVE_INFINITY : Number.POSITIVE_INFINITY;
  },
  stringify: _stringify.stringifyNumber
}, {
  identify: function identify(value) {
    return typeof value === 'number';
  },
  default: true,
  tag: 'tag:yaml.org,2002:float',
  format: 'EXP',
  test: /^[-+]?([0-9][0-9_]*)?(\.[0-9_]*)?[eE][-+]?[0-9]+$/,
  resolve: function resolve(str) {
    return parseFloat(str.replace(/_/g, ''));
  },
  stringify: function stringify(_ref6) {
    var value = _ref6.value;
    return Number(value).toExponential();
  }
}, {
  identify: function identify(value) {
    return typeof value === 'number';
  },
  default: true,
  tag: 'tag:yaml.org,2002:float',
  test: /^[-+]?(?:[0-9][0-9_]*)?\.([0-9_]*)$/,
  resolve: function resolve(str, frac) {
    var node = new _Scalar.default(parseFloat(str.replace(/_/g, '')));

    if (frac) {
      var f = frac.replace(/_/g, '');
      if (f[f.length - 1] === '0') node.minFractionDigits = f.length;
    }

    return node;
  },
  stringify: _stringify.stringifyNumber
}], _binary.default, _omap.default, _pairs.default, _set.default, _timestamp.intTime, _timestamp.floatTime, _timestamp.timestamp);

exports.default = _default;
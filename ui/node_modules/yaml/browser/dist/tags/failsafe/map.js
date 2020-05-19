"use strict";

var _interopRequireDefault = require("@babel/runtime/helpers/interopRequireDefault");

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.default = void 0;

var _typeof2 = _interopRequireDefault(require("@babel/runtime/helpers/typeof"));

var _slicedToArray2 = _interopRequireDefault(require("@babel/runtime/helpers/slicedToArray"));

var _Map = _interopRequireDefault(require("../../schema/Map"));

var _parseMap = _interopRequireDefault(require("../../schema/parseMap"));

function createMap(schema, obj, ctx) {
  var map = new _Map.default();

  if (obj instanceof Map) {
    var _iteratorNormalCompletion = true;
    var _didIteratorError = false;
    var _iteratorError = undefined;

    try {
      for (var _iterator = obj[Symbol.iterator](), _step; !(_iteratorNormalCompletion = (_step = _iterator.next()).done); _iteratorNormalCompletion = true) {
        var _step$value = (0, _slicedToArray2.default)(_step.value, 2),
            key = _step$value[0],
            value = _step$value[1];

        map.items.push(schema.createPair(key, value, ctx));
      }
    } catch (err) {
      _didIteratorError = true;
      _iteratorError = err;
    } finally {
      try {
        if (!_iteratorNormalCompletion && _iterator.return != null) {
          _iterator.return();
        }
      } finally {
        if (_didIteratorError) {
          throw _iteratorError;
        }
      }
    }
  } else if (obj && (0, _typeof2.default)(obj) === 'object') {
    for (var _i = 0, _Object$keys = Object.keys(obj); _i < _Object$keys.length; _i++) {
      var _key = _Object$keys[_i];
      map.items.push(schema.createPair(_key, obj[_key], ctx));
    }
  }

  return map;
}

var _default = {
  createNode: createMap,
  default: true,
  nodeClass: _Map.default,
  tag: 'tag:yaml.org,2002:map',
  resolve: _parseMap.default
};
exports.default = _default;
"use strict";

var _interopRequireDefault = require("@babel/runtime/helpers/interopRequireDefault");

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.default = exports.YAMLOMap = void 0;

var _classCallCheck2 = _interopRequireDefault(require("@babel/runtime/helpers/classCallCheck"));

var _createClass2 = _interopRequireDefault(require("@babel/runtime/helpers/createClass"));

var _possibleConstructorReturn2 = _interopRequireDefault(require("@babel/runtime/helpers/possibleConstructorReturn"));

var _getPrototypeOf2 = _interopRequireDefault(require("@babel/runtime/helpers/getPrototypeOf"));

var _assertThisInitialized2 = _interopRequireDefault(require("@babel/runtime/helpers/assertThisInitialized"));

var _inherits2 = _interopRequireDefault(require("@babel/runtime/helpers/inherits"));

var _defineProperty2 = _interopRequireDefault(require("@babel/runtime/helpers/defineProperty"));

var _errors = require("../../errors");

var _toJSON2 = _interopRequireDefault(require("../../toJSON"));

var _Map = _interopRequireDefault(require("../../schema/Map"));

var _Pair = _interopRequireDefault(require("../../schema/Pair"));

var _Scalar = _interopRequireDefault(require("../../schema/Scalar"));

var _Seq = _interopRequireDefault(require("../../schema/Seq"));

var _pairs = require("./pairs");

var YAMLOMap =
/*#__PURE__*/
function (_YAMLSeq) {
  (0, _inherits2.default)(YAMLOMap, _YAMLSeq);

  function YAMLOMap() {
    var _this;

    (0, _classCallCheck2.default)(this, YAMLOMap);
    _this = (0, _possibleConstructorReturn2.default)(this, (0, _getPrototypeOf2.default)(YAMLOMap).call(this));
    (0, _defineProperty2.default)((0, _assertThisInitialized2.default)(_this), "add", _Map.default.prototype.add.bind((0, _assertThisInitialized2.default)(_this)));
    (0, _defineProperty2.default)((0, _assertThisInitialized2.default)(_this), "delete", _Map.default.prototype.delete.bind((0, _assertThisInitialized2.default)(_this)));
    (0, _defineProperty2.default)((0, _assertThisInitialized2.default)(_this), "get", _Map.default.prototype.get.bind((0, _assertThisInitialized2.default)(_this)));
    (0, _defineProperty2.default)((0, _assertThisInitialized2.default)(_this), "has", _Map.default.prototype.has.bind((0, _assertThisInitialized2.default)(_this)));
    (0, _defineProperty2.default)((0, _assertThisInitialized2.default)(_this), "set", _Map.default.prototype.set.bind((0, _assertThisInitialized2.default)(_this)));
    _this.tag = YAMLOMap.tag;
    return _this;
  }

  (0, _createClass2.default)(YAMLOMap, [{
    key: "toJSON",
    value: function toJSON(_, ctx) {
      var map = new Map();
      if (ctx && ctx.onCreate) ctx.onCreate(map);
      var _iteratorNormalCompletion = true;
      var _didIteratorError = false;
      var _iteratorError = undefined;

      try {
        for (var _iterator = this.items[Symbol.iterator](), _step; !(_iteratorNormalCompletion = (_step = _iterator.next()).done); _iteratorNormalCompletion = true) {
          var pair = _step.value;
          var key = void 0,
              value = void 0;

          if (pair instanceof _Pair.default) {
            key = (0, _toJSON2.default)(pair.key, '', ctx);
            value = (0, _toJSON2.default)(pair.value, key, ctx);
          } else {
            key = (0, _toJSON2.default)(pair, '', ctx);
          }

          if (map.has(key)) throw new Error('Ordered maps must not include duplicate keys');
          map.set(key, value);
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

      return map;
    }
  }]);
  return YAMLOMap;
}(_Seq.default);

exports.YAMLOMap = YAMLOMap;
(0, _defineProperty2.default)(YAMLOMap, "tag", 'tag:yaml.org,2002:omap');

function parseOMap(doc, cst) {
  var pairs = (0, _pairs.parsePairs)(doc, cst);
  var seenKeys = [];
  var _iteratorNormalCompletion2 = true;
  var _didIteratorError2 = false;
  var _iteratorError2 = undefined;

  try {
    for (var _iterator2 = pairs.items[Symbol.iterator](), _step2; !(_iteratorNormalCompletion2 = (_step2 = _iterator2.next()).done); _iteratorNormalCompletion2 = true) {
      var key = _step2.value.key;

      if (key instanceof _Scalar.default) {
        if (seenKeys.includes(key.value)) {
          var msg = 'Ordered maps must not include duplicate keys';
          throw new _errors.YAMLSemanticError(cst, msg);
        } else {
          seenKeys.push(key.value);
        }
      }
    }
  } catch (err) {
    _didIteratorError2 = true;
    _iteratorError2 = err;
  } finally {
    try {
      if (!_iteratorNormalCompletion2 && _iterator2.return != null) {
        _iterator2.return();
      }
    } finally {
      if (_didIteratorError2) {
        throw _iteratorError2;
      }
    }
  }

  return Object.assign(new YAMLOMap(), pairs);
}

function createOMap(schema, iterable, ctx) {
  var pairs = (0, _pairs.createPairs)(schema, iterable, ctx);
  var omap = new YAMLOMap();
  omap.items = pairs.items;
  return omap;
}

var _default = {
  identify: function identify(value) {
    return value instanceof Map;
  },
  nodeClass: YAMLOMap,
  default: false,
  tag: 'tag:yaml.org,2002:omap',
  resolve: parseOMap,
  createNode: createOMap
};
exports.default = _default;
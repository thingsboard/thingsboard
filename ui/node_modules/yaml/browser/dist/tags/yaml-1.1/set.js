"use strict";

var _interopRequireWildcard = require("@babel/runtime/helpers/interopRequireWildcard");

var _interopRequireDefault = require("@babel/runtime/helpers/interopRequireDefault");

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.default = exports.YAMLSet = void 0;

var _typeof2 = _interopRequireDefault(require("@babel/runtime/helpers/typeof"));

var _classCallCheck2 = _interopRequireDefault(require("@babel/runtime/helpers/classCallCheck"));

var _createClass2 = _interopRequireDefault(require("@babel/runtime/helpers/createClass"));

var _possibleConstructorReturn2 = _interopRequireDefault(require("@babel/runtime/helpers/possibleConstructorReturn"));

var _getPrototypeOf2 = _interopRequireDefault(require("@babel/runtime/helpers/getPrototypeOf"));

var _get2 = _interopRequireDefault(require("@babel/runtime/helpers/get"));

var _inherits2 = _interopRequireDefault(require("@babel/runtime/helpers/inherits"));

var _defineProperty2 = _interopRequireDefault(require("@babel/runtime/helpers/defineProperty"));

var _errors = require("../../errors");

var _Map = _interopRequireWildcard(require("../../schema/Map"));

var _Pair = _interopRequireDefault(require("../../schema/Pair"));

var _parseMap = _interopRequireDefault(require("../../schema/parseMap"));

var _Scalar = _interopRequireDefault(require("../../schema/Scalar"));

var YAMLSet =
/*#__PURE__*/
function (_YAMLMap) {
  (0, _inherits2.default)(YAMLSet, _YAMLMap);

  function YAMLSet() {
    var _this;

    (0, _classCallCheck2.default)(this, YAMLSet);
    _this = (0, _possibleConstructorReturn2.default)(this, (0, _getPrototypeOf2.default)(YAMLSet).call(this));
    _this.tag = YAMLSet.tag;
    return _this;
  }

  (0, _createClass2.default)(YAMLSet, [{
    key: "add",
    value: function add(key) {
      var pair = key instanceof _Pair.default ? key : new _Pair.default(key);
      var prev = (0, _Map.findPair)(this.items, pair.key);
      if (!prev) this.items.push(pair);
    }
  }, {
    key: "get",
    value: function get(key, keepPair) {
      var pair = (0, _Map.findPair)(this.items, key);
      return !keepPair && pair instanceof _Pair.default ? pair.key instanceof _Scalar.default ? pair.key.value : pair.key : pair;
    }
  }, {
    key: "set",
    value: function set(key, value) {
      if (typeof value !== 'boolean') throw new Error("Expected boolean value for set(key, value) in a YAML set, not ".concat((0, _typeof2.default)(value)));
      var prev = (0, _Map.findPair)(this.items, key);

      if (prev && !value) {
        this.items.splice(this.items.indexOf(prev), 1);
      } else if (!prev && value) {
        this.items.push(new _Pair.default(key));
      }
    }
  }, {
    key: "toJSON",
    value: function toJSON(_, ctx) {
      return (0, _get2.default)((0, _getPrototypeOf2.default)(YAMLSet.prototype), "toJSON", this).call(this, _, ctx, Set);
    }
  }, {
    key: "toString",
    value: function toString(ctx, onComment, onChompKeep) {
      if (!ctx) return JSON.stringify(this);
      if (this.hasAllNullValues()) return (0, _get2.default)((0, _getPrototypeOf2.default)(YAMLSet.prototype), "toString", this).call(this, ctx, onComment, onChompKeep);else throw new Error('Set items must all have null values');
    }
  }]);
  return YAMLSet;
}(_Map.default);

exports.YAMLSet = YAMLSet;
(0, _defineProperty2.default)(YAMLSet, "tag", 'tag:yaml.org,2002:set');

function parseSet(doc, cst) {
  var map = (0, _parseMap.default)(doc, cst);
  if (!map.hasAllNullValues()) throw new _errors.YAMLSemanticError(cst, 'Set items must all have null values');
  return Object.assign(new YAMLSet(), map);
}

function createSet(schema, iterable, ctx) {
  var set = new YAMLSet();
  var _iteratorNormalCompletion = true;
  var _didIteratorError = false;
  var _iteratorError = undefined;

  try {
    for (var _iterator = iterable[Symbol.iterator](), _step; !(_iteratorNormalCompletion = (_step = _iterator.next()).done); _iteratorNormalCompletion = true) {
      var value = _step.value;
      set.items.push(schema.createPair(value, null, ctx));
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

  return set;
}

var _default = {
  identify: function identify(value) {
    return value instanceof Set;
  },
  nodeClass: YAMLSet,
  default: false,
  tag: 'tag:yaml.org,2002:set',
  resolve: parseSet,
  createNode: createSet
};
exports.default = _default;
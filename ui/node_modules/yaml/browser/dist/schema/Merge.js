"use strict";

var _interopRequireDefault = require("@babel/runtime/helpers/interopRequireDefault");

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.default = exports.MERGE_KEY = void 0;

var _slicedToArray2 = _interopRequireDefault(require("@babel/runtime/helpers/slicedToArray"));

var _classCallCheck2 = _interopRequireDefault(require("@babel/runtime/helpers/classCallCheck"));

var _createClass2 = _interopRequireDefault(require("@babel/runtime/helpers/createClass"));

var _possibleConstructorReturn2 = _interopRequireDefault(require("@babel/runtime/helpers/possibleConstructorReturn"));

var _getPrototypeOf2 = _interopRequireDefault(require("@babel/runtime/helpers/getPrototypeOf"));

var _get2 = _interopRequireDefault(require("@babel/runtime/helpers/get"));

var _inherits2 = _interopRequireDefault(require("@babel/runtime/helpers/inherits"));

var _Map = _interopRequireDefault(require("./Map"));

var _Pair2 = _interopRequireDefault(require("./Pair"));

var _Scalar = _interopRequireDefault(require("./Scalar"));

var _Seq = _interopRequireDefault(require("./Seq"));

var MERGE_KEY = '<<';
exports.MERGE_KEY = MERGE_KEY;

var Merge =
/*#__PURE__*/
function (_Pair) {
  (0, _inherits2.default)(Merge, _Pair);

  function Merge(pair) {
    var _this;

    (0, _classCallCheck2.default)(this, Merge);

    if (pair instanceof _Pair2.default) {
      var seq = pair.value;

      if (!(seq instanceof _Seq.default)) {
        seq = new _Seq.default();
        seq.items.push(pair.value);
        seq.range = pair.value.range;
      }

      _this = (0, _possibleConstructorReturn2.default)(this, (0, _getPrototypeOf2.default)(Merge).call(this, pair.key, seq));
      _this.range = pair.range;
    } else {
      _this = (0, _possibleConstructorReturn2.default)(this, (0, _getPrototypeOf2.default)(Merge).call(this, new _Scalar.default(MERGE_KEY), new _Seq.default()));
    }

    _this.type = 'MERGE_PAIR';
    return (0, _possibleConstructorReturn2.default)(_this);
  } // If the value associated with a merge key is a single mapping node, each of
  // its key/value pairs is inserted into the current mapping, unless the key
  // already exists in it. If the value associated with the merge key is a
  // sequence, then this sequence is expected to contain mapping nodes and each
  // of these nodes is merged in turn according to its order in the sequence.
  // Keys in mapping nodes earlier in the sequence override keys specified in
  // later mapping nodes. -- http://yaml.org/type/merge.html


  (0, _createClass2.default)(Merge, [{
    key: "addToJSMap",
    value: function addToJSMap(ctx, map) {
      var _iteratorNormalCompletion = true;
      var _didIteratorError = false;
      var _iteratorError = undefined;

      try {
        for (var _iterator = this.value.items[Symbol.iterator](), _step; !(_iteratorNormalCompletion = (_step = _iterator.next()).done); _iteratorNormalCompletion = true) {
          var source = _step.value.source;
          if (!(source instanceof _Map.default)) throw new Error('Merge sources must be maps');
          var srcMap = source.toJSON(null, ctx, Map);
          var _iteratorNormalCompletion2 = true;
          var _didIteratorError2 = false;
          var _iteratorError2 = undefined;

          try {
            for (var _iterator2 = srcMap[Symbol.iterator](), _step2; !(_iteratorNormalCompletion2 = (_step2 = _iterator2.next()).done); _iteratorNormalCompletion2 = true) {
              var _step2$value = (0, _slicedToArray2.default)(_step2.value, 2),
                  key = _step2$value[0],
                  value = _step2$value[1];

              if (map instanceof Map) {
                if (!map.has(key)) map.set(key, value);
              } else if (map instanceof Set) {
                map.add(key);
              } else {
                if (!Object.prototype.hasOwnProperty.call(map, key)) map[key] = value;
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
  }, {
    key: "toString",
    value: function toString(ctx, onComment) {
      var seq = this.value;
      if (seq.items.length > 1) return (0, _get2.default)((0, _getPrototypeOf2.default)(Merge.prototype), "toString", this).call(this, ctx, onComment);
      this.value = seq.items[0];
      var str = (0, _get2.default)((0, _getPrototypeOf2.default)(Merge.prototype), "toString", this).call(this, ctx, onComment);
      this.value = seq;
      return str;
    }
  }]);
  return Merge;
}(_Pair2.default);

exports.default = Merge;
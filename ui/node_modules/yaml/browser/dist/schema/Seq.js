"use strict";

var _interopRequireDefault = require("@babel/runtime/helpers/interopRequireDefault");

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.default = void 0;

var _classCallCheck2 = _interopRequireDefault(require("@babel/runtime/helpers/classCallCheck"));

var _createClass2 = _interopRequireDefault(require("@babel/runtime/helpers/createClass"));

var _possibleConstructorReturn2 = _interopRequireDefault(require("@babel/runtime/helpers/possibleConstructorReturn"));

var _getPrototypeOf2 = _interopRequireDefault(require("@babel/runtime/helpers/getPrototypeOf"));

var _get2 = _interopRequireDefault(require("@babel/runtime/helpers/get"));

var _inherits2 = _interopRequireDefault(require("@babel/runtime/helpers/inherits"));

var _toJSON2 = _interopRequireDefault(require("../toJSON"));

var _Collection2 = _interopRequireDefault(require("./Collection"));

var _Scalar = _interopRequireDefault(require("./Scalar"));

// Published as 'yaml/seq'
function asItemIndex(key) {
  var idx = key instanceof _Scalar.default ? key.value : key;
  if (idx && typeof idx === 'string') idx = Number(idx);
  return Number.isInteger(idx) && idx >= 0 ? idx : null;
}

var YAMLSeq =
/*#__PURE__*/
function (_Collection) {
  (0, _inherits2.default)(YAMLSeq, _Collection);

  function YAMLSeq() {
    (0, _classCallCheck2.default)(this, YAMLSeq);
    return (0, _possibleConstructorReturn2.default)(this, (0, _getPrototypeOf2.default)(YAMLSeq).apply(this, arguments));
  }

  (0, _createClass2.default)(YAMLSeq, [{
    key: "add",
    value: function add(value) {
      this.items.push(value);
    }
  }, {
    key: "delete",
    value: function _delete(key) {
      var idx = asItemIndex(key);
      if (typeof idx !== 'number') return false;
      var del = this.items.splice(idx, 1);
      return del.length > 0;
    }
  }, {
    key: "get",
    value: function get(key, keepScalar) {
      var idx = asItemIndex(key);
      if (typeof idx !== 'number') return undefined;
      var it = this.items[idx];
      return !keepScalar && it instanceof _Scalar.default ? it.value : it;
    }
  }, {
    key: "has",
    value: function has(key) {
      var idx = asItemIndex(key);
      return typeof idx === 'number' && idx < this.items.length;
    }
  }, {
    key: "set",
    value: function set(key, value) {
      var idx = asItemIndex(key);
      if (typeof idx !== 'number') throw new Error("Expected a valid index, not ".concat(key, "."));
      this.items[idx] = value;
    }
  }, {
    key: "toJSON",
    value: function toJSON(_, ctx) {
      var seq = [];
      if (ctx && ctx.onCreate) ctx.onCreate(seq);
      var i = 0;
      var _iteratorNormalCompletion = true;
      var _didIteratorError = false;
      var _iteratorError = undefined;

      try {
        for (var _iterator = this.items[Symbol.iterator](), _step; !(_iteratorNormalCompletion = (_step = _iterator.next()).done); _iteratorNormalCompletion = true) {
          var item = _step.value;
          seq.push((0, _toJSON2.default)(item, String(i++), ctx));
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

      return seq;
    }
  }, {
    key: "toString",
    value: function toString(ctx, onComment, onChompKeep) {
      if (!ctx) return JSON.stringify(this);
      return (0, _get2.default)((0, _getPrototypeOf2.default)(YAMLSeq.prototype), "toString", this).call(this, ctx, {
        blockItem: function blockItem(n) {
          return n.type === 'comment' ? n.str : "- ".concat(n.str);
        },
        flowChars: {
          start: '[',
          end: ']'
        },
        isMap: false,
        itemIndent: (ctx.indent || '') + '  '
      }, onComment, onChompKeep);
    }
  }]);
  return YAMLSeq;
}(_Collection2.default);

exports.default = YAMLSeq;
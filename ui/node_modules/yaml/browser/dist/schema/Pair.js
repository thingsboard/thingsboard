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

var _inherits2 = _interopRequireDefault(require("@babel/runtime/helpers/inherits"));

var _typeof2 = _interopRequireDefault(require("@babel/runtime/helpers/typeof"));

var _addComment = _interopRequireDefault(require("../addComment"));

var _constants = require("../constants");

var _toJSON = _interopRequireDefault(require("../toJSON"));

var _Collection = _interopRequireDefault(require("./Collection"));

var _Node2 = _interopRequireDefault(require("./Node"));

var _Scalar = _interopRequireDefault(require("./Scalar"));

// Published as 'yaml/pair'
var stringifyKey = function stringifyKey(key, jsKey, ctx) {
  if (jsKey === null) return '';
  if ((0, _typeof2.default)(jsKey) !== 'object') return String(jsKey);
  if (key instanceof _Node2.default && ctx && ctx.doc) return key.toString({
    anchors: {},
    doc: ctx.doc,
    indent: '',
    inFlow: true,
    inStringifyKey: true
  });
  return JSON.stringify(jsKey);
};

var Pair =
/*#__PURE__*/
function (_Node) {
  (0, _inherits2.default)(Pair, _Node);

  function Pair(key) {
    var _this;

    var value = arguments.length > 1 && arguments[1] !== undefined ? arguments[1] : null;
    (0, _classCallCheck2.default)(this, Pair);
    _this = (0, _possibleConstructorReturn2.default)(this, (0, _getPrototypeOf2.default)(Pair).call(this));
    _this.key = key;
    _this.value = value;
    _this.type = 'PAIR';
    return _this;
  }

  (0, _createClass2.default)(Pair, [{
    key: "addToJSMap",
    value: function addToJSMap(ctx, map) {
      var key = (0, _toJSON.default)(this.key, '', ctx);

      if (map instanceof Map) {
        var value = (0, _toJSON.default)(this.value, key, ctx);
        map.set(key, value);
      } else if (map instanceof Set) {
        map.add(key);
      } else {
        var stringKey = stringifyKey(this.key, key, ctx);
        map[stringKey] = (0, _toJSON.default)(this.value, stringKey, ctx);
      }

      return map;
    }
  }, {
    key: "toJSON",
    value: function toJSON(_, ctx) {
      var pair = ctx && ctx.mapAsMap ? new Map() : {};
      return this.addToJSMap(ctx, pair);
    }
  }, {
    key: "toString",
    value: function toString(ctx, onComment, onChompKeep) {
      if (!ctx || !ctx.doc) return JSON.stringify(this);
      var simpleKeys = ctx.doc.options.simpleKeys;
      var key = this.key,
          value = this.value;
      var keyComment = key instanceof _Node2.default && key.comment;

      if (simpleKeys) {
        if (keyComment) {
          throw new Error('With simple keys, key nodes cannot have comments');
        }

        if (key instanceof _Collection.default) {
          var msg = 'With simple keys, collection cannot be used as a key value';
          throw new Error(msg);
        }
      }

      var explicitKey = !simpleKeys && (!key || keyComment || key instanceof _Collection.default || key.type === _constants.Type.BLOCK_FOLDED || key.type === _constants.Type.BLOCK_LITERAL);
      var _ctx = ctx,
          doc = _ctx.doc,
          indent = _ctx.indent;
      ctx = Object.assign({}, ctx, {
        implicitKey: !explicitKey,
        indent: indent + '  '
      });
      var chompKeep = false;
      var str = doc.schema.stringify(key, ctx, function () {
        return keyComment = null;
      }, function () {
        return chompKeep = true;
      });
      str = (0, _addComment.default)(str, ctx.indent, keyComment);

      if (ctx.allNullValues && !simpleKeys) {
        if (this.comment) {
          str = (0, _addComment.default)(str, ctx.indent, this.comment);
          if (onComment) onComment();
        } else if (chompKeep && !keyComment && onChompKeep) onChompKeep();

        return ctx.inFlow ? str : "? ".concat(str);
      }

      str = explicitKey ? "? ".concat(str, "\n").concat(indent, ":") : "".concat(str, ":");

      if (this.comment) {
        // expected (but not strictly required) to be a single-line comment
        str = (0, _addComment.default)(str, ctx.indent, this.comment);
        if (onComment) onComment();
      }

      var vcb = '';
      var valueComment = null;

      if (value instanceof _Node2.default) {
        if (value.spaceBefore) vcb = '\n';

        if (value.commentBefore) {
          var cs = value.commentBefore.replace(/^/gm, "".concat(ctx.indent, "#"));
          vcb += "\n".concat(cs);
        }

        valueComment = value.comment;
      } else if (value && (0, _typeof2.default)(value) === 'object') {
        value = doc.schema.createNode(value, true);
      }

      ctx.implicitKey = false;
      chompKeep = false;
      var valueStr = doc.schema.stringify(value, ctx, function () {
        return valueComment = null;
      }, function () {
        return chompKeep = true;
      });
      var ws = ' ';

      if (vcb || this.comment) {
        ws = "".concat(vcb, "\n").concat(ctx.indent);
      } else if (!explicitKey && value instanceof _Collection.default) {
        var flow = valueStr[0] === '[' || valueStr[0] === '{';
        if (!flow || valueStr.includes('\n')) ws = "\n".concat(ctx.indent);
      }

      if (chompKeep && !valueComment && onChompKeep) onChompKeep();
      return (0, _addComment.default)(str + ws + valueStr, ctx.indent, valueComment);
    }
  }, {
    key: "commentBefore",
    get: function get() {
      return this.key && this.key.commentBefore;
    },
    set: function set(cb) {
      if (this.key == null) this.key = new _Scalar.default(null);
      this.key.commentBefore = cb;
    }
  }]);
  return Pair;
}(_Node2.default);

exports.default = Pair;
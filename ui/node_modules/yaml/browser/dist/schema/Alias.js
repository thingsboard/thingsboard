"use strict";

var _interopRequireDefault = require("@babel/runtime/helpers/interopRequireDefault");

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.default = void 0;

var _classCallCheck2 = _interopRequireDefault(require("@babel/runtime/helpers/classCallCheck"));

var _possibleConstructorReturn2 = _interopRequireDefault(require("@babel/runtime/helpers/possibleConstructorReturn"));

var _getPrototypeOf2 = _interopRequireDefault(require("@babel/runtime/helpers/getPrototypeOf"));

var _createClass2 = _interopRequireDefault(require("@babel/runtime/helpers/createClass"));

var _inherits2 = _interopRequireDefault(require("@babel/runtime/helpers/inherits"));

var _defineProperty2 = _interopRequireDefault(require("@babel/runtime/helpers/defineProperty"));

var _constants = require("../constants");

var _errors = require("../errors");

var _toJSON2 = _interopRequireDefault(require("../toJSON"));

var _Collection = _interopRequireDefault(require("./Collection"));

var _Node2 = _interopRequireDefault(require("./Node"));

var _Pair = _interopRequireDefault(require("./Pair"));

var getAliasCount = function getAliasCount(node, anchors) {
  if (node instanceof Alias) {
    var anchor = anchors.find(function (a) {
      return a.node === node.source;
    });
    return anchor.count * anchor.aliasCount;
  } else if (node instanceof _Collection.default) {
    var count = 0;
    var _iteratorNormalCompletion = true;
    var _didIteratorError = false;
    var _iteratorError = undefined;

    try {
      for (var _iterator = node.items[Symbol.iterator](), _step; !(_iteratorNormalCompletion = (_step = _iterator.next()).done); _iteratorNormalCompletion = true) {
        var item = _step.value;
        var c = getAliasCount(item, anchors);
        if (c > count) count = c;
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

    return count;
  } else if (node instanceof _Pair.default) {
    var kc = getAliasCount(node.key, anchors);
    var vc = getAliasCount(node.value, anchors);
    return Math.max(kc, vc);
  }

  return 1;
};

var Alias =
/*#__PURE__*/
function (_Node) {
  (0, _inherits2.default)(Alias, _Node);
  (0, _createClass2.default)(Alias, null, [{
    key: "stringify",
    value: function stringify(_ref, _ref2) {
      var range = _ref.range,
          source = _ref.source;
      var anchors = _ref2.anchors,
          doc = _ref2.doc,
          implicitKey = _ref2.implicitKey,
          inStringifyKey = _ref2.inStringifyKey;
      var anchor = Object.keys(anchors).find(function (a) {
        return anchors[a] === source;
      });
      if (!anchor && inStringifyKey) anchor = doc.anchors.getName(source) || doc.anchors.newName();
      if (anchor) return "*".concat(anchor).concat(implicitKey ? ' ' : '');
      var msg = doc.anchors.getName(source) ? 'Alias node must be after source node' : 'Source node not found for alias node';
      throw new Error("".concat(msg, " [").concat(range, "]"));
    }
  }]);

  function Alias(source) {
    var _this;

    (0, _classCallCheck2.default)(this, Alias);
    _this = (0, _possibleConstructorReturn2.default)(this, (0, _getPrototypeOf2.default)(Alias).call(this));
    _this.source = source;
    _this.type = _constants.Type.ALIAS;
    return _this;
  }

  (0, _createClass2.default)(Alias, [{
    key: "toJSON",
    value: function toJSON(arg, ctx) {
      var _this2 = this;

      if (!ctx) return (0, _toJSON2.default)(this.source, arg, ctx);
      var anchors = ctx.anchors,
          maxAliasCount = ctx.maxAliasCount;
      var anchor = anchors.find(function (a) {
        return a.node === _this2.source;
      });

      if (!anchor || anchor.res === undefined) {
        var msg = 'This should not happen: Alias anchor was not resolved?';
        if (this.cstNode) throw new _errors.YAMLReferenceError(this.cstNode, msg);else throw new ReferenceError(msg);
      }

      if (maxAliasCount >= 0) {
        anchor.count += 1;
        if (anchor.aliasCount === 0) anchor.aliasCount = getAliasCount(this.source, anchors);

        if (anchor.count * anchor.aliasCount > maxAliasCount) {
          var _msg = 'Excessive alias count indicates a resource exhaustion attack';
          if (this.cstNode) throw new _errors.YAMLReferenceError(this.cstNode, _msg);else throw new ReferenceError(_msg);
        }
      }

      return anchor.res;
    } // Only called when stringifying an alias mapping key while constructing
    // Object output.

  }, {
    key: "toString",
    value: function toString(ctx) {
      return Alias.stringify(this, ctx);
    }
  }, {
    key: "tag",
    set: function set(t) {
      throw new Error('Alias nodes cannot have tags');
    }
  }]);
  return Alias;
}(_Node2.default);

exports.default = Alias;
(0, _defineProperty2.default)(Alias, "default", true);
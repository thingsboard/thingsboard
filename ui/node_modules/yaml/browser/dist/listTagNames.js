"use strict";

var _interopRequireDefault = require("@babel/runtime/helpers/interopRequireDefault");

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.default = void 0;

var _typeof2 = _interopRequireDefault(require("@babel/runtime/helpers/typeof"));

var _Collection = _interopRequireDefault(require("./schema/Collection"));

var _Pair = _interopRequireDefault(require("./schema/Pair"));

var _Scalar = _interopRequireDefault(require("./schema/Scalar"));

var visit = function visit(node, tags) {
  if (node && (0, _typeof2.default)(node) === 'object') {
    var tag = node.tag;

    if (node instanceof _Collection.default) {
      if (tag) tags[tag] = true;
      node.items.forEach(function (n) {
        return visit(n, tags);
      });
    } else if (node instanceof _Pair.default) {
      visit(node.key, tags);
      visit(node.value, tags);
    } else if (node instanceof _Scalar.default) {
      if (tag) tags[tag] = true;
    }
  }

  return tags;
};

var _default = function _default(node) {
  return Object.keys(visit(node, {}));
};

exports.default = _default;
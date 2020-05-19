"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.default = mapSelf;

var _react = _interopRequireDefault(require("react"));

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

function mirror(o) {
  return o;
}

function mapSelf(children) {
  // return ReactFragment
  return _react.default.Children.map(children, mirror);
}
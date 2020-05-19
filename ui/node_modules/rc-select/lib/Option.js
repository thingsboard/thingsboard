'use strict';

Object.defineProperty(exports, "__esModule", {
  value: true
});

var _classCallCheck2 = require('babel-runtime/helpers/classCallCheck');

var _classCallCheck3 = _interopRequireDefault(_classCallCheck2);

var _possibleConstructorReturn2 = require('babel-runtime/helpers/possibleConstructorReturn');

var _possibleConstructorReturn3 = _interopRequireDefault(_possibleConstructorReturn2);

var _inherits2 = require('babel-runtime/helpers/inherits');

var _inherits3 = _interopRequireDefault(_inherits2);

var _react = require('react');

var _react2 = _interopRequireDefault(_react);

var _propTypes = require('prop-types');

var _propTypes2 = _interopRequireDefault(_propTypes);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { 'default': obj }; }

var Option = function (_React$Component) {
  (0, _inherits3['default'])(Option, _React$Component);

  function Option() {
    (0, _classCallCheck3['default'])(this, Option);
    return (0, _possibleConstructorReturn3['default'])(this, (Option.__proto__ || Object.getPrototypeOf(Option)).apply(this, arguments));
  }

  return Option;
}(_react2['default'].Component);

Option.propTypes = {
  value: _propTypes2['default'].string
};
Option.isSelectOption = true;
exports['default'] = Option;
module.exports = exports['default'];
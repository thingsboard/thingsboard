'use strict';

Object.defineProperty(exports, "__esModule", {
  value: true
});

var _extends2 = require('babel-runtime/helpers/extends');

var _extends3 = _interopRequireDefault(_extends2);

var _objectWithoutProperties2 = require('babel-runtime/helpers/objectWithoutProperties');

var _objectWithoutProperties3 = _interopRequireDefault(_objectWithoutProperties2);

var _getPrototypeOf = require('babel-runtime/core-js/object/get-prototype-of');

var _getPrototypeOf2 = _interopRequireDefault(_getPrototypeOf);

var _classCallCheck2 = require('babel-runtime/helpers/classCallCheck');

var _classCallCheck3 = _interopRequireDefault(_classCallCheck2);

var _createClass2 = require('babel-runtime/helpers/createClass');

var _createClass3 = _interopRequireDefault(_createClass2);

var _possibleConstructorReturn2 = require('babel-runtime/helpers/possibleConstructorReturn');

var _possibleConstructorReturn3 = _interopRequireDefault(_possibleConstructorReturn2);

var _inherits2 = require('babel-runtime/helpers/inherits');

var _inherits3 = _interopRequireDefault(_inherits2);

var _simpleAssign = require('simple-assign');

var _simpleAssign2 = _interopRequireDefault(_simpleAssign);

var _react = require('react');

var _react2 = _interopRequireDefault(_react);

var _reactAddonsTransitionGroup = require('react-addons-transition-group');

var _reactAddonsTransitionGroup2 = _interopRequireDefault(_reactAddonsTransitionGroup);

var _ExpandTransitionChild = require('./ExpandTransitionChild');

var _ExpandTransitionChild2 = _interopRequireDefault(_ExpandTransitionChild);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

var ExpandTransition = function (_Component) {
  (0, _inherits3.default)(ExpandTransition, _Component);

  function ExpandTransition() {
    (0, _classCallCheck3.default)(this, ExpandTransition);
    return (0, _possibleConstructorReturn3.default)(this, (ExpandTransition.__proto__ || (0, _getPrototypeOf2.default)(ExpandTransition)).apply(this, arguments));
  }

  (0, _createClass3.default)(ExpandTransition, [{
    key: 'renderChildren',
    value: function renderChildren(children) {
      var _props = this.props,
          enterDelay = _props.enterDelay,
          transitionDelay = _props.transitionDelay,
          transitionDuration = _props.transitionDuration;

      return _react2.default.Children.map(children, function (child) {
        return _react2.default.createElement(
          _ExpandTransitionChild2.default,
          {
            enterDelay: enterDelay,
            transitionDelay: transitionDelay,
            transitionDuration: transitionDuration,
            key: child.key
          },
          child
        );
      }, this);
    }
  }, {
    key: 'render',
    value: function render() {
      var _props2 = this.props,
          children = _props2.children,
          enterDelay = _props2.enterDelay,
          loading = _props2.loading,
          open = _props2.open,
          style = _props2.style,
          transitionDelay = _props2.transitionDelay,
          transitionDuration = _props2.transitionDuration,
          other = (0, _objectWithoutProperties3.default)(_props2, ['children', 'enterDelay', 'loading', 'open', 'style', 'transitionDelay', 'transitionDuration']);
      var prepareStyles = this.context.muiTheme.prepareStyles;


      var mergedRootStyles = (0, _simpleAssign2.default)({}, {
        position: 'relative',
        overflow: 'hidden',
        height: 'auto'
      }, style);

      var newChildren = loading ? [] : this.renderChildren(children);

      return _react2.default.createElement(
        _reactAddonsTransitionGroup2.default,
        (0, _extends3.default)({
          style: prepareStyles(mergedRootStyles),
          component: 'div'
        }, other),
        open && newChildren
      );
    }
  }]);
  return ExpandTransition;
}(_react.Component);

ExpandTransition.defaultProps = {
  enterDelay: 0,
  transitionDelay: 0,
  transitionDuration: 450,
  loading: false,
  open: false
};
ExpandTransition.contextTypes = {
  muiTheme: _react.PropTypes.object.isRequired
};
process.env.NODE_ENV !== "production" ? ExpandTransition.propTypes = {
  children: _react.PropTypes.node,
  enterDelay: _react.PropTypes.number,
  loading: _react.PropTypes.bool,
  open: _react.PropTypes.bool,
  style: _react.PropTypes.object,
  transitionDelay: _react.PropTypes.number,
  transitionDuration: _react.PropTypes.number
} : void 0;
exports.default = ExpandTransition;
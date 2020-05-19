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

var _reactDom = require('react-dom');

var _reactDom2 = _interopRequireDefault(_reactDom);

var _transitions = require('../styles/transitions');

var _transitions2 = _interopRequireDefault(_transitions);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

var reflow = function reflow(elem) {
  return elem.offsetHeight;
};

var ExpandTransitionChild = function (_Component) {
  (0, _inherits3.default)(ExpandTransitionChild, _Component);

  function ExpandTransitionChild() {
    (0, _classCallCheck3.default)(this, ExpandTransitionChild);
    return (0, _possibleConstructorReturn3.default)(this, (ExpandTransitionChild.__proto__ || (0, _getPrototypeOf2.default)(ExpandTransitionChild)).apply(this, arguments));
  }

  (0, _createClass3.default)(ExpandTransitionChild, [{
    key: 'componentWillUnmount',
    value: function componentWillUnmount() {
      clearTimeout(this.enterTimer);
      clearTimeout(this.enteredTimer);
      clearTimeout(this.leaveTimer);
    }
  }, {
    key: 'componentWillAppear',
    value: function componentWillAppear(callback) {
      this.open();
      callback();
    }
  }, {
    key: 'componentDidAppear',
    value: function componentDidAppear() {
      this.setAutoHeight();
    }
  }, {
    key: 'componentWillEnter',
    value: function componentWillEnter(callback) {
      var _this2 = this;

      var _props = this.props,
          enterDelay = _props.enterDelay,
          transitionDelay = _props.transitionDelay,
          transitionDuration = _props.transitionDuration;

      var element = _reactDom2.default.findDOMNode(this);
      element.style.height = 0;
      this.enterTimer = setTimeout(function () {
        return _this2.open();
      }, enterDelay);
      this.enteredTimer = setTimeout(function () {
        return callback();
      }, enterDelay + transitionDelay + transitionDuration);
    }
  }, {
    key: 'componentDidEnter',
    value: function componentDidEnter() {
      this.setAutoHeight();
    }
  }, {
    key: 'componentWillLeave',
    value: function componentWillLeave(callback) {
      var _props2 = this.props,
          transitionDelay = _props2.transitionDelay,
          transitionDuration = _props2.transitionDuration;

      var element = _reactDom2.default.findDOMNode(this);
      // Set fixed height first for animated property value
      element.style.height = this.refs.wrapper.clientHeight + 'px';
      reflow(element);
      element.style.transitionDuration = transitionDuration + 'ms';
      element.style.height = 0;
      this.leaveTimer = setTimeout(function () {
        return callback();
      }, transitionDelay + transitionDuration);
    }
  }, {
    key: 'setAutoHeight',
    value: function setAutoHeight() {
      var _ReactDOM$findDOMNode = _reactDom2.default.findDOMNode(this),
          style = _ReactDOM$findDOMNode.style;

      style.transitionDuration = 0;
      style.height = 'auto';
    }
  }, {
    key: 'open',
    value: function open() {
      var element = _reactDom2.default.findDOMNode(this);
      element.style.height = this.refs.wrapper.clientHeight + 'px';
    }
  }, {
    key: 'render',
    value: function render() {
      var _props3 = this.props,
          children = _props3.children,
          enterDelay = _props3.enterDelay,
          style = _props3.style,
          transitionDelay = _props3.transitionDelay,
          transitionDuration = _props3.transitionDuration,
          other = (0, _objectWithoutProperties3.default)(_props3, ['children', 'enterDelay', 'style', 'transitionDelay', 'transitionDuration']);
      var prepareStyles = this.context.muiTheme.prepareStyles;


      var mergedRootStyles = (0, _simpleAssign2.default)({
        position: 'relative',
        height: 0,
        width: '100%',
        top: 0,
        left: 0,
        overflow: 'hidden',
        transition: _transitions2.default.easeOut(transitionDuration + 'ms', ['height'], transitionDelay + 'ms')
      }, style);

      return _react2.default.createElement(
        'div',
        (0, _extends3.default)({}, other, { style: prepareStyles(mergedRootStyles) }),
        _react2.default.createElement(
          'div',
          { ref: 'wrapper' },
          children
        )
      );
    }
  }]);
  return ExpandTransitionChild;
}(_react.Component);

ExpandTransitionChild.defaultProps = {
  enterDelay: 0,
  transitionDelay: 0,
  transitionDuration: 450
};
ExpandTransitionChild.contextTypes = {
  muiTheme: _react.PropTypes.object.isRequired
};
process.env.NODE_ENV !== "production" ? ExpandTransitionChild.propTypes = {
  children: _react.PropTypes.node,
  enterDelay: _react.PropTypes.number,
  style: _react.PropTypes.object,
  transitionDelay: _react.PropTypes.number,
  transitionDuration: _react.PropTypes.number
} : void 0;
exports.default = ExpandTransitionChild;
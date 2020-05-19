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

var _transitions = require('../styles/transitions');

var _transitions2 = _interopRequireDefault(_transitions);

var _EnhancedButton = require('../internal/EnhancedButton');

var _EnhancedButton2 = _interopRequireDefault(_EnhancedButton);

var _StepLabel = require('./StepLabel');

var _StepLabel2 = _interopRequireDefault(_StepLabel);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

var isLabel = function isLabel(child) {
  return child && child.type && child.type.muiName === 'StepLabel';
};

var getStyles = function getStyles(props, context, state) {
  var hovered = state.hovered;
  var _context$muiTheme$ste = context.muiTheme.stepper,
      backgroundColor = _context$muiTheme$ste.backgroundColor,
      hoverBackgroundColor = _context$muiTheme$ste.hoverBackgroundColor;


  var styles = {
    root: {
      padding: 0,
      backgroundColor: hovered ? hoverBackgroundColor : backgroundColor,
      transition: _transitions2.default.easeOut()
    }
  };

  if (context.stepper.orientation === 'vertical') {
    styles.root.width = '100%';
  }

  return styles;
};

var StepButton = function (_Component) {
  (0, _inherits3.default)(StepButton, _Component);

  function StepButton() {
    var _ref;

    var _temp, _this, _ret;

    (0, _classCallCheck3.default)(this, StepButton);

    for (var _len = arguments.length, args = Array(_len), _key = 0; _key < _len; _key++) {
      args[_key] = arguments[_key];
    }

    return _ret = (_temp = (_this = (0, _possibleConstructorReturn3.default)(this, (_ref = StepButton.__proto__ || (0, _getPrototypeOf2.default)(StepButton)).call.apply(_ref, [this].concat(args))), _this), _this.state = {
      hovered: false,
      touched: false
    }, _this.handleMouseEnter = function (event) {
      var onMouseEnter = _this.props.onMouseEnter;
      // Cancel hover styles for touch devices

      if (!_this.state.touched) {
        _this.setState({ hovered: true });
      }
      if (typeof onMouseEnter === 'function') {
        onMouseEnter(event);
      }
    }, _this.handleMouseLeave = function (event) {
      var onMouseLeave = _this.props.onMouseLeave;

      _this.setState({ hovered: false });
      if (typeof onMouseLeave === 'function') {
        onMouseLeave(event);
      }
    }, _this.handleTouchStart = function (event) {
      var onTouchStart = _this.props.onTouchStart;

      if (!_this.state.touched) {
        _this.setState({ touched: true });
      }
      if (typeof onTouchStart === 'function') {
        onTouchStart(event);
      }
    }, _temp), (0, _possibleConstructorReturn3.default)(_this, _ret);
  }

  (0, _createClass3.default)(StepButton, [{
    key: 'render',
    value: function render() {
      var _props = this.props,
          active = _props.active,
          children = _props.children,
          completed = _props.completed,
          disabled = _props.disabled,
          icon = _props.icon,
          iconContainerStyle = _props.iconContainerStyle,
          last = _props.last,
          onMouseEnter = _props.onMouseEnter,
          onMouseLeave = _props.onMouseLeave,
          onTouchStart = _props.onTouchStart,
          style = _props.style,
          other = (0, _objectWithoutProperties3.default)(_props, ['active', 'children', 'completed', 'disabled', 'icon', 'iconContainerStyle', 'last', 'onMouseEnter', 'onMouseLeave', 'onTouchStart', 'style']);


      var styles = getStyles(this.props, this.context, this.state);

      var child = isLabel(children) ? children : _react2.default.createElement(
        _StepLabel2.default,
        null,
        children
      );

      return _react2.default.createElement(
        _EnhancedButton2.default,
        (0, _extends3.default)({
          disabled: disabled,
          style: (0, _simpleAssign2.default)(styles.root, style),
          onMouseEnter: this.handleMouseEnter,
          onMouseLeave: this.handleMouseLeave,
          onTouchStart: this.handleTouchStart
        }, other),
        _react2.default.cloneElement(child, { active: active, completed: completed, disabled: disabled, icon: icon, iconContainerStyle: iconContainerStyle })
      );
    }
  }]);
  return StepButton;
}(_react.Component);

StepButton.contextTypes = {
  muiTheme: _react.PropTypes.object.isRequired,
  stepper: _react.PropTypes.object
};
process.env.NODE_ENV !== "production" ? StepButton.propTypes = {
  /**
   * Passed from `Step` Is passed to StepLabel.
   */
  active: _react.PropTypes.bool,
  /**
   * Can be a `StepLabel` or a node to place inside `StepLabel` as children.
   */
  children: _react.PropTypes.node,
  /**
   * Sets completed styling. Is passed to StepLabel.
   */
  completed: _react.PropTypes.bool,
  /**
   * Disables the button and sets disabled styling. Is passed to StepLabel.
   */
  disabled: _react.PropTypes.bool,
  /**
   * The icon displayed by the step label.
   */
  icon: _react.PropTypes.oneOfType([_react.PropTypes.element, _react.PropTypes.string, _react.PropTypes.number]),
  /**
   * Override the inline-styles of the icon container element.
   */
  iconContainerStyle: _react.PropTypes.object,
  /** @ignore */
  last: _react.PropTypes.bool,
  /** @ignore */
  onMouseEnter: _react.PropTypes.func,
  /** @ignore */
  onMouseLeave: _react.PropTypes.func,
  /** @ignore */
  onTouchStart: _react.PropTypes.func,
  /**
   * Override the inline-style of the root element.
   */
  style: _react.PropTypes.object
} : void 0;
exports.default = StepButton;
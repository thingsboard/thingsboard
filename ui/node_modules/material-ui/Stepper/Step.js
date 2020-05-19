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

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

var getStyles = function getStyles(_ref, _ref2) {
  var index = _ref.index;
  var stepper = _ref2.stepper;
  var orientation = stepper.orientation;

  var styles = {
    root: {
      flex: '0 0 auto'
    }
  };

  if (index > 0) {
    if (orientation === 'horizontal') {
      styles.root.marginLeft = -6;
    } else if (orientation === 'vertical') {
      styles.root.marginTop = -14;
    }
  }

  return styles;
};

var Step = function (_Component) {
  (0, _inherits3.default)(Step, _Component);

  function Step() {
    var _ref3;

    var _temp, _this, _ret;

    (0, _classCallCheck3.default)(this, Step);

    for (var _len = arguments.length, args = Array(_len), _key = 0; _key < _len; _key++) {
      args[_key] = arguments[_key];
    }

    return _ret = (_temp = (_this = (0, _possibleConstructorReturn3.default)(this, (_ref3 = Step.__proto__ || (0, _getPrototypeOf2.default)(Step)).call.apply(_ref3, [this].concat(args))), _this), _this.renderChild = function (child) {
      var _this$props = _this.props,
          active = _this$props.active,
          completed = _this$props.completed,
          disabled = _this$props.disabled,
          index = _this$props.index,
          last = _this$props.last;


      var icon = index + 1;

      return _react2.default.cloneElement(child, (0, _simpleAssign2.default)({ active: active, completed: completed, disabled: disabled, icon: icon, last: last }, child.props));
    }, _temp), (0, _possibleConstructorReturn3.default)(_this, _ret);
  }

  (0, _createClass3.default)(Step, [{
    key: 'render',
    value: function render() {
      var _props = this.props,
          active = _props.active,
          completed = _props.completed,
          disabled = _props.disabled,
          index = _props.index,
          last = _props.last,
          children = _props.children,
          style = _props.style,
          other = (0, _objectWithoutProperties3.default)(_props, ['active', 'completed', 'disabled', 'index', 'last', 'children', 'style']);
      var prepareStyles = this.context.muiTheme.prepareStyles;

      var styles = getStyles(this.props, this.context);

      return _react2.default.createElement(
        'div',
        (0, _extends3.default)({ style: prepareStyles((0, _simpleAssign2.default)(styles.root, style)) }, other),
        _react2.default.Children.map(children, this.renderChild)
      );
    }
  }]);
  return Step;
}(_react.Component);

Step.contextTypes = {
  muiTheme: _react.PropTypes.object.isRequired,
  stepper: _react.PropTypes.object
};
process.env.NODE_ENV !== "production" ? Step.propTypes = {
  /**
   * Sets the step as active. Is passed to child components.
   */
  active: _react.PropTypes.bool,
  /**
   * Should be `Step` sub-components such as `StepLabel`.
   */
  children: _react.PropTypes.node,
  /**
   * Mark the step as completed. Is passed to child components.
   */
  completed: _react.PropTypes.bool,
  /**
   * Mark the step as disabled, will also disable the button if
   * `StepButton` is a child of `Step`. Is passed to child components.
   */
  disabled: _react.PropTypes.bool,
  /**
   * @ignore
   * Used internally for numbering.
   */
  index: _react.PropTypes.number,
  /**
   * @ignore
   */
  last: _react.PropTypes.bool,
  /**
   * Override the inline-style of the root element.
   */
  style: _react.PropTypes.object
} : void 0;
exports.default = Step;
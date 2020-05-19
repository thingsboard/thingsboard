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

var _react = require('react');

var _react2 = _interopRequireDefault(_react);

var _transitions = require('../styles/transitions');

var _transitions2 = _interopRequireDefault(_transitions);

var _dateUtils = require('./dateUtils');

var _EnhancedButton = require('../internal/EnhancedButton');

var _EnhancedButton2 = _interopRequireDefault(_EnhancedButton);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

function getStyles(props, context, state) {
  var date = props.date,
      disabled = props.disabled,
      selected = props.selected;
  var hover = state.hover;
  var _context$muiTheme = context.muiTheme,
      baseTheme = _context$muiTheme.baseTheme,
      datePicker = _context$muiTheme.datePicker;


  var labelColor = baseTheme.palette.textColor;
  var buttonStateOpacity = 0;
  var buttonStateTransform = 'scale(0)';

  if (hover || selected) {
    labelColor = datePicker.selectTextColor;
    buttonStateOpacity = selected ? 1 : 0.6;
    buttonStateTransform = 'scale(1)';
  } else if ((0, _dateUtils.isEqualDate)(date, new Date())) {
    labelColor = datePicker.color;
  }

  return {
    root: {
      boxSizing: 'border-box',
      fontWeight: '400',
      opacity: disabled && '0.4',
      padding: '4px 0px',
      position: 'relative',
      WebkitTapHighlightColor: 'rgba(0,0,0,0)', // Remove mobile color flashing (deprecated)
      width: 42
    },
    label: {
      color: labelColor,
      fontWeight: '400',
      position: 'relative'
    },
    buttonState: {
      backgroundColor: datePicker.selectColor,
      borderRadius: '50%',
      height: 34,
      left: 4,
      opacity: buttonStateOpacity,
      position: 'absolute',
      top: 0,
      transform: buttonStateTransform,
      transition: _transitions2.default.easeOut(),
      width: 34
    }
  };
}

var DayButton = function (_Component) {
  (0, _inherits3.default)(DayButton, _Component);

  function DayButton() {
    var _ref;

    var _temp, _this, _ret;

    (0, _classCallCheck3.default)(this, DayButton);

    for (var _len = arguments.length, args = Array(_len), _key = 0; _key < _len; _key++) {
      args[_key] = arguments[_key];
    }

    return _ret = (_temp = (_this = (0, _possibleConstructorReturn3.default)(this, (_ref = DayButton.__proto__ || (0, _getPrototypeOf2.default)(DayButton)).call.apply(_ref, [this].concat(args))), _this), _this.state = {
      hover: false
    }, _this.handleMouseEnter = function () {
      if (!_this.props.disabled) {
        _this.setState({ hover: true });
      }
    }, _this.handleMouseLeave = function () {
      if (!_this.props.disabled) {
        _this.setState({ hover: false });
      }
    }, _this.handleTouchTap = function (event) {
      if (!_this.props.disabled && _this.props.onTouchTap) {
        _this.props.onTouchTap(event, _this.props.date);
      }
    }, _this.handleKeyboardFocus = function (event, keyboardFocused) {
      if (!_this.props.disabled && _this.props.onKeyboardFocus) {
        _this.props.onKeyboardFocus(event, keyboardFocused, _this.props.date);
      }
    }, _temp), (0, _possibleConstructorReturn3.default)(_this, _ret);
  }

  (0, _createClass3.default)(DayButton, [{
    key: 'render',
    value: function render() {
      var _props = this.props,
          DateTimeFormat = _props.DateTimeFormat,
          date = _props.date,
          disabled = _props.disabled,
          locale = _props.locale,
          onTouchTap = _props.onTouchTap,
          selected = _props.selected,
          other = (0, _objectWithoutProperties3.default)(_props, ['DateTimeFormat', 'date', 'disabled', 'locale', 'onTouchTap', 'selected']);
      var prepareStyles = this.context.muiTheme.prepareStyles;

      var styles = getStyles(this.props, this.context, this.state);

      return date ? _react2.default.createElement(
        _EnhancedButton2.default,
        (0, _extends3.default)({}, other, {
          disabled: disabled,
          disableFocusRipple: true,
          disableTouchRipple: true,
          onKeyboardFocus: this.handleKeyboardFocus,
          onMouseEnter: this.handleMouseEnter,
          onMouseLeave: this.handleMouseLeave,
          onTouchTap: this.handleTouchTap,
          style: styles.root
        }),
        _react2.default.createElement('div', { style: prepareStyles(styles.buttonState) }),
        _react2.default.createElement(
          'span',
          { style: prepareStyles(styles.label) },
          new DateTimeFormat(locale, {
            day: 'numeric'
          }).format(date)
        )
      ) : _react2.default.createElement('span', { style: prepareStyles(styles.root) });
    }
  }]);
  return DayButton;
}(_react.Component);

DayButton.defaultProps = {
  selected: false,
  disabled: false
};
DayButton.contextTypes = {
  muiTheme: _react.PropTypes.object.isRequired
};
process.env.NODE_ENV !== "production" ? DayButton.propTypes = {
  DateTimeFormat: _react.PropTypes.func.isRequired,
  date: _react.PropTypes.object,
  disabled: _react.PropTypes.bool,
  locale: _react.PropTypes.string.isRequired,
  onKeyboardFocus: _react.PropTypes.func,
  onTouchTap: _react.PropTypes.func,
  selected: _react.PropTypes.bool
} : void 0;
exports.default = DayButton;
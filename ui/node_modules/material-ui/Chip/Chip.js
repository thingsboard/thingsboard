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

var _keycode = require('keycode');

var _keycode2 = _interopRequireDefault(_keycode);

var _colorManipulator = require('../utils/colorManipulator');

var _EnhancedButton = require('../internal/EnhancedButton');

var _EnhancedButton2 = _interopRequireDefault(_EnhancedButton);

var _cancel = require('../svg-icons/navigation/cancel');

var _cancel2 = _interopRequireDefault(_cancel);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

function getStyles(props, context, state) {
  var chip = context.muiTheme.chip;


  var backgroundColor = props.backgroundColor || chip.backgroundColor;
  var focusColor = (0, _colorManipulator.emphasize)(backgroundColor, 0.08);
  var pressedColor = (0, _colorManipulator.emphasize)(backgroundColor, 0.12);

  return {
    avatar: {
      marginRight: -4
    },
    deleteIcon: {
      color: state.deleteHovered ? (0, _colorManipulator.fade)(chip.deleteIconColor, 0.4) : chip.deleteIconColor,
      cursor: 'pointer',
      margin: '4px 4px 0px -8px'
    },
    label: {
      color: props.labelColor || chip.textColor,
      fontSize: chip.fontSize,
      fontWeight: chip.fontWeight,
      lineHeight: '32px',
      paddingLeft: 12,
      paddingRight: 12,
      userSelect: 'none',
      whiteSpace: 'nowrap'
    },
    root: {
      backgroundColor: state.clicked ? pressedColor : state.focused || state.hovered ? focusColor : backgroundColor,
      borderRadius: 16,
      boxShadow: state.clicked ? chip.shadow : null,
      cursor: props.onTouchTap ? 'pointer' : 'default',
      display: 'flex',
      whiteSpace: 'nowrap',
      width: 'fit-content'
    }
  };
}

var Chip = function (_Component) {
  (0, _inherits3.default)(Chip, _Component);

  function Chip() {
    var _ref;

    var _temp, _this, _ret;

    (0, _classCallCheck3.default)(this, Chip);

    for (var _len = arguments.length, args = Array(_len), _key = 0; _key < _len; _key++) {
      args[_key] = arguments[_key];
    }

    return _ret = (_temp = (_this = (0, _possibleConstructorReturn3.default)(this, (_ref = Chip.__proto__ || (0, _getPrototypeOf2.default)(Chip)).call.apply(_ref, [this].concat(args))), _this), _this.state = {
      clicked: false,
      deleteHovered: false,
      focused: false,
      hovered: false
    }, _this.handleBlur = function (event) {
      _this.setState({ clicked: false, focused: false });
      _this.props.onBlur(event);
    }, _this.handleFocus = function (event) {
      if (_this.props.onTouchTap || _this.props.onRequestDelete) {
        _this.setState({ focused: true });
      }
      _this.props.onFocus(event);
    }, _this.handleKeyboardFocus = function (event, keyboardFocused) {
      if (keyboardFocused) {
        _this.handleFocus();
        _this.props.onFocus(event);
      } else {
        _this.handleBlur();
      }

      _this.props.onKeyboardFocus(event, keyboardFocused);
    }, _this.handleKeyDown = function (event) {
      if ((0, _keycode2.default)(event) === 'backspace') {
        event.preventDefault();
        if (_this.props.onRequestDelete) {
          _this.props.onRequestDelete(event);
        }
      }
      _this.props.onKeyDown(event);
    }, _this.handleMouseDown = function (event) {
      // Only listen to left clicks
      if (event.button === 0) {
        event.stopPropagation();
        if (_this.props.onTouchTap) {
          _this.setState({ clicked: true });
        }
      }
      _this.props.onMouseDown(event);
    }, _this.handleMouseEnter = function (event) {
      if (_this.props.onTouchTap) {
        _this.setState({ hovered: true });
      }
      _this.props.onMouseEnter(event);
    }, _this.handleMouseEnterDeleteIcon = function () {
      _this.setState({ deleteHovered: true });
    }, _this.handleMouseLeave = function (event) {
      _this.setState({
        clicked: false,
        hovered: false
      });
      _this.props.onMouseLeave(event);
    }, _this.handleMouseLeaveDeleteIcon = function () {
      _this.setState({ deleteHovered: false });
    }, _this.handleMouseUp = function (event) {
      _this.setState({ clicked: false });
      _this.props.onMouseUp(event);
    }, _this.handleTouchTapDeleteIcon = function (event) {
      // Stop the event from bubbling up to the `Chip`
      event.stopPropagation();
      _this.props.onRequestDelete(event);
    }, _this.handleTouchEnd = function (event) {
      _this.setState({ clicked: false });
      _this.props.onTouchEnd(event);
    }, _this.handleTouchStart = function (event) {
      event.stopPropagation();
      if (_this.props.onTouchTap) {
        _this.setState({ clicked: true });
      }
      _this.props.onTouchStart(event);
    }, _temp), (0, _possibleConstructorReturn3.default)(_this, _ret);
  }

  (0, _createClass3.default)(Chip, [{
    key: 'render',
    value: function render() {
      var buttonEventHandlers = {
        onBlur: this.handleBlur,
        onFocus: this.handleFocus,
        onKeyDown: this.handleKeyDown,
        onMouseDown: this.handleMouseDown,
        onMouseEnter: this.handleMouseEnter,
        onMouseLeave: this.handleMouseLeave,
        onMouseUp: this.handleMouseUp,
        onTouchEnd: this.handleTouchEnd,
        onTouchStart: this.handleTouchStart,
        onKeyboardFocus: this.handleKeyboardFocus
      };

      var prepareStyles = this.context.muiTheme.prepareStyles;

      var styles = getStyles(this.props, this.context, this.state);

      var _props = this.props,
          childrenProp = _props.children,
          style = _props.style,
          className = _props.className,
          labelStyle = _props.labelStyle,
          labelColor = _props.labelColor,
          backgroundColor = _props.backgroundColor,
          onRequestDelete = _props.onRequestDelete,
          other = (0, _objectWithoutProperties3.default)(_props, ['children', 'style', 'className', 'labelStyle', 'labelColor', 'backgroundColor', 'onRequestDelete']);


      var deletable = this.props.onRequestDelete;
      var avatar = null;

      var deleteIcon = deletable ? _react2.default.createElement(_cancel2.default, {
        color: styles.deleteIcon.color,
        style: styles.deleteIcon,
        onTouchTap: this.handleTouchTapDeleteIcon,
        onMouseEnter: this.handleMouseEnterDeleteIcon,
        onMouseLeave: this.handleMouseLeaveDeleteIcon
      }) : null;

      var children = childrenProp;
      var childCount = _react2.default.Children.count(children);

      // If the first child is an avatar, extract it and style it
      if (childCount > 1) {
        children = _react2.default.Children.toArray(children);

        if (_react2.default.isValidElement(children[0]) && children[0].type.muiName === 'Avatar') {
          avatar = children.shift();

          avatar = _react2.default.cloneElement(avatar, {
            style: (0, _simpleAssign2.default)(styles.avatar, avatar.props.style),
            size: 32
          });
        }
      }

      return _react2.default.createElement(
        _EnhancedButton2.default,
        (0, _extends3.default)({}, other, buttonEventHandlers, {
          className: className,
          containerElement: 'div' // Firefox doesn't support nested buttons
          , disableTouchRipple: true,
          disableFocusRipple: true,
          style: (0, _simpleAssign2.default)(styles.root, style)
        }),
        avatar,
        _react2.default.createElement(
          'span',
          { style: prepareStyles((0, _simpleAssign2.default)(styles.label, labelStyle)) },
          children
        ),
        deleteIcon
      );
    }
  }]);
  return Chip;
}(_react.Component);

Chip.defaultProps = {
  onBlur: function onBlur() {},
  onFocus: function onFocus() {},
  onKeyDown: function onKeyDown() {},
  onKeyboardFocus: function onKeyboardFocus() {},
  onMouseDown: function onMouseDown() {},
  onMouseEnter: function onMouseEnter() {},
  onMouseLeave: function onMouseLeave() {},
  onMouseUp: function onMouseUp() {},
  onTouchEnd: function onTouchEnd() {},
  onTouchStart: function onTouchStart() {}
};
Chip.contextTypes = { muiTheme: _react.PropTypes.object.isRequired };
process.env.NODE_ENV !== "production" ? Chip.propTypes = {
  /**
   * Override the background color of the chip.
   */
  backgroundColor: _react.PropTypes.string,
  /**
   * Used to render elements inside the Chip.
   */
  children: _react.PropTypes.node,
  /**
   * CSS `className` of the root element.
   */
  className: _react.PropTypes.node,
  /**
   * Override the label color.
   */
  labelColor: _react.PropTypes.string,
  /**
   * Override the inline-styles of the label.
   */
  labelStyle: _react.PropTypes.object,
  /** @ignore */
  onBlur: _react.PropTypes.func,
  /** @ignore */
  onFocus: _react.PropTypes.func,
  /** @ignore */
  onKeyDown: _react.PropTypes.func,
  /** @ignore */
  onKeyboardFocus: _react.PropTypes.func,
  /** @ignore */
  onMouseDown: _react.PropTypes.func,
  /** @ignore */
  onMouseEnter: _react.PropTypes.func,
  /** @ignore */
  onMouseLeave: _react.PropTypes.func,
  /** @ignore */
  onMouseUp: _react.PropTypes.func,
  /**
   * Callback function fired when the delete icon is clicked. If set, the delete icon will be shown.
   * @param {object} event `touchTap` event targeting the element.
   */
  onRequestDelete: _react.PropTypes.func,
  /** @ignore */
  onTouchEnd: _react.PropTypes.func,
  /** @ignore */
  onTouchStart: _react.PropTypes.func,
  /**
   * Callback function fired when the `Chip` element is touch-tapped.
   *
   * @param {object} event TouchTap event targeting the element.
   */
  onTouchTap: _react.PropTypes.func,
  /**
   * Override the inline-styles of the root element.
   */
  style: _react.PropTypes.object
} : void 0;
exports.default = Chip;
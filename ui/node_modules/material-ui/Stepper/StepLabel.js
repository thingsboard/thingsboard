'use strict';

Object.defineProperty(exports, "__esModule", {
  value: true
});

var _extends2 = require('babel-runtime/helpers/extends');

var _extends3 = _interopRequireDefault(_extends2);

var _objectWithoutProperties2 = require('babel-runtime/helpers/objectWithoutProperties');

var _objectWithoutProperties3 = _interopRequireDefault(_objectWithoutProperties2);

var _typeof2 = require('babel-runtime/helpers/typeof');

var _typeof3 = _interopRequireDefault(_typeof2);

var _simpleAssign = require('simple-assign');

var _simpleAssign2 = _interopRequireDefault(_simpleAssign);

var _react = require('react');

var _react2 = _interopRequireDefault(_react);

var _checkCircle = require('../svg-icons/action/check-circle');

var _checkCircle2 = _interopRequireDefault(_checkCircle);

var _SvgIcon = require('../SvgIcon');

var _SvgIcon2 = _interopRequireDefault(_SvgIcon);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

var getStyles = function getStyles(_ref, _ref2) {
  var active = _ref.active,
      completed = _ref.completed,
      disabled = _ref.disabled;
  var muiTheme = _ref2.muiTheme,
      stepper = _ref2.stepper;
  var _muiTheme$stepper = muiTheme.stepper,
      textColor = _muiTheme$stepper.textColor,
      disabledTextColor = _muiTheme$stepper.disabledTextColor,
      iconColor = _muiTheme$stepper.iconColor,
      inactiveIconColor = _muiTheme$stepper.inactiveIconColor;
  var orientation = stepper.orientation;


  var styles = {
    root: {
      height: orientation === 'horizontal' ? 72 : 64,
      color: textColor,
      display: 'flex',
      alignItems: 'center',
      fontSize: 14,
      paddingLeft: 14,
      paddingRight: 14
    },
    icon: {
      color: iconColor,
      display: 'block',
      fontSize: 24,
      width: 24,
      height: 24
    },
    iconContainer: {
      paddingRight: 8
    }
  };

  if (active) {
    styles.root.fontWeight = 500;
  }

  if (!completed && !active) {
    styles.icon.color = inactiveIconColor;
  }

  if (disabled) {
    styles.icon.color = inactiveIconColor;
    styles.root.color = disabledTextColor;
    styles.root.cursor = 'not-allowed';
  }

  return styles;
};

var renderIcon = function renderIcon(completed, icon, styles) {
  var iconType = typeof icon === 'undefined' ? 'undefined' : (0, _typeof3.default)(icon);

  if (iconType === 'number' || iconType === 'string') {
    if (completed) {
      return _react2.default.createElement(_checkCircle2.default, {
        color: styles.icon.color,
        style: styles.icon
      });
    }

    return _react2.default.createElement(
      _SvgIcon2.default,
      { color: styles.icon.color, style: styles.icon },
      _react2.default.createElement('circle', { cx: '12', cy: '12', r: '10' }),
      _react2.default.createElement(
        'text',
        {
          x: '12',
          y: '16',
          textAnchor: 'middle',
          fontSize: '12',
          fill: '#fff'
        },
        icon
      )
    );
  }

  return icon;
};

var StepLabel = function StepLabel(props, context) {
  var active = props.active,
      children = props.children,
      completed = props.completed,
      userIcon = props.icon,
      iconContainerStyle = props.iconContainerStyle,
      last = props.last,
      style = props.style,
      other = (0, _objectWithoutProperties3.default)(props, ['active', 'children', 'completed', 'icon', 'iconContainerStyle', 'last', 'style']);
  var prepareStyles = context.muiTheme.prepareStyles;

  var styles = getStyles(props, context);
  var icon = renderIcon(completed, userIcon, styles);

  return _react2.default.createElement(
    'span',
    (0, _extends3.default)({ style: prepareStyles((0, _simpleAssign2.default)(styles.root, style)) }, other),
    icon && _react2.default.createElement(
      'span',
      { style: prepareStyles((0, _simpleAssign2.default)(styles.iconContainer, iconContainerStyle)) },
      icon
    ),
    children
  );
};

StepLabel.muiName = 'StepLabel';

process.env.NODE_ENV !== "production" ? StepLabel.propTypes = {
  /**
   * Sets active styling. Overrides disabled coloring.
   */
  active: _react.PropTypes.bool,
  /**
   * The label text node
   */
  children: _react.PropTypes.node,
  /**
   * Sets completed styling. Overrides disabled coloring.
   */
  completed: _react.PropTypes.bool,
  /**
   * Sets disabled styling.
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
  /**
   * @ignore
   */
  last: _react.PropTypes.bool,
  /**
   * Override the inline-style of the root element.
   */
  style: _react.PropTypes.object
} : void 0;

StepLabel.contextTypes = {
  muiTheme: _react.PropTypes.object.isRequired,
  stepper: _react.PropTypes.object
};

exports.default = StepLabel;
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

var _ExpandTransition = require('../internal/ExpandTransition');

var _ExpandTransition2 = _interopRequireDefault(_ExpandTransition);

var _warning = require('warning');

var _warning2 = _interopRequireDefault(_warning);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

function ExpandTransition(props) {
  return _react2.default.createElement(_ExpandTransition2.default, props);
}

var getStyles = function getStyles(props, context) {
  var styles = {
    root: {
      marginTop: -14,
      marginLeft: 14 + 11, // padding + 1/2 icon
      paddingLeft: 24 - 11 + 8,
      paddingRight: 16,
      overflow: 'hidden'
    }
  };

  if (!props.last) {
    styles.root.borderLeft = '1px solid ' + context.muiTheme.stepper.connectorLineColor;
  }

  return styles;
};

var StepContent = function (_Component) {
  (0, _inherits3.default)(StepContent, _Component);

  function StepContent() {
    (0, _classCallCheck3.default)(this, StepContent);
    return (0, _possibleConstructorReturn3.default)(this, (StepContent.__proto__ || (0, _getPrototypeOf2.default)(StepContent)).apply(this, arguments));
  }

  (0, _createClass3.default)(StepContent, [{
    key: 'render',
    value: function render() {
      var _props = this.props,
          active = _props.active,
          children = _props.children,
          completed = _props.completed,
          last = _props.last,
          style = _props.style,
          transition = _props.transition,
          transitionDuration = _props.transitionDuration,
          other = (0, _objectWithoutProperties3.default)(_props, ['active', 'children', 'completed', 'last', 'style', 'transition', 'transitionDuration']);
      var _context = this.context,
          stepper = _context.stepper,
          prepareStyles = _context.muiTheme.prepareStyles;


      if (stepper.orientation !== 'vertical') {
        process.env.NODE_ENV !== "production" ? (0, _warning2.default)(false, 'Material-UI: <StepContent /> is only designed for use with the vertical stepper.') : void 0;
        return null;
      }

      var styles = getStyles(this.props, this.context);
      var transitionProps = {
        enterDelay: transitionDuration,
        transitionDuration: transitionDuration,
        open: active
      };

      return _react2.default.createElement(
        'div',
        (0, _extends3.default)({ style: prepareStyles((0, _simpleAssign2.default)(styles.root, style)) }, other),
        _react2.default.createElement(transition, transitionProps, _react2.default.createElement(
          'div',
          { style: { overflow: 'hidden' } },
          children
        ))
      );
    }
  }]);
  return StepContent;
}(_react.Component);

StepContent.defaultProps = {
  transition: ExpandTransition,
  transitionDuration: 450
};
StepContent.contextTypes = {
  muiTheme: _react.PropTypes.object.isRequired,
  stepper: _react.PropTypes.object
};
process.env.NODE_ENV !== "production" ? StepContent.propTypes = {
  /**
   * Expands the content
   */
  active: _react.PropTypes.bool,
  /**
   * Step content
   */
  children: _react.PropTypes.node,
  /**
   * @ignore
   */
  completed: _react.PropTypes.bool,
  /**
   * @ignore
   */
  last: _react.PropTypes.bool,
  /**
   * Override the inline-style of the root element.
   */
  style: _react.PropTypes.object,
  /**
   * ReactTransitionGroup component.
   */
  transition: _react.PropTypes.func,
  /**
   * Adjust the duration of the content expand transition. Passed as a prop to the transition component.
   */
  transitionDuration: _react.PropTypes.number
} : void 0;
exports.default = StepContent;
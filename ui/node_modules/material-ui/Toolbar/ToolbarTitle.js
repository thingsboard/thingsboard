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

function getStyles(props, context) {
  var _context$muiTheme = context.muiTheme,
      baseTheme = _context$muiTheme.baseTheme,
      toolbar = _context$muiTheme.toolbar;


  return {
    root: {
      paddingRight: baseTheme.spacing.desktopGutterLess,
      lineHeight: toolbar.height + 'px',
      fontSize: toolbar.titleFontSize,
      fontFamily: baseTheme.fontFamily,
      position: 'relative',
      textOverflow: 'ellipsis',
      whiteSpace: 'nowrap',
      overflow: 'hidden'
    }
  };
}

var ToolbarTitle = function (_Component) {
  (0, _inherits3.default)(ToolbarTitle, _Component);

  function ToolbarTitle() {
    (0, _classCallCheck3.default)(this, ToolbarTitle);
    return (0, _possibleConstructorReturn3.default)(this, (ToolbarTitle.__proto__ || (0, _getPrototypeOf2.default)(ToolbarTitle)).apply(this, arguments));
  }

  (0, _createClass3.default)(ToolbarTitle, [{
    key: 'render',
    value: function render() {
      var _props = this.props,
          className = _props.className,
          style = _props.style,
          text = _props.text,
          other = (0, _objectWithoutProperties3.default)(_props, ['className', 'style', 'text']);
      var prepareStyles = this.context.muiTheme.prepareStyles;

      var styles = getStyles(this.props, this.context);

      return _react2.default.createElement(
        'span',
        (0, _extends3.default)({}, other, { className: className, style: prepareStyles((0, _simpleAssign2.default)({}, styles.root, style)) }),
        text
      );
    }
  }]);
  return ToolbarTitle;
}(_react.Component);

ToolbarTitle.muiName = 'ToolbarTitle';
ToolbarTitle.contextTypes = {
  muiTheme: _react.PropTypes.object.isRequired
};
process.env.NODE_ENV !== "production" ? ToolbarTitle.propTypes = {
  /**
   * The css class name of the root element.
   */
  className: _react.PropTypes.string,
  /**
   * Override the inline-styles of the root element.
   */
  style: _react.PropTypes.object,
  /**
   * The text to be displayed.
   */
  text: _react.PropTypes.string
} : void 0;
exports.default = ToolbarTitle;
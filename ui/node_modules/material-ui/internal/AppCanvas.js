'use strict';

Object.defineProperty(exports, "__esModule", {
  value: true
});

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

var AppCanvas = function (_Component) {
  (0, _inherits3.default)(AppCanvas, _Component);

  function AppCanvas() {
    (0, _classCallCheck3.default)(this, AppCanvas);
    return (0, _possibleConstructorReturn3.default)(this, (AppCanvas.__proto__ || (0, _getPrototypeOf2.default)(AppCanvas)).apply(this, arguments));
  }

  (0, _createClass3.default)(AppCanvas, [{
    key: 'render',
    value: function render() {
      var _context$muiTheme = this.context.muiTheme,
          baseTheme = _context$muiTheme.baseTheme,
          prepareStyles = _context$muiTheme.prepareStyles;


      var styles = {
        height: '100%',
        color: baseTheme.palette.textColor,
        backgroundColor: baseTheme.palette.canvasColor,
        direction: 'ltr'
      };

      var newChildren = _react2.default.Children.map(this.props.children, function (currentChild) {
        if (!currentChild) {
          // If undefined, skip it
          return null;
        }

        switch (currentChild.type.muiName) {
          case 'AppBar':
            return _react2.default.cloneElement(currentChild, {
              style: (0, _simpleAssign2.default)({}, currentChild.props.style, {
                position: 'fixed'
              })
            });
          default:
            return currentChild;
        }
      }, this);

      return _react2.default.createElement(
        'div',
        { style: prepareStyles(styles) },
        newChildren
      );
    }
  }]);
  return AppCanvas;
}(_react.Component);

AppCanvas.contextTypes = {
  muiTheme: _react.PropTypes.object.isRequired
};
process.env.NODE_ENV !== "production" ? AppCanvas.propTypes = {
  children: _react.PropTypes.node
} : void 0;
exports.default = AppCanvas;
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

var _reactEventListener = require('react-event-listener');

var _reactEventListener2 = _interopRequireDefault(_reactEventListener);

var _keycode = require('keycode');

var _keycode2 = _interopRequireDefault(_keycode);

var _Clock = require('./Clock');

var _Clock2 = _interopRequireDefault(_Clock);

var _Dialog = require('../Dialog');

var _Dialog2 = _interopRequireDefault(_Dialog);

var _FlatButton = require('../FlatButton');

var _FlatButton2 = _interopRequireDefault(_FlatButton);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

var TimePickerDialog = function (_Component) {
  (0, _inherits3.default)(TimePickerDialog, _Component);

  function TimePickerDialog() {
    var _ref;

    var _temp, _this, _ret;

    (0, _classCallCheck3.default)(this, TimePickerDialog);

    for (var _len = arguments.length, args = Array(_len), _key = 0; _key < _len; _key++) {
      args[_key] = arguments[_key];
    }

    return _ret = (_temp = (_this = (0, _possibleConstructorReturn3.default)(this, (_ref = TimePickerDialog.__proto__ || (0, _getPrototypeOf2.default)(TimePickerDialog)).call.apply(_ref, [this].concat(args))), _this), _this.state = {
      open: false
    }, _this.handleRequestClose = function () {
      _this.dismiss();
    }, _this.handleTouchTapCancel = function () {
      _this.dismiss();
    }, _this.handleTouchTapOK = function () {
      if (_this.props.onAccept) {
        _this.props.onAccept(_this.refs.clock.getSelectedTime());
      }
      _this.setState({
        open: false
      });
    }, _this.handleKeyUp = function (event) {
      switch ((0, _keycode2.default)(event)) {
        case 'enter':
          _this.handleTouchTapOK();
          break;
      }
    }, _temp), (0, _possibleConstructorReturn3.default)(_this, _ret);
  }

  (0, _createClass3.default)(TimePickerDialog, [{
    key: 'show',
    value: function show() {
      if (this.props.onShow && !this.state.open) this.props.onShow();
      this.setState({
        open: true
      });
    }
  }, {
    key: 'dismiss',
    value: function dismiss() {
      if (this.props.onDismiss && this.state.open) this.props.onDismiss();
      this.setState({
        open: false
      });
    }
  }, {
    key: 'render',
    value: function render() {
      var _props = this.props,
          bodyStyle = _props.bodyStyle,
          initialTime = _props.initialTime,
          onAccept = _props.onAccept,
          format = _props.format,
          autoOk = _props.autoOk,
          okLabel = _props.okLabel,
          cancelLabel = _props.cancelLabel,
          style = _props.style,
          other = (0, _objectWithoutProperties3.default)(_props, ['bodyStyle', 'initialTime', 'onAccept', 'format', 'autoOk', 'okLabel', 'cancelLabel', 'style']);


      var styles = {
        root: {
          fontSize: 14,
          color: this.context.muiTheme.timePicker.clockColor
        },
        dialogContent: {
          width: 280
        },
        body: {
          padding: 0
        }
      };

      var actions = [_react2.default.createElement(_FlatButton2.default, {
        key: 0,
        label: cancelLabel,
        primary: true,
        onTouchTap: this.handleTouchTapCancel
      }), _react2.default.createElement(_FlatButton2.default, {
        key: 1,
        label: okLabel,
        primary: true,
        onTouchTap: this.handleTouchTapOK
      })];

      var onClockChangeMinutes = autoOk === true ? this.handleTouchTapOK : undefined;
      var open = this.state.open;

      return _react2.default.createElement(
        _Dialog2.default,
        (0, _extends3.default)({}, other, {
          style: (0, _simpleAssign2.default)(styles.root, style),
          bodyStyle: (0, _simpleAssign2.default)(styles.body, bodyStyle),
          actions: actions,
          contentStyle: styles.dialogContent,
          repositionOnUpdate: false,
          open: open,
          onRequestClose: this.handleRequestClose
        }),
        open && _react2.default.createElement(_reactEventListener2.default, { target: 'window', onKeyUp: this.handleKeyUp }),
        open && _react2.default.createElement(_Clock2.default, {
          ref: 'clock',
          format: format,
          initialTime: initialTime,
          onChangeMinutes: onClockChangeMinutes
        })
      );
    }
  }]);
  return TimePickerDialog;
}(_react.Component);

TimePickerDialog.defaultProps = {
  okLabel: 'OK',
  cancelLabel: 'Cancel'
};
TimePickerDialog.contextTypes = {
  muiTheme: _react.PropTypes.object.isRequired
};
process.env.NODE_ENV !== "production" ? TimePickerDialog.propTypes = {
  autoOk: _react.PropTypes.bool,
  bodyStyle: _react.PropTypes.object,
  cancelLabel: _react.PropTypes.node,
  format: _react.PropTypes.oneOf(['ampm', '24hr']),
  initialTime: _react.PropTypes.object,
  okLabel: _react.PropTypes.node,
  onAccept: _react.PropTypes.func,
  onDismiss: _react.PropTypes.func,
  onShow: _react.PropTypes.func,
  style: _react.PropTypes.object
} : void 0;
exports.default = TimePickerDialog;
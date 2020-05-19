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

var _react = require('react');

var _react2 = _interopRequireDefault(_react);

var _TimeDisplay = require('./TimeDisplay');

var _TimeDisplay2 = _interopRequireDefault(_TimeDisplay);

var _ClockHours = require('./ClockHours');

var _ClockHours2 = _interopRequireDefault(_ClockHours);

var _ClockMinutes = require('./ClockMinutes');

var _ClockMinutes2 = _interopRequireDefault(_ClockMinutes);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

var Clock = function (_Component) {
  (0, _inherits3.default)(Clock, _Component);

  function Clock() {
    var _ref;

    var _temp, _this, _ret;

    (0, _classCallCheck3.default)(this, Clock);

    for (var _len = arguments.length, args = Array(_len), _key = 0; _key < _len; _key++) {
      args[_key] = arguments[_key];
    }

    return _ret = (_temp = (_this = (0, _possibleConstructorReturn3.default)(this, (_ref = Clock.__proto__ || (0, _getPrototypeOf2.default)(Clock)).call.apply(_ref, [this].concat(args))), _this), _this.state = {
      selectedTime: null,
      mode: 'hour'
    }, _this.setMode = function (mode) {
      setTimeout(function () {
        _this.setState({
          mode: mode
        });
      }, 100);
    }, _this.handleSelectAffix = function (affix) {
      if (affix === _this.getAffix()) return;

      var hours = _this.state.selectedTime.getHours();

      if (affix === 'am') {
        _this.handleChangeHours(hours - 12, affix);
        return;
      }

      _this.handleChangeHours(hours + 12, affix);
    }, _this.handleChangeHours = function (hours, finished) {
      var time = new Date(_this.state.selectedTime);
      var affix = void 0;

      if (typeof finished === 'string') {
        affix = finished;
        finished = undefined;
      }
      if (!affix) {
        affix = _this.getAffix();
      }
      if (affix === 'pm' && hours < 12) {
        hours += 12;
      }

      time.setHours(hours);
      _this.setState({
        selectedTime: time
      });

      if (finished) {
        setTimeout(function () {
          _this.setState({
            mode: 'minute'
          });

          var onChangeHours = _this.props.onChangeHours;

          if (onChangeHours) {
            onChangeHours(time);
          }
        }, 100);
      }
    }, _this.handleChangeMinutes = function (minutes, finished) {
      var time = new Date(_this.state.selectedTime);
      time.setMinutes(minutes);
      _this.setState({
        selectedTime: time
      });

      var onChangeMinutes = _this.props.onChangeMinutes;

      if (onChangeMinutes && finished) {
        setTimeout(function () {
          onChangeMinutes(time);
        }, 0);
      }
    }, _temp), (0, _possibleConstructorReturn3.default)(_this, _ret);
  }

  (0, _createClass3.default)(Clock, [{
    key: 'componentWillMount',
    value: function componentWillMount() {
      this.setState({
        selectedTime: this.props.initialTime || new Date()
      });
    }
  }, {
    key: 'getAffix',
    value: function getAffix() {
      if (this.props.format !== 'ampm') return '';

      var hours = this.state.selectedTime.getHours();
      if (hours < 12) {
        return 'am';
      }

      return 'pm';
    }
  }, {
    key: 'getSelectedTime',
    value: function getSelectedTime() {
      return this.state.selectedTime;
    }
  }, {
    key: 'render',
    value: function render() {
      var clock = null;

      var _context$muiTheme = this.context.muiTheme,
          prepareStyles = _context$muiTheme.prepareStyles,
          timePicker = _context$muiTheme.timePicker;


      var styles = {
        root: {
          userSelect: 'none'
        },
        container: {
          height: 280,
          padding: 10,
          position: 'relative',
          boxSizing: 'content-box'
        },
        circle: {
          position: 'absolute',
          top: 20,
          width: 260,
          height: 260,
          borderRadius: '100%',
          backgroundColor: timePicker.clockCircleColor
        }
      };

      if (this.state.mode === 'hour') {
        clock = _react2.default.createElement(_ClockHours2.default, {
          key: 'hours',
          format: this.props.format,
          onChange: this.handleChangeHours,
          initialHours: this.state.selectedTime.getHours()
        });
      } else {
        clock = _react2.default.createElement(_ClockMinutes2.default, {
          key: 'minutes',
          onChange: this.handleChangeMinutes,
          initialMinutes: this.state.selectedTime.getMinutes()
        });
      }

      return _react2.default.createElement(
        'div',
        { style: prepareStyles(styles.root) },
        _react2.default.createElement(_TimeDisplay2.default, {
          selectedTime: this.state.selectedTime,
          mode: this.state.mode,
          format: this.props.format,
          affix: this.getAffix(),
          onSelectAffix: this.handleSelectAffix,
          onSelectHour: this.setMode.bind(this, 'hour'),
          onSelectMin: this.setMode.bind(this, 'minute')
        }),
        _react2.default.createElement(
          'div',
          { style: prepareStyles(styles.container) },
          _react2.default.createElement('div', { style: prepareStyles(styles.circle) }),
          clock
        )
      );
    }
  }]);
  return Clock;
}(_react.Component);

Clock.defaultProps = {
  initialTime: new Date()
};
Clock.contextTypes = {
  muiTheme: _react.PropTypes.object.isRequired
};
process.env.NODE_ENV !== "production" ? Clock.propTypes = {
  format: _react.PropTypes.oneOf(['ampm', '24hr']),
  initialTime: _react.PropTypes.object,
  onChangeHours: _react.PropTypes.func,
  onChangeMinutes: _react.PropTypes.func
} : void 0;
exports.default = Clock;
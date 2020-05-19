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

var _dateUtils = require('./dateUtils');

var _DayButton = require('./DayButton');

var _DayButton2 = _interopRequireDefault(_DayButton);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

var styles = {
  root: {
    display: 'flex',
    flexDirection: 'column',
    justifyContent: 'flex-start',
    fontWeight: 400,
    height: 228,
    lineHeight: 2,
    position: 'relative',
    textAlign: 'center',
    MozPaddingStart: 0
  },
  week: {
    display: 'flex',
    flexDirection: 'row',
    justifyContent: 'space-around',
    height: 34,
    marginBottom: 2
  }
};

var CalendarMonth = function (_Component) {
  (0, _inherits3.default)(CalendarMonth, _Component);

  function CalendarMonth() {
    var _ref;

    var _temp, _this, _ret;

    (0, _classCallCheck3.default)(this, CalendarMonth);

    for (var _len = arguments.length, args = Array(_len), _key = 0; _key < _len; _key++) {
      args[_key] = arguments[_key];
    }

    return _ret = (_temp = (_this = (0, _possibleConstructorReturn3.default)(this, (_ref = CalendarMonth.__proto__ || (0, _getPrototypeOf2.default)(CalendarMonth)).call.apply(_ref, [this].concat(args))), _this), _this.handleTouchTapDay = function (event, date) {
      if (_this.props.onTouchTapDay) {
        _this.props.onTouchTapDay(event, date);
      }
    }, _temp), (0, _possibleConstructorReturn3.default)(_this, _ret);
  }

  (0, _createClass3.default)(CalendarMonth, [{
    key: 'isSelectedDateDisabled',
    value: function isSelectedDateDisabled() {
      return this.selectedDateDisabled;
    }
  }, {
    key: 'shouldDisableDate',
    value: function shouldDisableDate(day) {
      if (day === null) return false;
      var disabled = !(0, _dateUtils.isBetweenDates)(day, this.props.minDate, this.props.maxDate);
      if (!disabled && this.props.shouldDisableDate) disabled = this.props.shouldDisableDate(day);

      return disabled;
    }
  }, {
    key: 'getWeekElements',
    value: function getWeekElements() {
      var _this2 = this;

      var weekArray = (0, _dateUtils.getWeekArray)(this.props.displayDate, this.props.firstDayOfWeek);

      return weekArray.map(function (week, i) {
        return _react2.default.createElement(
          'div',
          { key: i, style: styles.week },
          _this2.getDayElements(week, i)
        );
      }, this);
    }
  }, {
    key: 'getDayElements',
    value: function getDayElements(week, i) {
      var _this3 = this;

      var _props = this.props,
          DateTimeFormat = _props.DateTimeFormat,
          locale = _props.locale,
          selectedDate = _props.selectedDate;


      return week.map(function (day, j) {
        var isSameDate = (0, _dateUtils.isEqualDate)(selectedDate, day);
        var disabled = _this3.shouldDisableDate(day);
        var selected = !disabled && isSameDate;

        if (isSameDate) {
          _this3.selectedDateDisabled = disabled;
        }

        return _react2.default.createElement(_DayButton2.default, {
          DateTimeFormat: DateTimeFormat,
          locale: locale,
          date: day,
          disabled: disabled,
          key: 'db' + (i + j),
          onTouchTap: _this3.handleTouchTapDay,
          selected: selected
        });
      }, this);
    }
  }, {
    key: 'render',
    value: function render() {
      return _react2.default.createElement(
        'div',
        { style: styles.root },
        this.getWeekElements()
      );
    }
  }]);
  return CalendarMonth;
}(_react.Component);

process.env.NODE_ENV !== "production" ? CalendarMonth.propTypes = {
  DateTimeFormat: _react.PropTypes.func.isRequired,
  autoOk: _react.PropTypes.bool,
  displayDate: _react.PropTypes.object.isRequired,
  firstDayOfWeek: _react.PropTypes.number,
  locale: _react.PropTypes.string.isRequired,
  maxDate: _react.PropTypes.object,
  minDate: _react.PropTypes.object,
  onTouchTapDay: _react.PropTypes.func,
  selectedDate: _react.PropTypes.object.isRequired,
  shouldDisableDate: _react.PropTypes.func
} : void 0;
exports.default = CalendarMonth;
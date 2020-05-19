'use strict';

Object.defineProperty(exports, "__esModule", {
  value: true
});

var _extends2 = require('babel-runtime/helpers/extends');

var _extends3 = _interopRequireDefault(_extends2);

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

var _reactDom = require('react-dom');

var _reactDom2 = _interopRequireDefault(_reactDom);

var _YearButton = require('./YearButton');

var _YearButton2 = _interopRequireDefault(_YearButton);

var _dateUtils = require('./dateUtils');

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

var CalendarYear = function (_Component) {
  (0, _inherits3.default)(CalendarYear, _Component);

  function CalendarYear() {
    var _ref;

    var _temp, _this, _ret;

    (0, _classCallCheck3.default)(this, CalendarYear);

    for (var _len = arguments.length, args = Array(_len), _key = 0; _key < _len; _key++) {
      args[_key] = arguments[_key];
    }

    return _ret = (_temp = (_this = (0, _possibleConstructorReturn3.default)(this, (_ref = CalendarYear.__proto__ || (0, _getPrototypeOf2.default)(CalendarYear)).call.apply(_ref, [this].concat(args))), _this), _this.handleTouchTapYear = function (event, year) {
      if (_this.props.onTouchTapYear) {
        _this.props.onTouchTapYear(event, year);
      }
    }, _temp), (0, _possibleConstructorReturn3.default)(_this, _ret);
  }

  (0, _createClass3.default)(CalendarYear, [{
    key: 'componentDidMount',
    value: function componentDidMount() {
      this.scrollToSelectedYear();
    }
  }, {
    key: 'componentDidUpdate',
    value: function componentDidUpdate() {
      this.scrollToSelectedYear();
    }
  }, {
    key: 'getYears',
    value: function getYears() {
      var _props = this.props,
          DateTimeFormat = _props.DateTimeFormat,
          locale = _props.locale,
          minDate = _props.minDate,
          maxDate = _props.maxDate,
          selectedDate = _props.selectedDate;


      var minYear = minDate.getFullYear();
      var maxYear = maxDate.getFullYear();
      var years = [];
      var dateCheck = (0, _dateUtils.cloneDate)(selectedDate);

      for (var year = minYear; year <= maxYear; year++) {
        dateCheck.setFullYear(year);
        var selected = selectedDate.getFullYear() === year;
        var selectedProps = {};
        if (selected) {
          selectedProps.ref = 'selectedYearButton';
        }

        var yearFormated = new DateTimeFormat(locale, {
          year: 'numeric'
        }).format(dateCheck);

        var yearButton = _react2.default.createElement(
          _YearButton2.default,
          (0, _extends3.default)({
            key: 'yb' + year,
            onTouchTap: this.handleTouchTapYear,
            selected: selected,
            year: year
          }, selectedProps),
          yearFormated
        );

        years.push(yearButton);
      }

      return years;
    }
  }, {
    key: 'scrollToSelectedYear',
    value: function scrollToSelectedYear() {
      if (this.refs.selectedYearButton === undefined) {
        return;
      }

      var container = _reactDom2.default.findDOMNode(this);
      var yearButtonNode = _reactDom2.default.findDOMNode(this.refs.selectedYearButton);

      var containerHeight = container.clientHeight;
      var yearButtonNodeHeight = yearButtonNode.clientHeight || 32;

      var scrollYOffset = yearButtonNode.offsetTop + yearButtonNodeHeight / 2 - containerHeight / 2;
      container.scrollTop = scrollYOffset;
    }
  }, {
    key: 'render',
    value: function render() {
      var _context$muiTheme = this.context.muiTheme,
          prepareStyles = _context$muiTheme.prepareStyles,
          calendarYearBackgroundColor = _context$muiTheme.datePicker.calendarYearBackgroundColor;


      var styles = {
        root: {
          backgroundColor: calendarYearBackgroundColor,
          height: 'inherit',
          lineHeight: '35px',
          overflowX: 'hidden',
          overflowY: 'scroll',
          position: 'relative'
        },
        child: {
          display: 'flex',
          flexDirection: 'column',
          justifyContent: 'center',
          minHeight: '100%'
        }
      };

      return _react2.default.createElement(
        'div',
        { style: prepareStyles(styles.root) },
        _react2.default.createElement(
          'div',
          { style: prepareStyles(styles.child) },
          this.getYears()
        )
      );
    }
  }]);
  return CalendarYear;
}(_react.Component);

CalendarYear.contextTypes = {
  muiTheme: _react.PropTypes.object.isRequired
};
process.env.NODE_ENV !== "production" ? CalendarYear.propTypes = {
  DateTimeFormat: _react.PropTypes.func.isRequired,
  locale: _react.PropTypes.string.isRequired,
  maxDate: _react.PropTypes.object.isRequired,
  minDate: _react.PropTypes.object.isRequired,
  onTouchTapYear: _react.PropTypes.func,
  selectedDate: _react.PropTypes.object.isRequired,
  wordings: _react.PropTypes.object
} : void 0;
exports.default = CalendarYear;
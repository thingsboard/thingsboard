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

var _slicedToArray2 = require('babel-runtime/helpers/slicedToArray');

var _slicedToArray3 = _interopRequireDefault(_slicedToArray2);

var _react = require('react');

var _react2 = _interopRequireDefault(_react);

var _timeUtils = require('./timeUtils');

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

function getStyles(props, context) {
  var styles = {
    root: {
      directionInvariant: true,
      display: 'inline-block',
      position: 'absolute',
      width: 32,
      height: 32,
      borderRadius: '100%',
      left: 'calc(50% - 16px)',
      top: 10,
      textAlign: 'center',
      paddingTop: 5,
      userSelect: 'none', /* Chrome all / Safari all */
      fontSize: '1.1em',
      pointerEvents: 'none',
      boxSizing: 'border-box'
    }
  };

  var muiTheme = context.muiTheme;


  var pos = props.value;

  if (props.type === 'hour') {
    pos %= 12;
  } else {
    pos = pos / 5;
  }

  var positions = [[0, 5], [54.5, 16.6], [94.4, 59.5], [109, 114], [94.4, 168.5], [54.5, 208.4], [0, 223], [-54.5, 208.4], [-94.4, 168.5], [-109, 114], [-94.4, 59.5], [-54.5, 19.6]];

  var innerPositions = [[0, 40], [36.9, 49.9], [64, 77], [74, 114], [64, 151], [37, 178], [0, 188], [-37, 178], [-64, 151], [-74, 114], [-64, 77], [-37, 50]];

  if (props.isSelected) {
    styles.root.backgroundColor = muiTheme.timePicker.accentColor;
    styles.root.color = muiTheme.timePicker.selectTextColor;
  }

  var transformPos = positions[pos];

  if ((0, _timeUtils.isInner)(props)) {
    styles.root.width = 28;
    styles.root.height = 28;
    styles.root.left = 'calc(50% - 14px)';
    transformPos = innerPositions[pos];
  }

  var _transformPos = transformPos,
      _transformPos2 = (0, _slicedToArray3.default)(_transformPos, 2),
      x = _transformPos2[0],
      y = _transformPos2[1];

  styles.root.transform = 'translate(' + x + 'px, ' + y + 'px)';

  return styles;
}

var ClockNumber = function (_Component) {
  (0, _inherits3.default)(ClockNumber, _Component);

  function ClockNumber() {
    (0, _classCallCheck3.default)(this, ClockNumber);
    return (0, _possibleConstructorReturn3.default)(this, (ClockNumber.__proto__ || (0, _getPrototypeOf2.default)(ClockNumber)).apply(this, arguments));
  }

  (0, _createClass3.default)(ClockNumber, [{
    key: 'render',
    value: function render() {
      var prepareStyles = this.context.muiTheme.prepareStyles;

      var styles = getStyles(this.props, this.context);
      var clockNumber = this.props.value === 0 ? '00' : this.props.value;

      return _react2.default.createElement(
        'span',
        { style: prepareStyles(styles.root) },
        clockNumber
      );
    }
  }]);
  return ClockNumber;
}(_react.Component);

ClockNumber.defaultProps = {
  value: 0,
  type: 'minute',
  isSelected: false
};
ClockNumber.contextTypes = {
  muiTheme: _react.PropTypes.object.isRequired
};
process.env.NODE_ENV !== "production" ? ClockNumber.propTypes = {
  isSelected: _react.PropTypes.bool,
  onSelected: _react.PropTypes.func,
  type: _react.PropTypes.oneOf(['hour', 'minute']),
  value: _react.PropTypes.number
} : void 0;
exports.default = ClockNumber;
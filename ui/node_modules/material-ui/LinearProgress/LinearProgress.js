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

var _transitions = require('../styles/transitions');

var _transitions2 = _interopRequireDefault(_transitions);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

function getRelativeValue(value, min, max) {
  var clampedValue = Math.min(Math.max(min, value), max);
  var rangeValue = max - min;
  var relValue = Math.round((clampedValue - min) / rangeValue * 10000) / 10000;
  return relValue * 100;
}

function getStyles(props, context) {
  var max = props.max,
      min = props.min,
      value = props.value;
  var palette = context.muiTheme.baseTheme.palette;


  var styles = {
    root: {
      position: 'relative',
      height: 4,
      display: 'block',
      width: '100%',
      backgroundColor: palette.primary3Color,
      borderRadius: 2,
      margin: 0,
      overflow: 'hidden'
    },
    bar: {
      height: '100%'
    },
    barFragment1: {},
    barFragment2: {}
  };

  if (props.mode === 'indeterminate') {
    styles.barFragment1 = {
      position: 'absolute',
      backgroundColor: props.color || palette.primary1Color,
      top: 0,
      left: 0,
      bottom: 0,
      transition: _transitions2.default.create('all', '840ms', null, 'cubic-bezier(0.650, 0.815, 0.735, 0.395)')
    };

    styles.barFragment2 = {
      position: 'absolute',
      backgroundColor: props.color || palette.primary1Color,
      top: 0,
      left: 0,
      bottom: 0,
      transition: _transitions2.default.create('all', '840ms', null, 'cubic-bezier(0.165, 0.840, 0.440, 1.000)')
    };
  } else {
    styles.bar.backgroundColor = props.color || palette.primary1Color;
    styles.bar.transition = _transitions2.default.create('width', '.3s', null, 'linear');
    styles.bar.width = getRelativeValue(value, min, max) + '%';
  }

  return styles;
}

var LinearProgress = function (_Component) {
  (0, _inherits3.default)(LinearProgress, _Component);

  function LinearProgress() {
    (0, _classCallCheck3.default)(this, LinearProgress);
    return (0, _possibleConstructorReturn3.default)(this, (LinearProgress.__proto__ || (0, _getPrototypeOf2.default)(LinearProgress)).apply(this, arguments));
  }

  (0, _createClass3.default)(LinearProgress, [{
    key: 'componentDidMount',
    value: function componentDidMount() {
      var _this2 = this;

      this.timers = {};

      this.timers.bar1 = this.barUpdate('bar1', 0, this.refs.bar1, [[-35, 100], [100, -90]]);

      this.timers.bar2 = setTimeout(function () {
        _this2.barUpdate('bar2', 0, _this2.refs.bar2, [[-200, 100], [107, -8]]);
      }, 850);
    }
  }, {
    key: 'componentWillUnmount',
    value: function componentWillUnmount() {
      clearTimeout(this.timers.bar1);
      clearTimeout(this.timers.bar2);
    }
  }, {
    key: 'barUpdate',
    value: function barUpdate(id, step, barElement, stepValues) {
      var _this3 = this;

      if (this.props.mode !== 'indeterminate') return;

      step = step || 0;
      step %= 4;

      var right = this.context.muiTheme.isRtl ? 'left' : 'right';
      var left = this.context.muiTheme.isRtl ? 'right' : 'left';

      if (step === 0) {
        barElement.style[left] = stepValues[0][0] + '%';
        barElement.style[right] = stepValues[0][1] + '%';
      } else if (step === 1) {
        barElement.style.transitionDuration = '840ms';
      } else if (step === 2) {
        barElement.style[left] = stepValues[1][0] + '%';
        barElement.style[right] = stepValues[1][1] + '%';
      } else if (step === 3) {
        barElement.style.transitionDuration = '0ms';
      }
      this.timers[id] = setTimeout(function () {
        return _this3.barUpdate(id, step + 1, barElement, stepValues);
      }, 420);
    }
  }, {
    key: 'render',
    value: function render() {
      var _props = this.props,
          style = _props.style,
          other = (0, _objectWithoutProperties3.default)(_props, ['style']);
      var prepareStyles = this.context.muiTheme.prepareStyles;

      var styles = getStyles(this.props, this.context);

      return _react2.default.createElement(
        'div',
        (0, _extends3.default)({}, other, { style: prepareStyles((0, _simpleAssign2.default)(styles.root, style)) }),
        _react2.default.createElement(
          'div',
          { style: prepareStyles(styles.bar) },
          _react2.default.createElement('div', { ref: 'bar1', style: prepareStyles(styles.barFragment1) }),
          _react2.default.createElement('div', { ref: 'bar2', style: prepareStyles(styles.barFragment2) })
        )
      );
    }
  }]);
  return LinearProgress;
}(_react.Component);

LinearProgress.defaultProps = {
  mode: 'indeterminate',
  value: 0,
  min: 0,
  max: 100
};
LinearProgress.contextTypes = {
  muiTheme: _react.PropTypes.object.isRequired
};
process.env.NODE_ENV !== "production" ? LinearProgress.propTypes = {
  /**
   * The color of the progress bar, defaults to
   * primary color of theme.
   */
  color: _react.PropTypes.string,
  /**
   * The max value of progress, only works in determinate mode.
   */
  max: _react.PropTypes.number,
  /**
   * The min value of progress, only works in determinate mode.
   */
  min: _react.PropTypes.number,
  /**
   * The mode of show your progress, indeterminate for when
   * there is no value for progress.
   */
  mode: _react.PropTypes.oneOf(['determinate', 'indeterminate']),
  /**
   * Override the inline-styles of the root element.
   */
  style: _react.PropTypes.object,
  /**
   * The value of progress, only works in determinate mode.
   */
  value: _react.PropTypes.number
} : void 0;
exports.default = LinearProgress;
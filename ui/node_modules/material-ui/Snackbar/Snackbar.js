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

var _ClickAwayListener = require('../internal/ClickAwayListener');

var _ClickAwayListener2 = _interopRequireDefault(_ClickAwayListener);

var _SnackbarBody = require('./SnackbarBody');

var _SnackbarBody2 = _interopRequireDefault(_SnackbarBody);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

function getStyles(props, context, state) {
  var _context$muiTheme = context.muiTheme,
      desktopSubheaderHeight = _context$muiTheme.baseTheme.spacing.desktopSubheaderHeight,
      zIndex = _context$muiTheme.zIndex;
  var open = state.open;


  var styles = {
    root: {
      position: 'fixed',
      left: '50%',
      display: 'flex',
      bottom: 0,
      zIndex: zIndex.snackbar,
      visibility: open ? 'visible' : 'hidden',
      transform: open ? 'translate(-50%, 0)' : 'translate(-50%, ' + desktopSubheaderHeight + 'px)',
      transition: _transitions2.default.easeOut('400ms', 'transform') + ', ' + _transitions2.default.easeOut('400ms', 'visibility')
    }
  };

  return styles;
}

var Snackbar = function (_Component) {
  (0, _inherits3.default)(Snackbar, _Component);

  function Snackbar() {
    var _ref;

    var _temp, _this, _ret;

    (0, _classCallCheck3.default)(this, Snackbar);

    for (var _len = arguments.length, args = Array(_len), _key = 0; _key < _len; _key++) {
      args[_key] = arguments[_key];
    }

    return _ret = (_temp = (_this = (0, _possibleConstructorReturn3.default)(this, (_ref = Snackbar.__proto__ || (0, _getPrototypeOf2.default)(Snackbar)).call.apply(_ref, [this].concat(args))), _this), _this.componentClickAway = function () {
      if (_this.timerTransitionId) {
        // If transitioning, don't close the snackbar.
        return;
      }

      if (_this.props.open !== null && _this.props.onRequestClose) {
        _this.props.onRequestClose('clickaway');
      } else {
        _this.setState({ open: false });
      }
    }, _temp), (0, _possibleConstructorReturn3.default)(_this, _ret);
  }

  (0, _createClass3.default)(Snackbar, [{
    key: 'componentWillMount',
    value: function componentWillMount() {
      this.setState({
        open: this.props.open,
        message: this.props.message,
        action: this.props.action
      });
    }
  }, {
    key: 'componentDidMount',
    value: function componentDidMount() {
      if (this.state.open) {
        this.setAutoHideTimer();
        this.setTransitionTimer();
      }
    }
  }, {
    key: 'componentWillReceiveProps',
    value: function componentWillReceiveProps(nextProps) {
      var _this2 = this;

      if (this.props.open && nextProps.open && (nextProps.message !== this.props.message || nextProps.action !== this.props.action)) {
        this.setState({
          open: false
        });

        clearTimeout(this.timerOneAtTheTimeId);
        this.timerOneAtTheTimeId = setTimeout(function () {
          _this2.setState({
            message: nextProps.message,
            action: nextProps.action,
            open: true
          });
        }, 400);
      } else {
        var open = nextProps.open;

        this.setState({
          open: open !== null ? open : this.state.open,
          message: nextProps.message,
          action: nextProps.action
        });
      }
    }
  }, {
    key: 'componentDidUpdate',
    value: function componentDidUpdate(prevProps, prevState) {
      if (prevState.open !== this.state.open) {
        if (this.state.open) {
          this.setAutoHideTimer();
          this.setTransitionTimer();
        } else {
          clearTimeout(this.timerAutoHideId);
        }
      }
    }
  }, {
    key: 'componentWillUnmount',
    value: function componentWillUnmount() {
      clearTimeout(this.timerAutoHideId);
      clearTimeout(this.timerTransitionId);
      clearTimeout(this.timerOneAtTheTimeId);
    }
  }, {
    key: 'setAutoHideTimer',


    // Timer that controls delay before snackbar auto hides
    value: function setAutoHideTimer() {
      var _this3 = this;

      var autoHideDuration = this.props.autoHideDuration;

      if (autoHideDuration > 0) {
        clearTimeout(this.timerAutoHideId);
        this.timerAutoHideId = setTimeout(function () {
          if (_this3.props.open !== null && _this3.props.onRequestClose) {
            _this3.props.onRequestClose('timeout');
          } else {
            _this3.setState({ open: false });
          }
        }, autoHideDuration);
      }
    }

    // Timer that controls delay before click-away events are captured (based on when animation completes)

  }, {
    key: 'setTransitionTimer',
    value: function setTransitionTimer() {
      var _this4 = this;

      this.timerTransitionId = setTimeout(function () {
        _this4.timerTransitionId = undefined;
      }, 400);
    }
  }, {
    key: 'render',
    value: function render() {
      var _props = this.props,
          autoHideDuration = _props.autoHideDuration,
          contentStyle = _props.contentStyle,
          bodyStyle = _props.bodyStyle,
          messageProp = _props.message,
          onRequestClose = _props.onRequestClose,
          onActionTouchTap = _props.onActionTouchTap,
          style = _props.style,
          other = (0, _objectWithoutProperties3.default)(_props, ['autoHideDuration', 'contentStyle', 'bodyStyle', 'message', 'onRequestClose', 'onActionTouchTap', 'style']);
      var _state = this.state,
          action = _state.action,
          message = _state.message,
          open = _state.open;
      var prepareStyles = this.context.muiTheme.prepareStyles;

      var styles = getStyles(this.props, this.context, this.state);

      return _react2.default.createElement(
        _ClickAwayListener2.default,
        { onClickAway: open ? this.componentClickAway : null },
        _react2.default.createElement(
          'div',
          (0, _extends3.default)({}, other, { style: prepareStyles((0, _simpleAssign2.default)(styles.root, style)) }),
          _react2.default.createElement(_SnackbarBody2.default, {
            action: action,
            contentStyle: contentStyle,
            message: message,
            open: open,
            onActionTouchTap: onActionTouchTap,
            style: bodyStyle
          })
        )
      );
    }
  }]);
  return Snackbar;
}(_react.Component);

Snackbar.contextTypes = {
  muiTheme: _react.PropTypes.object.isRequired
};
process.env.NODE_ENV !== "production" ? Snackbar.propTypes = {
  /**
   * The label for the action on the snackbar.
   */
  action: _react.PropTypes.node,
  /**
   * The number of milliseconds to wait before automatically dismissing.
   * If no value is specified the snackbar will dismiss normally.
   * If a value is provided the snackbar can still be dismissed normally.
   * If a snackbar is dismissed before the timer expires, the timer will be cleared.
   */
  autoHideDuration: _react.PropTypes.number,
  /**
   * Override the inline-styles of the body element.
   */
  bodyStyle: _react.PropTypes.object,
  /**
   * The css class name of the root element.
   */
  className: _react.PropTypes.string,
  /**
   * Override the inline-styles of the content element.
   */
  contentStyle: _react.PropTypes.object,
  /**
   * The message to be displayed.
   *
   * (Note: If the message is an element or array, and the `Snackbar` may re-render while it is still open,
   * ensure that the same object remains as the `message` property if you want to avoid the `Snackbar` hiding and
   * showing again)
   */
  message: _react.PropTypes.node.isRequired,
  /**
   * Fired when the action button is touchtapped.
   *
   * @param {object} event Action button event.
   */
  onActionTouchTap: _react.PropTypes.func,
  /**
   * Fired when the `Snackbar` is requested to be closed by a click outside the `Snackbar`, or after the
   * `autoHideDuration` timer expires.
   *
   * Typically `onRequestClose` is used to set state in the parent component, which is used to control the `Snackbar`
   * `open` prop.
   *
   * The `reason` parameter can optionally be used to control the response to `onRequestClose`,
   * for example ignoring `clickaway`.
   *
   * @param {string} reason Can be:`"timeout"` (`autoHideDuration` expired) or: `"clickaway"`
   */
  onRequestClose: _react.PropTypes.func,
  /**
   * Controls whether the `Snackbar` is opened or not.
   */
  open: _react.PropTypes.bool.isRequired,
  /**
   * Override the inline-styles of the root element.
   */
  style: _react.PropTypes.object
} : void 0;
exports.default = Snackbar;
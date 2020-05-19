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

var _reactDom = require('react-dom');

var _reactDom2 = _interopRequireDefault(_reactDom);

var _reactEventListener = require('react-event-listener');

var _reactEventListener2 = _interopRequireDefault(_reactEventListener);

var _keycode = require('keycode');

var _keycode2 = _interopRequireDefault(_keycode);

var _autoPrefix = require('../utils/autoPrefix');

var _autoPrefix2 = _interopRequireDefault(_autoPrefix);

var _transitions = require('../styles/transitions');

var _transitions2 = _interopRequireDefault(_transitions);

var _Overlay = require('../internal/Overlay');

var _Overlay2 = _interopRequireDefault(_Overlay);

var _Paper = require('../Paper');

var _Paper2 = _interopRequireDefault(_Paper);

var _propTypes = require('../utils/propTypes');

var _propTypes2 = _interopRequireDefault(_propTypes);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

var openNavEventHandler = null;

var Drawer = function (_Component) {
  (0, _inherits3.default)(Drawer, _Component);

  function Drawer() {
    var _ref;

    var _temp, _this, _ret;

    (0, _classCallCheck3.default)(this, Drawer);

    for (var _len = arguments.length, args = Array(_len), _key = 0; _key < _len; _key++) {
      args[_key] = arguments[_key];
    }

    return _ret = (_temp = (_this = (0, _possibleConstructorReturn3.default)(this, (_ref = Drawer.__proto__ || (0, _getPrototypeOf2.default)(Drawer)).call.apply(_ref, [this].concat(args))), _this), _this.handleTouchTapOverlay = function (event) {
      event.preventDefault();
      _this.close('clickaway');
    }, _this.handleKeyUp = function (event) {
      if (_this.state.open && !_this.props.docked && (0, _keycode2.default)(event) === 'esc') {
        _this.close('escape');
      }
    }, _this.onBodyTouchStart = function (event) {
      var swipeAreaWidth = _this.props.swipeAreaWidth;

      var touchStartX = event.touches[0].pageX;
      var touchStartY = event.touches[0].pageY;

      // Open only if swiping from far left (or right) while closed
      if (swipeAreaWidth !== null && !_this.state.open) {
        if (_this.props.openSecondary) {
          // If openSecondary is true calculate from the far right
          if (touchStartX < document.body.offsetWidth - swipeAreaWidth) return;
        } else {
          // If openSecondary is false calculate from the far left
          if (touchStartX > swipeAreaWidth) return;
        }
      }

      if (!_this.state.open && (openNavEventHandler !== _this.onBodyTouchStart || _this.props.disableSwipeToOpen)) {
        return;
      }

      _this.maybeSwiping = true;
      _this.touchStartX = touchStartX;
      _this.touchStartY = touchStartY;

      document.body.addEventListener('touchmove', _this.onBodyTouchMove);
      document.body.addEventListener('touchend', _this.onBodyTouchEnd);
      document.body.addEventListener('touchcancel', _this.onBodyTouchEnd);
    }, _this.onBodyTouchMove = function (event) {
      var currentX = event.touches[0].pageX;
      var currentY = event.touches[0].pageY;

      if (_this.state.swiping) {
        event.preventDefault();
        _this.setPosition(_this.getTranslateX(currentX));
      } else if (_this.maybeSwiping) {
        var dXAbs = Math.abs(currentX - _this.touchStartX);
        var dYAbs = Math.abs(currentY - _this.touchStartY);
        // If the user has moved his thumb ten pixels in either direction,
        // we can safely make an assumption about whether he was intending
        // to swipe or scroll.
        var threshold = 10;

        if (dXAbs > threshold && dYAbs <= threshold) {
          _this.swipeStartX = currentX;
          _this.setState({
            swiping: _this.state.open ? 'closing' : 'opening'
          });
          _this.setPosition(_this.getTranslateX(currentX));
        } else if (dXAbs <= threshold && dYAbs > threshold) {
          _this.onBodyTouchEnd();
        }
      }
    }, _this.onBodyTouchEnd = function (event) {
      if (_this.state.swiping) {
        var currentX = event.changedTouches[0].pageX;
        var translateRatio = _this.getTranslateX(currentX) / _this.getMaxTranslateX();

        _this.maybeSwiping = false;
        var swiping = _this.state.swiping;
        _this.setState({
          swiping: null
        });

        // We have to open or close after setting swiping to null,
        // because only then CSS transition is enabled.
        if (translateRatio > 0.5) {
          if (swiping === 'opening') {
            _this.setPosition(_this.getMaxTranslateX());
          } else {
            _this.close('swipe');
          }
        } else {
          if (swiping === 'opening') {
            _this.open('swipe');
          } else {
            _this.setPosition(0);
          }
        }
      } else {
        _this.maybeSwiping = false;
      }

      document.body.removeEventListener('touchmove', _this.onBodyTouchMove);
      document.body.removeEventListener('touchend', _this.onBodyTouchEnd);
      document.body.removeEventListener('touchcancel', _this.onBodyTouchEnd);
    }, _temp), (0, _possibleConstructorReturn3.default)(_this, _ret);
  }

  (0, _createClass3.default)(Drawer, [{
    key: 'componentWillMount',
    value: function componentWillMount() {
      this.maybeSwiping = false;
      this.touchStartX = null;
      this.touchStartY = null;
      this.swipeStartX = null;

      this.setState({
        open: this.props.open !== null ? this.props.open : this.props.docked,
        swiping: null
      });
    }
  }, {
    key: 'componentDidMount',
    value: function componentDidMount() {
      this.enableSwipeHandling();
    }
  }, {
    key: 'componentWillReceiveProps',
    value: function componentWillReceiveProps(nextProps) {
      // If controlled then the open prop takes precedence.
      if (nextProps.open !== null) {
        this.setState({
          open: nextProps.open
        });
        // Otherwise, if docked is changed, change the open state for when uncontrolled.
      } else if (this.props.docked !== nextProps.docked) {
        this.setState({
          open: nextProps.docked
        });
      }
    }
  }, {
    key: 'componentDidUpdate',
    value: function componentDidUpdate() {
      this.enableSwipeHandling();
    }
  }, {
    key: 'componentWillUnmount',
    value: function componentWillUnmount() {
      this.disableSwipeHandling();
    }
  }, {
    key: 'getStyles',
    value: function getStyles() {
      var muiTheme = this.context.muiTheme;
      var theme = muiTheme.drawer;

      var x = this.getTranslateMultiplier() * (this.state.open ? 0 : this.getMaxTranslateX());

      var styles = {
        root: {
          height: '100%',
          width: this.props.width || theme.width,
          position: 'fixed',
          zIndex: muiTheme.zIndex.drawer,
          left: 0,
          top: 0,
          transform: 'translate(' + x + 'px, 0)',
          transition: !this.state.swiping && _transitions2.default.easeOut(null, 'transform', null),
          backgroundColor: theme.color,
          overflow: 'auto',
          WebkitOverflowScrolling: 'touch' },
        overlay: {
          zIndex: muiTheme.zIndex.drawerOverlay,
          pointerEvents: this.state.open ? 'auto' : 'none' },
        rootWhenOpenRight: {
          left: 'auto',
          right: 0
        }
      };

      return styles;
    }
  }, {
    key: 'shouldShow',
    value: function shouldShow() {
      return this.state.open || !!this.state.swiping; // component is swiping
    }
  }, {
    key: 'close',
    value: function close(reason) {
      if (this.props.open === null) this.setState({ open: false });
      if (this.props.onRequestChange) this.props.onRequestChange(false, reason);
      return this;
    }
  }, {
    key: 'open',
    value: function open(reason) {
      if (this.props.open === null) this.setState({ open: true });
      if (this.props.onRequestChange) this.props.onRequestChange(true, reason);
      return this;
    }
  }, {
    key: 'getMaxTranslateX',
    value: function getMaxTranslateX() {
      var width = this.props.width || this.context.muiTheme.drawer.width;
      return width + 10;
    }
  }, {
    key: 'getTranslateMultiplier',
    value: function getTranslateMultiplier() {
      return this.props.openSecondary ? 1 : -1;
    }
  }, {
    key: 'enableSwipeHandling',
    value: function enableSwipeHandling() {
      if (!this.props.docked) {
        document.body.addEventListener('touchstart', this.onBodyTouchStart);
        if (!openNavEventHandler) {
          openNavEventHandler = this.onBodyTouchStart;
        }
      } else {
        this.disableSwipeHandling();
      }
    }
  }, {
    key: 'disableSwipeHandling',
    value: function disableSwipeHandling() {
      document.body.removeEventListener('touchstart', this.onBodyTouchStart);
      if (openNavEventHandler === this.onBodyTouchStart) {
        openNavEventHandler = null;
      }
    }
  }, {
    key: 'setPosition',
    value: function setPosition(translateX) {
      var drawer = _reactDom2.default.findDOMNode(this.refs.clickAwayableElement);
      var transformCSS = 'translate(' + this.getTranslateMultiplier() * translateX + 'px, 0)';
      this.refs.overlay.setOpacity(1 - translateX / this.getMaxTranslateX());
      _autoPrefix2.default.set(drawer.style, 'transform', transformCSS);
    }
  }, {
    key: 'getTranslateX',
    value: function getTranslateX(currentX) {
      return Math.min(Math.max(this.state.swiping === 'closing' ? this.getTranslateMultiplier() * (currentX - this.swipeStartX) : this.getMaxTranslateX() - this.getTranslateMultiplier() * (this.swipeStartX - currentX), 0), this.getMaxTranslateX());
    }
  }, {
    key: 'render',
    value: function render() {
      var _props = this.props,
          children = _props.children,
          className = _props.className,
          containerClassName = _props.containerClassName,
          containerStyle = _props.containerStyle,
          docked = _props.docked,
          openSecondary = _props.openSecondary,
          overlayClassName = _props.overlayClassName,
          overlayStyle = _props.overlayStyle,
          style = _props.style,
          zDepth = _props.zDepth;


      var styles = this.getStyles();

      var overlay = void 0;
      if (!docked) {
        overlay = _react2.default.createElement(_Overlay2.default, {
          ref: 'overlay',
          show: this.shouldShow(),
          className: overlayClassName,
          style: (0, _simpleAssign2.default)(styles.overlay, overlayStyle),
          transitionEnabled: !this.state.swiping,
          onTouchTap: this.handleTouchTapOverlay
        });
      }

      return _react2.default.createElement(
        'div',
        {
          className: className,
          style: style
        },
        _react2.default.createElement(_reactEventListener2.default, { target: 'window', onKeyUp: this.handleKeyUp }),
        overlay,
        _react2.default.createElement(
          _Paper2.default,
          {
            ref: 'clickAwayableElement',
            zDepth: zDepth,
            rounded: false,
            transitionEnabled: !this.state.swiping,
            className: containerClassName,
            style: (0, _simpleAssign2.default)(styles.root, openSecondary && styles.rootWhenOpenRight, containerStyle)
          },
          children
        )
      );
    }
  }]);
  return Drawer;
}(_react.Component);

Drawer.defaultProps = {
  disableSwipeToOpen: false,
  docked: true,
  open: null,
  openSecondary: false,
  swipeAreaWidth: 30,
  width: null,
  zDepth: 2
};
Drawer.contextTypes = {
  muiTheme: _react.PropTypes.object.isRequired
};
process.env.NODE_ENV !== "production" ? Drawer.propTypes = {
  /**
   * The contents of the `Drawer`
   */
  children: _react.PropTypes.node,
  /**
   * The CSS class name of the root element.
   */
  className: _react.PropTypes.string,
  /**
   * The CSS class name of the container element.
   */
  containerClassName: _react.PropTypes.string,
  /**
   * Override the inline-styles of the container element.
   */
  containerStyle: _react.PropTypes.object,
  /**
   * If true, swiping sideways when the `Drawer` is closed will not open it.
   */
  disableSwipeToOpen: _react.PropTypes.bool,
  /**
   * If true, the `Drawer` will be docked. In this state, the overlay won't show and
   * clicking on a menu item will not close the `Drawer`.
   */
  docked: _react.PropTypes.bool,
  /**
   * Callback function fired when the `open` state of the `Drawer` is requested to be changed.
   *
   * @param {boolean} open If true, the `Drawer` was requested to be opened.
   * @param {string} reason The reason for the open or close request. Possible values are
   * 'swipe' for open requests; 'clickaway' (on overlay clicks),
   * 'escape' (on escape key press), and 'swipe' for close requests.
   */
  onRequestChange: _react.PropTypes.func,
  /**
   * If true, the `Drawer` is opened.  Providing a value will turn the `Drawer`
   * into a controlled component.
   */
  open: _react.PropTypes.bool,
  /**
   * If true, the `Drawer` is positioned to open from the opposite side.
   */
  openSecondary: _react.PropTypes.bool,
  /**
   * The CSS class name to add to the `Overlay` component that is rendered behind the `Drawer`.
   */
  overlayClassName: _react.PropTypes.string,
  /**
   * Override the inline-styles of the `Overlay` component that is rendered behind the `Drawer`.
   */
  overlayStyle: _react.PropTypes.object,
  /**
   * Override the inline-styles of the root element.
   */
  style: _react.PropTypes.object,
  /**
   * The width of the left most (or right most) area in pixels where the `Drawer` can be
   * swiped open from. Setting this to `null` spans that area to the entire page
   * (**CAUTION!** Setting this property to `null` might cause issues with sliders and
   * swipeable `Tabs`: use at your own risk).
   */
  swipeAreaWidth: _react.PropTypes.number,
  /**
   * The width of the `Drawer` in pixels. Defaults to using the values from theme.
   */
  width: _react.PropTypes.number,
  /**
   * The zDepth of the `Drawer`.
   */
  zDepth: _propTypes2.default.zDepth

} : void 0;
exports.default = Drawer;
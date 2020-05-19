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

var _defineProperty2 = require('babel-runtime/helpers/defineProperty');

var _defineProperty3 = _interopRequireDefault(_defineProperty2);

var _simpleAssign = require('simple-assign');

var _simpleAssign2 = _interopRequireDefault(_simpleAssign);

var _react = require('react');

var _react2 = _interopRequireDefault(_react);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

function getStyles(props, context) {
  var _titleBar;

  var _context$muiTheme = context.muiTheme,
      baseTheme = _context$muiTheme.baseTheme,
      gridTile = _context$muiTheme.gridTile;


  var actionPos = props.actionIcon && props.actionPosition;

  var styles = {
    root: {
      position: 'relative',
      display: 'block',
      height: '100%',
      overflow: 'hidden'
    },
    titleBar: (_titleBar = {
      position: 'absolute',
      left: 0,
      right: 0
    }, (0, _defineProperty3.default)(_titleBar, props.titlePosition, 0), (0, _defineProperty3.default)(_titleBar, 'height', props.subtitle ? 68 : 48), (0, _defineProperty3.default)(_titleBar, 'background', props.titleBackground), (0, _defineProperty3.default)(_titleBar, 'display', 'flex'), (0, _defineProperty3.default)(_titleBar, 'alignItems', 'center'), _titleBar),
    titleWrap: {
      flexGrow: 1,
      marginLeft: actionPos !== 'left' ? baseTheme.spacing.desktopGutterLess : 0,
      marginRight: actionPos === 'left' ? baseTheme.spacing.desktopGutterLess : 0,
      color: gridTile.textColor,
      overflow: 'hidden'
    },
    title: {
      fontSize: '16px',
      textOverflow: 'ellipsis',
      overflow: 'hidden',
      whiteSpace: 'nowrap'
    },
    subtitle: {
      fontSize: '12px',
      textOverflow: 'ellipsis',
      overflow: 'hidden',
      whiteSpace: 'nowrap'
    },
    actionIcon: {
      order: actionPos === 'left' ? -1 : 1
    },
    childImg: {
      height: '100%',
      transform: 'translateX(-50%)',
      position: 'relative',
      left: '50%'
    }
  };
  return styles;
}

var GridTile = function (_Component) {
  (0, _inherits3.default)(GridTile, _Component);

  function GridTile() {
    (0, _classCallCheck3.default)(this, GridTile);
    return (0, _possibleConstructorReturn3.default)(this, (GridTile.__proto__ || (0, _getPrototypeOf2.default)(GridTile)).apply(this, arguments));
  }

  (0, _createClass3.default)(GridTile, [{
    key: 'componentDidMount',
    value: function componentDidMount() {
      this.ensureImageCover();
    }
  }, {
    key: 'componentDidUpdate',
    value: function componentDidUpdate() {
      this.ensureImageCover();
    }
  }, {
    key: 'ensureImageCover',
    value: function ensureImageCover() {
      var _this2 = this;

      var imgEl = this.refs.img;

      if (imgEl) {
        (function () {
          var fit = function fit() {
            if (imgEl.offsetWidth < imgEl.parentNode.offsetWidth) {
              var isRtl = _this2.context.muiTheme.isRtl;

              imgEl.style.height = 'auto';
              if (isRtl) {
                imgEl.style.right = '0';
              } else {
                imgEl.style.left = '0';
              }
              imgEl.style.width = '100%';
              imgEl.style.top = '50%';
              imgEl.style.transform = imgEl.style.WebkitTransform = 'translateY(-50%)';
            }
            imgEl.removeEventListener('load', fit);
            imgEl = null; // prevent closure memory leak
          };
          if (imgEl.complete) {
            fit();
          } else {
            imgEl.addEventListener('load', fit);
          }
        })();
      }
    }
  }, {
    key: 'render',
    value: function render() {
      var _props = this.props,
          title = _props.title,
          subtitle = _props.subtitle,
          titlePosition = _props.titlePosition,
          titleBackground = _props.titleBackground,
          titleStyle = _props.titleStyle,
          actionIcon = _props.actionIcon,
          actionPosition = _props.actionPosition,
          style = _props.style,
          children = _props.children,
          containerElement = _props.containerElement,
          other = (0, _objectWithoutProperties3.default)(_props, ['title', 'subtitle', 'titlePosition', 'titleBackground', 'titleStyle', 'actionIcon', 'actionPosition', 'style', 'children', 'containerElement']);
      var prepareStyles = this.context.muiTheme.prepareStyles;

      var styles = getStyles(this.props, this.context);
      var mergedRootStyles = (0, _simpleAssign2.default)(styles.root, style);

      var titleBar = null;

      if (title) {
        titleBar = _react2.default.createElement(
          'div',
          { key: 'titlebar', style: prepareStyles(styles.titleBar) },
          _react2.default.createElement(
            'div',
            { style: prepareStyles(styles.titleWrap) },
            _react2.default.createElement(
              'div',
              { style: prepareStyles((0, _simpleAssign2.default)(styles.title, titleStyle)) },
              title
            ),
            subtitle ? _react2.default.createElement(
              'div',
              { style: prepareStyles(styles.subtitle) },
              subtitle
            ) : null
          ),
          actionIcon ? _react2.default.createElement(
            'div',
            { style: prepareStyles(styles.actionIcon) },
            actionIcon
          ) : null
        );
      }

      var newChildren = children;

      // if there is a single image passed as children
      // clone it and add our styles
      if (_react2.default.Children.count(children) === 1) {
        newChildren = _react2.default.Children.map(children, function (child) {
          if (child.type === 'img') {
            return _react2.default.cloneElement(child, {
              key: 'img',
              ref: 'img',
              style: prepareStyles((0, _simpleAssign2.default)({}, styles.childImg, child.props.style))
            });
          } else {
            return child;
          }
        });
      }

      var containerProps = (0, _extends3.default)({
        style: prepareStyles(mergedRootStyles)
      }, other);

      return _react2.default.isValidElement(containerElement) ? _react2.default.cloneElement(containerElement, containerProps, [newChildren, titleBar]) : _react2.default.createElement(containerElement, containerProps, [newChildren, titleBar]);
    }
  }]);
  return GridTile;
}(_react.Component);

GridTile.defaultProps = {
  titlePosition: 'bottom',
  titleBackground: 'rgba(0, 0, 0, 0.4)',
  actionPosition: 'right',
  cols: 1,
  rows: 1,
  containerElement: 'div'
};
GridTile.contextTypes = {
  muiTheme: _react.PropTypes.object.isRequired
};
process.env.NODE_ENV !== "production" ? GridTile.propTypes = {
  /**
   * An IconButton element to be used as secondary action target
   * (primary action target is the tile itself).
   */
  actionIcon: _react.PropTypes.element,
  /**
   * Position of secondary action IconButton.
   */
  actionPosition: _react.PropTypes.oneOf(['left', 'right']),
  /**
   * Theoretically you can pass any node as children, but the main use case is to pass an img,
   * in whichcase GridTile takes care of making the image "cover" available space
   * (similar to background-size: cover or to object-fit:cover).
   */
  children: _react.PropTypes.node,
  /**
   * Width of the tile in number of grid cells.
   */
  cols: _react.PropTypes.number,
  /**
   * Either a string used as tag name for the tile root element, or a ReactElement.
   * This is useful when you have, for example, a custom implementation of
   * a navigation link (that knows about your routes) and you want to use it as the primary tile action.
   * In case you pass a ReactElement, please ensure that it passes all props,
   * accepts styles overrides and render it's children.
   */
  containerElement: _react.PropTypes.oneOfType([_react.PropTypes.string, _react.PropTypes.element]),
  /**
   * Height of the tile in number of grid cells.
   */
  rows: _react.PropTypes.number,
  /**
   * Override the inline-styles of the root element.
   */
  style: _react.PropTypes.object,
  /**
   * String or element serving as subtitle (support text).
   */
  subtitle: _react.PropTypes.node,
  /**
   * Title to be displayed on tile.
   */
  title: _react.PropTypes.node,
  /**
   * Style used for title bar background.
   * Useful for setting custom gradients for example
   */
  titleBackground: _react.PropTypes.string,
  /**
   * Position of the title bar (container of title, subtitle and action icon).
   */
  titlePosition: _react.PropTypes.oneOf(['top', 'bottom']),
  /**
   * Override the inline-styles of the title element.
   */
  titleStyle: _react.PropTypes.object
} : void 0;
exports.default = GridTile;
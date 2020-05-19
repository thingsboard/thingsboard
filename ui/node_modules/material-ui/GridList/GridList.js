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

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

function getStyles(props) {
  return {
    root: {
      display: 'flex',
      flexWrap: 'wrap',
      margin: -props.padding / 2
    },
    item: {
      boxSizing: 'border-box',
      padding: props.padding / 2
    }
  };
}

var GridList = function (_Component) {
  (0, _inherits3.default)(GridList, _Component);

  function GridList() {
    (0, _classCallCheck3.default)(this, GridList);
    return (0, _possibleConstructorReturn3.default)(this, (GridList.__proto__ || (0, _getPrototypeOf2.default)(GridList)).apply(this, arguments));
  }

  (0, _createClass3.default)(GridList, [{
    key: 'render',
    value: function render() {
      var _props = this.props,
          cols = _props.cols,
          padding = _props.padding,
          cellHeight = _props.cellHeight,
          children = _props.children,
          style = _props.style,
          other = (0, _objectWithoutProperties3.default)(_props, ['cols', 'padding', 'cellHeight', 'children', 'style']);
      var prepareStyles = this.context.muiTheme.prepareStyles;

      var styles = getStyles(this.props, this.context);
      var mergedRootStyles = (0, _simpleAssign2.default)(styles.root, style);

      var wrappedChildren = _react2.default.Children.map(children, function (currentChild) {
        if (_react2.default.isValidElement(currentChild) && currentChild.type.muiName === 'Subheader') {
          return currentChild;
        }
        var childCols = currentChild.props.cols || 1;
        var childRows = currentChild.props.rows || 1;
        var itemStyle = (0, _simpleAssign2.default)({}, styles.item, {
          width: 100 / cols * childCols + '%',
          height: cellHeight === 'auto' ? 'auto' : cellHeight * childRows + padding
        });

        return _react2.default.createElement(
          'div',
          { style: prepareStyles(itemStyle) },
          currentChild
        );
      });

      return _react2.default.createElement(
        'div',
        (0, _extends3.default)({ style: prepareStyles(mergedRootStyles) }, other),
        wrappedChildren
      );
    }
  }]);
  return GridList;
}(_react.Component);

GridList.defaultProps = {
  cols: 2,
  padding: 4,
  cellHeight: 180
};
GridList.contextTypes = {
  muiTheme: _react.PropTypes.object.isRequired
};
process.env.NODE_ENV !== "production" ? GridList.propTypes = {
  /**
   * Number of px for one cell height.
   * You can set `'auto'` if you want to let the children determine the height.
   */
  cellHeight: _react.PropTypes.oneOfType([_react.PropTypes.number, _react.PropTypes.oneOf(['auto'])]),
  /**
   * Grid Tiles that will be in Grid List.
   */
  children: _react.PropTypes.node,
  /**
   * Number of columns.
   */
  cols: _react.PropTypes.number,
  /**
   * Number of px for the padding/spacing between items.
   */
  padding: _react.PropTypes.number,
  /**
   * Override the inline-styles of the root element.
   */
  style: _react.PropTypes.object
} : void 0;
exports.default = GridList;
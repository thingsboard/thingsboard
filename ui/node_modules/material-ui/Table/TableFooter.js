'use strict';

Object.defineProperty(exports, "__esModule", {
  value: true
});

var _extends2 = require('babel-runtime/helpers/extends');

var _extends3 = _interopRequireDefault(_extends2);

var _toConsumableArray2 = require('babel-runtime/helpers/toConsumableArray');

var _toConsumableArray3 = _interopRequireDefault(_toConsumableArray2);

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

var _TableRowColumn = require('./TableRowColumn');

var _TableRowColumn2 = _interopRequireDefault(_TableRowColumn);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

function getStyles(props, context) {
  var tableFooter = context.muiTheme.tableFooter;


  return {
    cell: {
      borderTop: '1px solid ' + tableFooter.borderColor,
      verticalAlign: 'bottom',
      padding: 20,
      textAlign: 'left',
      whiteSpace: 'nowrap'
    }
  };
}

var TableFooter = function (_Component) {
  (0, _inherits3.default)(TableFooter, _Component);

  function TableFooter() {
    (0, _classCallCheck3.default)(this, TableFooter);
    return (0, _possibleConstructorReturn3.default)(this, (TableFooter.__proto__ || (0, _getPrototypeOf2.default)(TableFooter)).apply(this, arguments));
  }

  (0, _createClass3.default)(TableFooter, [{
    key: 'render',
    value: function render() {
      var _props = this.props,
          adjustForCheckbox = _props.adjustForCheckbox,
          children = _props.children,
          className = _props.className,
          style = _props.style,
          other = (0, _objectWithoutProperties3.default)(_props, ['adjustForCheckbox', 'children', 'className', 'style']);
      var prepareStyles = this.context.muiTheme.prepareStyles;

      var styles = getStyles(this.props, this.context);

      var footerRows = _react2.default.Children.map(children, function (child, rowNumber) {
        var newChildProps = {
          displayBorder: false,
          key: 'f-' + rowNumber,
          rowNumber: rowNumber,
          style: (0, _simpleAssign2.default)({}, styles.cell, child.props.style)
        };

        var newDescendants = void 0;

        if (adjustForCheckbox) {
          newDescendants = [_react2.default.createElement(_TableRowColumn2.default, { key: 'fpcb' + rowNumber, style: { width: 24 } })].concat((0, _toConsumableArray3.default)(_react2.default.Children.toArray(child.props.children)));
        } else {
          newDescendants = child.props.children;
        }

        return _react2.default.cloneElement(child, newChildProps, newDescendants);
      });

      return _react2.default.createElement(
        'tfoot',
        (0, _extends3.default)({ className: className, style: prepareStyles((0, _simpleAssign2.default)({}, style)) }, other),
        footerRows
      );
    }
  }]);
  return TableFooter;
}(_react.Component);

TableFooter.muiName = 'TableFooter';
TableFooter.defaultProps = {
  adjustForCheckbox: true,
  style: {}
};
TableFooter.contextTypes = {
  muiTheme: _react.PropTypes.object.isRequired
};
process.env.NODE_ENV !== "production" ? TableFooter.propTypes = {
  /**
   * @ignore
   * Controls whether or not header rows should be adjusted
   * for a checkbox column. If the select all checkbox is true,
   * this property will not influence the number of columns.
   * This is mainly useful for "super header" rows so that
   * the checkbox column does not create an offset that needs
   * to be accounted for manually.
   */
  adjustForCheckbox: _react.PropTypes.bool,
  /**
   * Children passed to table footer.
   */
  children: _react.PropTypes.node,
  /**
   * The css class name of the root element.
   */
  className: _react.PropTypes.string,
  /**
   * Override the inline-styles of the root element.
   */
  style: _react.PropTypes.object
} : void 0;
exports.default = TableFooter;
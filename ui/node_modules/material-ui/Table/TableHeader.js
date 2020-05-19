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

var _Checkbox = require('../Checkbox');

var _Checkbox2 = _interopRequireDefault(_Checkbox);

var _TableHeaderColumn = require('./TableHeaderColumn');

var _TableHeaderColumn2 = _interopRequireDefault(_TableHeaderColumn);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

function getStyles(props, context) {
  var tableHeader = context.muiTheme.tableHeader;


  return {
    root: {
      borderBottom: '1px solid ' + tableHeader.borderColor
    }
  };
}

var TableHeader = function (_Component) {
  (0, _inherits3.default)(TableHeader, _Component);

  function TableHeader() {
    var _ref;

    var _temp, _this, _ret;

    (0, _classCallCheck3.default)(this, TableHeader);

    for (var _len = arguments.length, args = Array(_len), _key = 0; _key < _len; _key++) {
      args[_key] = arguments[_key];
    }

    return _ret = (_temp = (_this = (0, _possibleConstructorReturn3.default)(this, (_ref = TableHeader.__proto__ || (0, _getPrototypeOf2.default)(TableHeader)).call.apply(_ref, [this].concat(args))), _this), _this.handleCheckAll = function (event, checked) {
      if (_this.props.onSelectAll) {
        _this.props.onSelectAll(checked);
      }
    }, _temp), (0, _possibleConstructorReturn3.default)(_this, _ret);
  }

  (0, _createClass3.default)(TableHeader, [{
    key: 'createSuperHeaderRows',
    value: function createSuperHeaderRows() {
      var numChildren = _react2.default.Children.count(this.props.children);
      if (numChildren === 1) return undefined;

      var superHeaders = [];
      for (var index = 0; index < numChildren - 1; index++) {
        var child = this.props.children[index];

        if (!_react2.default.isValidElement(child)) continue;

        var props = {
          key: 'sh' + index,
          rowNumber: index
        };
        superHeaders.push(this.createSuperHeaderRow(child, props));
      }

      if (superHeaders.length) return superHeaders;
    }
  }, {
    key: 'createSuperHeaderRow',
    value: function createSuperHeaderRow(child, props) {
      var children = [];
      if (this.props.adjustForCheckbox) {
        children.push(this.getCheckboxPlaceholder(props));
      }
      _react2.default.Children.forEach(child.props.children, function (child) {
        children.push(child);
      });

      return _react2.default.cloneElement(child, props, children);
    }
  }, {
    key: 'createBaseHeaderRow',
    value: function createBaseHeaderRow() {
      var numChildren = _react2.default.Children.count(this.props.children);
      var child = numChildren === 1 ? this.props.children : this.props.children[numChildren - 1];
      var props = {
        key: 'h' + numChildren,
        rowNumber: numChildren
      };

      var children = [this.getSelectAllCheckboxColumn(props)];
      _react2.default.Children.forEach(child.props.children, function (child) {
        children.push(child);
      });

      return _react2.default.cloneElement(child, props, children);
    }
  }, {
    key: 'getCheckboxPlaceholder',
    value: function getCheckboxPlaceholder(props) {
      if (!this.props.adjustForCheckbox) return null;

      var disabled = !this.props.enableSelectAll;
      var key = 'hpcb' + props.rowNumber;
      return _react2.default.createElement(_TableHeaderColumn2.default, {
        key: key,
        style: {
          width: 24,
          cursor: disabled ? 'not-allowed' : 'inherit'
        }
      });
    }
  }, {
    key: 'getSelectAllCheckboxColumn',
    value: function getSelectAllCheckboxColumn(props) {
      if (!this.props.displaySelectAll) return this.getCheckboxPlaceholder(props);

      var disabled = !this.props.enableSelectAll;
      var checkbox = _react2.default.createElement(_Checkbox2.default, {
        key: 'selectallcb',
        name: 'selectallcb',
        value: 'selected',
        disabled: disabled,
        checked: this.props.selectAllSelected,
        onCheck: this.handleCheckAll
      });

      var key = 'hpcb' + props.rowNumber;
      return _react2.default.createElement(
        _TableHeaderColumn2.default,
        {
          key: key,
          style: {
            width: 24,
            cursor: disabled ? 'not-allowed' : 'inherit'
          }
        },
        checkbox
      );
    }
  }, {
    key: 'render',
    value: function render() {
      var _props = this.props,
          className = _props.className,
          style = _props.style;
      var prepareStyles = this.context.muiTheme.prepareStyles;

      var styles = getStyles(this.props, this.context);
      var superHeaderRows = this.createSuperHeaderRows();
      var baseHeaderRow = this.createBaseHeaderRow();

      return _react2.default.createElement(
        'thead',
        { className: className, style: prepareStyles((0, _simpleAssign2.default)(styles.root, style)) },
        superHeaderRows,
        baseHeaderRow
      );
    }
  }]);
  return TableHeader;
}(_react.Component);

TableHeader.muiName = 'TableHeader';
TableHeader.defaultProps = {
  adjustForCheckbox: true,
  displaySelectAll: true,
  enableSelectAll: true,
  selectAllSelected: false
};
TableHeader.contextTypes = {
  muiTheme: _react.PropTypes.object.isRequired
};
process.env.NODE_ENV !== "production" ? TableHeader.propTypes = {
  /**
   * Controls whether or not header rows should be
   * adjusted for a checkbox column. If the select all
   * checkbox is true, this property will not influence
   * the number of columns. This is mainly useful for
   * "super header" rows so that the checkbox column
   * does not create an offset that needs to be accounted
   * for manually.
   */
  adjustForCheckbox: _react.PropTypes.bool,
  /**
   * Children passed to table header.
   */
  children: _react.PropTypes.node,
  /**
   * The css class name of the root element.
   */
  className: _react.PropTypes.string,
  /**
   * Controls whether or not the select all checkbox is displayed.
   */
  displaySelectAll: _react.PropTypes.bool,
  /**
   * If set to true, the select all button will be interactable.
   * If set to false, the button will not be interactable.
   * To hide the checkbox, set displaySelectAll to false.
   */
  enableSelectAll: _react.PropTypes.bool,
  /**
   * @ignore
   * Callback when select all has been checked.
   */
  onSelectAll: _react.PropTypes.func,
  /**
   * @ignore
   * True when select all has been checked.
   */
  selectAllSelected: _react.PropTypes.bool,
  /**
   * Override the inline-styles of the root element.
   */
  style: _react.PropTypes.object
} : void 0;
exports.default = TableHeader;
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

var _TextField = require('../TextField');

var _TextField2 = _interopRequireDefault(_TextField);

var _DropDownMenu = require('../DropDownMenu');

var _DropDownMenu2 = _interopRequireDefault(_DropDownMenu);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

function getStyles(props) {
  return {
    label: {
      paddingLeft: 0,
      top: props.floatingLabelText ? 6 : -4
    },
    icon: {
      right: 0,
      top: props.floatingLabelText ? 8 : 0
    },
    hideDropDownUnderline: {
      borderTop: 'none'
    },
    dropDownMenu: {
      display: 'block'
    }
  };
}

var SelectField = function (_Component) {
  (0, _inherits3.default)(SelectField, _Component);

  function SelectField() {
    (0, _classCallCheck3.default)(this, SelectField);
    return (0, _possibleConstructorReturn3.default)(this, (SelectField.__proto__ || (0, _getPrototypeOf2.default)(SelectField)).apply(this, arguments));
  }

  (0, _createClass3.default)(SelectField, [{
    key: 'render',
    value: function render() {
      var _props = this.props,
          autoWidth = _props.autoWidth,
          children = _props.children,
          style = _props.style,
          labelStyle = _props.labelStyle,
          iconStyle = _props.iconStyle,
          id = _props.id,
          underlineDisabledStyle = _props.underlineDisabledStyle,
          underlineFocusStyle = _props.underlineFocusStyle,
          menuItemStyle = _props.menuItemStyle,
          selectedMenuItemStyle = _props.selectedMenuItemStyle,
          underlineStyle = _props.underlineStyle,
          errorStyle = _props.errorStyle,
          disabled = _props.disabled,
          floatingLabelFixed = _props.floatingLabelFixed,
          floatingLabelText = _props.floatingLabelText,
          floatingLabelStyle = _props.floatingLabelStyle,
          hintStyle = _props.hintStyle,
          hintText = _props.hintText,
          fullWidth = _props.fullWidth,
          errorText = _props.errorText,
          listStyle = _props.listStyle,
          maxHeight = _props.maxHeight,
          menuStyle = _props.menuStyle,
          onFocus = _props.onFocus,
          onBlur = _props.onBlur,
          onChange = _props.onChange,
          value = _props.value,
          other = (0, _objectWithoutProperties3.default)(_props, ['autoWidth', 'children', 'style', 'labelStyle', 'iconStyle', 'id', 'underlineDisabledStyle', 'underlineFocusStyle', 'menuItemStyle', 'selectedMenuItemStyle', 'underlineStyle', 'errorStyle', 'disabled', 'floatingLabelFixed', 'floatingLabelText', 'floatingLabelStyle', 'hintStyle', 'hintText', 'fullWidth', 'errorText', 'listStyle', 'maxHeight', 'menuStyle', 'onFocus', 'onBlur', 'onChange', 'value']);


      var styles = getStyles(this.props, this.context);

      return _react2.default.createElement(
        _TextField2.default,
        (0, _extends3.default)({}, other, {
          style: style,
          disabled: disabled,
          floatingLabelFixed: floatingLabelFixed,
          floatingLabelText: floatingLabelText,
          floatingLabelStyle: floatingLabelStyle,
          hintStyle: hintStyle,
          hintText: !hintText && !floatingLabelText ? ' ' : hintText,
          fullWidth: fullWidth,
          errorText: errorText,
          underlineStyle: underlineStyle,
          errorStyle: errorStyle,
          onFocus: onFocus,
          onBlur: onBlur,
          id: id,
          underlineDisabledStyle: underlineDisabledStyle,
          underlineFocusStyle: underlineFocusStyle
        }),
        _react2.default.createElement(
          _DropDownMenu2.default,
          {
            disabled: disabled,
            style: (0, _simpleAssign2.default)(styles.dropDownMenu, menuStyle),
            labelStyle: (0, _simpleAssign2.default)(styles.label, labelStyle),
            iconStyle: (0, _simpleAssign2.default)(styles.icon, iconStyle),
            menuItemStyle: menuItemStyle,
            selectedMenuItemStyle: selectedMenuItemStyle,
            underlineStyle: styles.hideDropDownUnderline,
            listStyle: listStyle,
            autoWidth: autoWidth,
            value: value,
            onChange: onChange,
            maxHeight: maxHeight
          },
          children
        )
      );
    }
  }]);
  return SelectField;
}(_react.Component);

SelectField.defaultProps = {
  autoWidth: false,
  disabled: false,
  fullWidth: false
};
SelectField.contextTypes = {
  muiTheme: _react.PropTypes.object.isRequired
};
process.env.NODE_ENV !== "production" ? SelectField.propTypes = {
  /**
   * If true, the width will automatically be set according to the
   * items inside the menu.
   * To control the width in CSS instead, leave this prop set to `false`.
   */
  autoWidth: _react.PropTypes.bool,
  /**
   * The `MenuItem` elements to populate the select field with.
   * If the menu items have a `label` prop, that value will
   * represent the selected menu item in the rendered select field.
   */
  children: _react.PropTypes.node,
  /**
   * If true, the select field will be disabled.
   */
  disabled: _react.PropTypes.bool,
  /**
   * Override the inline-styles of the error element.
   */
  errorStyle: _react.PropTypes.object,
  /**
   * The error content to display.
   */
  errorText: _react.PropTypes.node,
  /**
   * If true, the floating label will float even when no value is selected.
   */
  floatingLabelFixed: _react.PropTypes.bool,
  /**
   * Override the inline-styles of the floating label.
   */
  floatingLabelStyle: _react.PropTypes.object,
  /**
   * The content of the floating label.
   */
  floatingLabelText: _react.PropTypes.node,
  /**
   * If true, the select field will take up the full width of its container.
   */
  fullWidth: _react.PropTypes.bool,
  /**
   * Override the inline-styles of the hint element.
   */
  hintStyle: _react.PropTypes.object,
  /**
   * The hint content to display.
   */
  hintText: _react.PropTypes.node,
  /**
   * Override the inline-styles of the icon element.
   */
  iconStyle: _react.PropTypes.object,
  /**
   * The id prop for the text field.
   */
  id: _react.PropTypes.string,
  /**
   * Override the label style when the select field is inactive.
   */
  labelStyle: _react.PropTypes.object,
  /**
   * Override the inline-styles of the underlying `List` element.
   */
  listStyle: _react.PropTypes.object,
  /**
   * Override the default max-height of the underlying `DropDownMenu` element.
   */
  maxHeight: _react.PropTypes.number,
  /**
   * Override the inline-styles of menu items.
   */
  menuItemStyle: _react.PropTypes.object,
  /**
   * Override the inline-styles of the underlying `DropDownMenu` element.
   */
  menuStyle: _react.PropTypes.object,
  /** @ignore */
  onBlur: _react.PropTypes.func,
  /**
   * Callback function fired when a menu item is selected.
   *
   * @param {object} event TouchTap event targeting the menu item
   * that was selected.
   * @param {number} key The index of the selected menu item.
   * @param {any} payload The `value` prop of the selected menu item.
   */
  onChange: _react.PropTypes.func,
  /** @ignore */
  onFocus: _react.PropTypes.func,
  /**
   * Override the inline-styles of selected menu items.
   */
  selectedMenuItemStyle: _react.PropTypes.object,
  /**
   * Override the inline-styles of the root element.
   */
  style: _react.PropTypes.object,
  /**
   * Override the inline-styles of the underline element when the select
   * field is disabled.
   */
  underlineDisabledStyle: _react.PropTypes.object,
  /**
   * Override the inline-styles of the underline element when the select field
   * is focused.
   */
  underlineFocusStyle: _react.PropTypes.object,
  /**
   * Override the inline-styles of the underline element.
   */
  underlineStyle: _react.PropTypes.object,
  /**
   * The value that is currently selected.
   */
  value: _react.PropTypes.any
} : void 0;
exports.default = SelectField;
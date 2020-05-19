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

var _RadioButton = require('../RadioButton');

var _RadioButton2 = _interopRequireDefault(_RadioButton);

var _warning = require('warning');

var _warning2 = _interopRequireDefault(_warning);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

var RadioButtonGroup = function (_Component) {
  (0, _inherits3.default)(RadioButtonGroup, _Component);

  function RadioButtonGroup() {
    var _ref;

    var _temp, _this, _ret;

    (0, _classCallCheck3.default)(this, RadioButtonGroup);

    for (var _len = arguments.length, args = Array(_len), _key = 0; _key < _len; _key++) {
      args[_key] = arguments[_key];
    }

    return _ret = (_temp = (_this = (0, _possibleConstructorReturn3.default)(this, (_ref = RadioButtonGroup.__proto__ || (0, _getPrototypeOf2.default)(RadioButtonGroup)).call.apply(_ref, [this].concat(args))), _this), _this.state = {
      numberCheckedRadioButtons: 0,
      selected: ''
    }, _this.handleChange = function (event, newSelection) {
      _this.updateRadioButtons(newSelection);

      // Successful update
      if (_this.state.numberCheckedRadioButtons === 0) {
        if (_this.props.onChange) _this.props.onChange(event, newSelection);
      }
    }, _temp), (0, _possibleConstructorReturn3.default)(_this, _ret);
  }

  (0, _createClass3.default)(RadioButtonGroup, [{
    key: 'componentWillMount',
    value: function componentWillMount() {
      var _this2 = this;

      var cnt = 0;
      var selected = '';
      var _props = this.props,
          valueSelected = _props.valueSelected,
          defaultSelected = _props.defaultSelected;

      if (valueSelected !== undefined) {
        selected = valueSelected;
      } else if (defaultSelected !== undefined) {
        selected = defaultSelected;
      }

      _react2.default.Children.forEach(this.props.children, function (option) {
        if (_this2.hasCheckAttribute(option)) cnt++;
      }, this);

      this.setState({
        numberCheckedRadioButtons: cnt,
        selected: selected
      });
    }
  }, {
    key: 'componentWillReceiveProps',
    value: function componentWillReceiveProps(nextProps) {
      if (nextProps.hasOwnProperty('valueSelected')) {
        this.setState({
          selected: nextProps.valueSelected
        });
      }
    }
  }, {
    key: 'hasCheckAttribute',
    value: function hasCheckAttribute(radioButton) {
      return radioButton.props.hasOwnProperty('checked') && radioButton.props.checked;
    }
  }, {
    key: 'updateRadioButtons',
    value: function updateRadioButtons(newSelection) {
      if (this.state.numberCheckedRadioButtons === 0) {
        this.setState({ selected: newSelection });
      } else {
        process.env.NODE_ENV !== "production" ? (0, _warning2.default)(false, 'Material-UI: Cannot select a different radio button while another radio button\n        has the \'checked\' property set to true.') : void 0;
      }
    }
  }, {
    key: 'getSelectedValue',
    value: function getSelectedValue() {
      return this.state.selected;
    }
  }, {
    key: 'setSelectedValue',
    value: function setSelectedValue(newSelectionValue) {
      this.updateRadioButtons(newSelectionValue);
    }
  }, {
    key: 'clearValue',
    value: function clearValue() {
      this.setSelectedValue('');
    }
  }, {
    key: 'render',
    value: function render() {
      var _this3 = this;

      var prepareStyles = this.context.muiTheme.prepareStyles;


      var options = _react2.default.Children.map(this.props.children, function (option) {
        var _option$props = option.props,
            name = _option$props.name,
            value = _option$props.value,
            label = _option$props.label,
            onCheck = _option$props.onCheck,
            other = (0, _objectWithoutProperties3.default)(_option$props, ['name', 'value', 'label', 'onCheck']);


        return _react2.default.createElement(_RadioButton2.default, (0, _extends3.default)({}, other, {
          ref: option.props.value,
          name: _this3.props.name,
          key: option.props.value,
          value: option.props.value,
          label: option.props.label,
          labelPosition: _this3.props.labelPosition,
          onCheck: _this3.handleChange,
          checked: option.props.value === _this3.state.selected
        }));
      }, this);

      return _react2.default.createElement(
        'div',
        {
          style: prepareStyles((0, _simpleAssign2.default)({}, this.props.style)),
          className: this.props.className
        },
        options
      );
    }
  }]);
  return RadioButtonGroup;
}(_react.Component);

RadioButtonGroup.defaultProps = {
  style: {}
};
RadioButtonGroup.contextTypes = {
  muiTheme: _react.PropTypes.object.isRequired
};
process.env.NODE_ENV !== "production" ? RadioButtonGroup.propTypes = {
  /**
   * Should be used to pass `RadioButton` components.
   */
  children: _react.PropTypes.node,
  /**
   * The CSS class name of the root element.
   */
  className: _react.PropTypes.string,
  /**
   * The `value` property of the radio button that will be
   * selected by default. This takes precedence over the `checked` property
   * of the `RadioButton` elements.
   */
  defaultSelected: _react.PropTypes.any,
  /**
   * Where the label will be placed for all child radio buttons.
   * This takes precedence over the `labelPosition` property of the
   * `RadioButton` elements.
   */
  labelPosition: _react.PropTypes.oneOf(['left', 'right']),
  /**
   * The name that will be applied to all child radio buttons.
   */
  name: _react.PropTypes.string.isRequired,
  /**
   * Callback function that is fired when a radio button has
   * been checked.
   *
   * @param {object} event `change` event targeting the selected
   * radio button.
   * @param {*} value The `value` of the selected radio button.
   */
  onChange: _react.PropTypes.func,
  /**
   * Override the inline-styles of the root element.
   */
  style: _react.PropTypes.object,
  /**
   * The `value` of the currently selected radio button.
   */
  valueSelected: _react.PropTypes.any
} : void 0;
exports.default = RadioButtonGroup;
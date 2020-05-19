'use strict';

Object.defineProperty(exports, "__esModule", {
    value: true
});

var _createClass = function () { function defineProperties(target, props) { for (var i = 0; i < props.length; i++) { var descriptor = props[i]; descriptor.enumerable = descriptor.enumerable || false; descriptor.configurable = true; if ("value" in descriptor) descriptor.writable = true; Object.defineProperty(target, descriptor.key, descriptor); } } return function (Constructor, protoProps, staticProps) { if (protoProps) defineProperties(Constructor.prototype, protoProps); if (staticProps) defineProperties(Constructor, staticProps); return Constructor; }; }();

var _react = require('react');

var _react2 = _interopRequireDefault(_react);

var _ComposedComponent = require('./ComposedComponent');

var _ComposedComponent2 = _interopRequireDefault(_ComposedComponent);

var _TextField = require('material-ui/TextField');

var _TextField2 = _interopRequireDefault(_TextField);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

function _classCallCheck(instance, Constructor) { if (!(instance instanceof Constructor)) { throw new TypeError("Cannot call a class as a function"); } }

function _possibleConstructorReturn(self, call) { if (!self) { throw new ReferenceError("this hasn't been initialised - super() hasn't been called"); } return call && (typeof call === "object" || typeof call === "function") ? call : self; }

function _inherits(subClass, superClass) { if (typeof superClass !== "function" && superClass !== null) { throw new TypeError("Super expression must either be null or a function, not " + typeof superClass); } subClass.prototype = Object.create(superClass && superClass.prototype, { constructor: { value: subClass, enumerable: false, writable: true, configurable: true } }); if (superClass) Object.setPrototypeOf ? Object.setPrototypeOf(subClass, superClass) : subClass.__proto__ = superClass; } /**
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                * Created by steve on 15/09/15.
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                */


/**
 * There is no default number picker as part of Material-UI.
 * Instead, use a TextField and validate.
 */
var Number = function (_React$Component) {
    _inherits(Number, _React$Component);

    function Number(props) {
        _classCallCheck(this, Number);

        var _this = _possibleConstructorReturn(this, (Number.__proto__ || Object.getPrototypeOf(Number)).call(this, props));

        _this.preValidationCheck = _this.preValidationCheck.bind(_this);
        _this.state = {
            lastSuccessfulValue: _this.props.value
        };
        return _this;
    }

    _createClass(Number, [{
        key: 'componentWillReceiveProps',
        value: function componentWillReceiveProps(nextProps) {
            this.setState({
                lastSuccessfulValue: nextProps.value
            });
        }
    }, {
        key: 'isNumeric',
        value: function isNumeric(n) {
            return !isNaN(parseFloat(n)) && isFinite(n);
        }

        /**
         * Prevent the field from accepting non-numeric characters.
         * @param e
         */

    }, {
        key: 'preValidationCheck',
        value: function preValidationCheck(e) {
            if (this.isNumeric(e.target.value)) {
                this.setState({
                    lastSuccessfulValue: e.target.value
                });
                this.props.onChangeValidate(e);
            } else {
                this.refs.numberField.value = this.state.lastSuccessfulValue;
            }
        }
    }, {
        key: 'render',
        value: function render() {
            return _react2.default.createElement(
                'div',
                { className: this.props.form.htmlClass },
                _react2.default.createElement(_TextField2.default, {
                    type: this.props.form.type,
                    floatingLabelText: this.props.form.title,
                    hintText: this.props.form.placeholder,
                    errorText: this.props.error,
                    onChange: this.preValidationCheck,
                    value: this.state.lastSuccessfulValue,
                    ref: 'numberField',
                    disabled: this.props.form.readonly,
                    style: this.props.form.style || { width: '100%' } })
            );
        }
    }]);

    return Number;
}(_react2.default.Component);

var _default = (0, _ComposedComponent2.default)(Number);

exports.default = _default;
;

var _temp = function () {
    if (typeof __REACT_HOT_LOADER__ === 'undefined') {
        return;
    }

    __REACT_HOT_LOADER__.register(Number, 'Number', 'src/Number.js');

    __REACT_HOT_LOADER__.register(_default, 'default', 'src/Number.js');
}();

;
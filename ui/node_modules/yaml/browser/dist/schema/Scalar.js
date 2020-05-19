"use strict";

var _interopRequireDefault = require("@babel/runtime/helpers/interopRequireDefault");

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.default = void 0;

var _classCallCheck2 = _interopRequireDefault(require("@babel/runtime/helpers/classCallCheck"));

var _createClass2 = _interopRequireDefault(require("@babel/runtime/helpers/createClass"));

var _possibleConstructorReturn2 = _interopRequireDefault(require("@babel/runtime/helpers/possibleConstructorReturn"));

var _getPrototypeOf2 = _interopRequireDefault(require("@babel/runtime/helpers/getPrototypeOf"));

var _inherits2 = _interopRequireDefault(require("@babel/runtime/helpers/inherits"));

var _toJSON2 = _interopRequireDefault(require("../toJSON"));

var _Node2 = _interopRequireDefault(require("./Node"));

// Published as 'yaml/scalar'
var Scalar =
/*#__PURE__*/
function (_Node) {
  (0, _inherits2.default)(Scalar, _Node);

  function Scalar(value) {
    var _this;

    (0, _classCallCheck2.default)(this, Scalar);
    _this = (0, _possibleConstructorReturn2.default)(this, (0, _getPrototypeOf2.default)(Scalar).call(this));
    _this.value = value;
    return _this;
  }

  (0, _createClass2.default)(Scalar, [{
    key: "toJSON",
    value: function toJSON(arg, ctx) {
      return ctx && ctx.keep ? this.value : (0, _toJSON2.default)(this.value, arg, ctx);
    }
  }, {
    key: "toString",
    value: function toString() {
      return String(this.value);
    }
  }]);
  return Scalar;
}(_Node2.default);

exports.default = Scalar;
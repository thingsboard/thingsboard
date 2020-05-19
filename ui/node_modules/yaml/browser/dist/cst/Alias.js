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

var _Node2 = _interopRequireDefault(require("./Node"));

var _Range = _interopRequireDefault(require("./Range"));

var Alias =
/*#__PURE__*/
function (_Node) {
  (0, _inherits2.default)(Alias, _Node);

  function Alias() {
    (0, _classCallCheck2.default)(this, Alias);
    return (0, _possibleConstructorReturn2.default)(this, (0, _getPrototypeOf2.default)(Alias).apply(this, arguments));
  }

  (0, _createClass2.default)(Alias, [{
    key: "parse",

    /**
     * Parses an *alias from the source
     *
     * @param {ParseContext} context
     * @param {number} start - Index of first character
     * @returns {number} - Index of the character after this scalar
     */
    value: function parse(context, start) {
      this.context = context;
      var src = context.src;

      var offset = _Node2.default.endOfIdentifier(src, start + 1);

      this.valueRange = new _Range.default(start + 1, offset);
      offset = _Node2.default.endOfWhiteSpace(src, offset);
      offset = this.parseComment(offset);
      return offset;
    }
  }]);
  return Alias;
}(_Node2.default);

exports.default = Alias;
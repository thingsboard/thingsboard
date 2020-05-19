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

var _constants = require("../constants");

var _Node2 = _interopRequireDefault(require("./Node"));

var _Range = _interopRequireDefault(require("./Range"));

var Comment =
/*#__PURE__*/
function (_Node) {
  (0, _inherits2.default)(Comment, _Node);

  function Comment() {
    (0, _classCallCheck2.default)(this, Comment);
    return (0, _possibleConstructorReturn2.default)(this, (0, _getPrototypeOf2.default)(Comment).call(this, _constants.Type.COMMENT));
  }
  /**
   * Parses a comment line from the source
   *
   * @param {ParseContext} context
   * @param {number} start - Index of first character
   * @returns {number} - Index of the character after this scalar
   */


  (0, _createClass2.default)(Comment, [{
    key: "parse",
    value: function parse(context, start) {
      this.context = context;
      var offset = this.parseComment(start);
      this.range = new _Range.default(start, offset);
      return offset;
    }
  }]);
  return Comment;
}(_Node2.default);

exports.default = Comment;
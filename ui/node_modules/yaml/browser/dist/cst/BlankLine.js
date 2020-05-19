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

var BlankLine =
/*#__PURE__*/
function (_Node) {
  (0, _inherits2.default)(BlankLine, _Node);

  function BlankLine() {
    (0, _classCallCheck2.default)(this, BlankLine);
    return (0, _possibleConstructorReturn2.default)(this, (0, _getPrototypeOf2.default)(BlankLine).call(this, _constants.Type.BLANK_LINE));
  }

  (0, _createClass2.default)(BlankLine, [{
    key: "parse",

    /**
     * Parses blank lines from the source
     *
     * @param {ParseContext} context
     * @param {number} start - Index of first \n character
     * @returns {number} - Index of the character after this
     */
    value: function parse(context, start) {
      this.context = context;
      var src = context.src;
      var offset = start + 1;

      while (_Node2.default.atBlank(src, offset)) {
        var lineEnd = _Node2.default.endOfWhiteSpace(src, offset);

        if (lineEnd === '\n') offset = lineEnd + 1;else break;
      }

      this.range = new _Range.default(start, offset);
      return offset;
    }
  }, {
    key: "includesTrailingLines",
    get: function get() {
      return true;
    }
  }]);
  return BlankLine;
}(_Node2.default);

exports.default = BlankLine;
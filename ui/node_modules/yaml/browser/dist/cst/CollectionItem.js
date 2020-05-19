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

var _get2 = _interopRequireDefault(require("@babel/runtime/helpers/get"));

var _inherits2 = _interopRequireDefault(require("@babel/runtime/helpers/inherits"));

var _constants = require("../constants");

var _errors = require("../errors");

var _BlankLine = _interopRequireDefault(require("./BlankLine"));

var _Node2 = _interopRequireDefault(require("./Node"));

var _Range = _interopRequireDefault(require("./Range"));

var CollectionItem =
/*#__PURE__*/
function (_Node) {
  (0, _inherits2.default)(CollectionItem, _Node);

  function CollectionItem(type, props) {
    var _this;

    (0, _classCallCheck2.default)(this, CollectionItem);
    _this = (0, _possibleConstructorReturn2.default)(this, (0, _getPrototypeOf2.default)(CollectionItem).call(this, type, props));
    _this.node = null;
    return _this;
  }

  (0, _createClass2.default)(CollectionItem, [{
    key: "parse",

    /**
     * @param {ParseContext} context
     * @param {number} start - Index of first character
     * @returns {number} - Index of the character after this
     */
    value: function parse(context, start) {
      this.context = context;
      var parseNode = context.parseNode,
          src = context.src;
      var atLineStart = context.atLineStart,
          lineStart = context.lineStart;
      if (!atLineStart && this.type === _constants.Type.SEQ_ITEM) this.error = new _errors.YAMLSemanticError(this, 'Sequence items must not have preceding content on the same line');
      var indent = atLineStart ? start - lineStart : context.indent;

      var offset = _Node2.default.endOfWhiteSpace(src, start + 1);

      var ch = src[offset];
      var inlineComment = ch === '#';
      var comments = [];
      var blankLine = null;

      while (ch === '\n' || ch === '#') {
        if (ch === '#') {
          var _end = _Node2.default.endOfLine(src, offset + 1);

          comments.push(new _Range.default(offset, _end));
          offset = _end;
        } else {
          atLineStart = true;
          lineStart = offset + 1;

          var wsEnd = _Node2.default.endOfWhiteSpace(src, lineStart);

          if (src[wsEnd] === '\n' && comments.length === 0) {
            blankLine = new _BlankLine.default();
            lineStart = blankLine.parse({
              src: src
            }, lineStart);
          }

          offset = _Node2.default.endOfIndent(src, lineStart);
        }

        ch = src[offset];
      }

      if (_Node2.default.nextNodeIsIndented(ch, offset - (lineStart + indent), this.type !== _constants.Type.SEQ_ITEM)) {
        this.node = parseNode({
          atLineStart: atLineStart,
          inCollection: false,
          indent: indent,
          lineStart: lineStart,
          parent: this
        }, offset);
      } else if (ch && lineStart > start + 1) {
        offset = lineStart - 1;
      }

      if (this.node) {
        if (blankLine) {
          // Only blank lines preceding non-empty nodes are captured. Note that
          // this means that collection item range start indices do not always
          // increase monotonically. -- eemeli/yaml#126
          var items = context.parent.items || context.parent.contents;
          if (items) items.push(blankLine);
        }

        if (comments.length) Array.prototype.push.apply(this.props, comments);
        offset = this.node.range.end;
      } else {
        if (inlineComment) {
          var c = comments[0];
          this.props.push(c);
          offset = c.end;
        } else {
          offset = _Node2.default.endOfLine(src, start + 1);
        }
      }

      var end = this.node ? this.node.valueRange.end : offset;
      this.valueRange = new _Range.default(start, end);
      return offset;
    }
  }, {
    key: "setOrigRanges",
    value: function setOrigRanges(cr, offset) {
      offset = (0, _get2.default)((0, _getPrototypeOf2.default)(CollectionItem.prototype), "setOrigRanges", this).call(this, cr, offset);
      return this.node ? this.node.setOrigRanges(cr, offset) : offset;
    }
  }, {
    key: "toString",
    value: function toString() {
      var src = this.context.src,
          node = this.node,
          range = this.range,
          value = this.value;
      if (value != null) return value;
      var str = node ? src.slice(range.start, node.range.start) + String(node) : src.slice(range.start, range.end);
      return _Node2.default.addStringTerminator(src, range.end, str);
    }
  }, {
    key: "includesTrailingLines",
    get: function get() {
      return !!this.node && this.node.includesTrailingLines;
    }
  }]);
  return CollectionItem;
}(_Node2.default);

exports.default = CollectionItem;
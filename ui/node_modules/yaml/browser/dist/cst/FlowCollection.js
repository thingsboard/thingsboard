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

var _Comment = _interopRequireDefault(require("./Comment"));

var _Node2 = _interopRequireDefault(require("./Node"));

var _Range = _interopRequireDefault(require("./Range"));

var FlowCollection =
/*#__PURE__*/
function (_Node) {
  (0, _inherits2.default)(FlowCollection, _Node);

  function FlowCollection(type, props) {
    var _this;

    (0, _classCallCheck2.default)(this, FlowCollection);
    _this = (0, _possibleConstructorReturn2.default)(this, (0, _getPrototypeOf2.default)(FlowCollection).call(this, type, props));
    _this.items = null;
    return _this;
  }

  (0, _createClass2.default)(FlowCollection, [{
    key: "prevNodeIsJsonLike",
    value: function prevNodeIsJsonLike() {
      var idx = arguments.length > 0 && arguments[0] !== undefined ? arguments[0] : this.items.length;
      var node = this.items[idx - 1];
      return !!node && (node.jsonLike || node.type === _constants.Type.COMMENT && this.nodeIsJsonLike(idx - 1));
    }
    /**
     * @param {ParseContext} context
     * @param {number} start - Index of first character
     * @returns {number} - Index of the character after this
     */

  }, {
    key: "parse",
    value: function parse(context, start) {
      this.context = context;
      var parseNode = context.parseNode,
          src = context.src;
      var indent = context.indent,
          lineStart = context.lineStart;
      var char = src[start]; // { or [

      this.items = [{
        char: char,
        offset: start
      }];

      var offset = _Node2.default.endOfWhiteSpace(src, start + 1);

      char = src[offset];

      while (char && char !== ']' && char !== '}') {
        switch (char) {
          case '\n':
            {
              lineStart = offset + 1;

              var wsEnd = _Node2.default.endOfWhiteSpace(src, lineStart);

              if (src[wsEnd] === '\n') {
                var blankLine = new _BlankLine.default();
                lineStart = blankLine.parse({
                  src: src
                }, lineStart);
                this.items.push(blankLine);
              }

              offset = _Node2.default.endOfIndent(src, lineStart);

              if (offset <= lineStart + indent) {
                char = src[offset];

                if (offset < lineStart + indent || char !== ']' && char !== '}') {
                  var msg = 'Insufficient indentation in flow collection';
                  this.error = new _errors.YAMLSemanticError(this, msg);
                }
              }
            }
            break;

          case ',':
            {
              this.items.push({
                char: char,
                offset: offset
              });
              offset += 1;
            }
            break;

          case '#':
            {
              var comment = new _Comment.default();
              offset = comment.parse({
                src: src
              }, offset);
              this.items.push(comment);
            }
            break;

          case '?':
          case ':':
            {
              var next = src[offset + 1];

              if (next === '\n' || next === '\t' || next === ' ' || next === ',' || // in-flow : after JSON-like key does not need to be followed by whitespace
              char === ':' && this.prevNodeIsJsonLike()) {
                this.items.push({
                  char: char,
                  offset: offset
                });
                offset += 1;
                break;
              }
            }
          // fallthrough

          default:
            {
              var node = parseNode({
                atLineStart: false,
                inCollection: false,
                inFlow: true,
                indent: -1,
                lineStart: lineStart,
                parent: this
              }, offset);

              if (!node) {
                // at next document start
                this.valueRange = new _Range.default(start, offset);
                return offset;
              }

              this.items.push(node);
              offset = _Node2.default.normalizeOffset(src, node.range.end);
            }
        }

        offset = _Node2.default.endOfWhiteSpace(src, offset);
        char = src[offset];
      }

      this.valueRange = new _Range.default(start, offset + 1);

      if (char) {
        this.items.push({
          char: char,
          offset: offset
        });
        offset = _Node2.default.endOfWhiteSpace(src, offset + 1);
        offset = this.parseComment(offset);
      }

      return offset;
    }
  }, {
    key: "setOrigRanges",
    value: function setOrigRanges(cr, offset) {
      offset = (0, _get2.default)((0, _getPrototypeOf2.default)(FlowCollection.prototype), "setOrigRanges", this).call(this, cr, offset);
      this.items.forEach(function (node) {
        if (node instanceof _Node2.default) {
          offset = node.setOrigRanges(cr, offset);
        } else if (cr.length === 0) {
          node.origOffset = node.offset;
        } else {
          var i = offset;

          while (i < cr.length) {
            if (cr[i] > node.offset) break;else ++i;
          }

          node.origOffset = node.offset + i;
          offset = i;
        }
      });
      return offset;
    }
  }, {
    key: "toString",
    value: function toString() {
      var src = this.context.src,
          items = this.items,
          range = this.range,
          value = this.value;
      if (value != null) return value;
      var nodes = items.filter(function (item) {
        return item instanceof _Node2.default;
      });
      var str = '';
      var prevEnd = range.start;
      nodes.forEach(function (node) {
        var prefix = src.slice(prevEnd, node.range.start);
        prevEnd = node.range.end;
        str += prefix + String(node);

        if (str[str.length - 1] === '\n' && src[prevEnd - 1] !== '\n' && src[prevEnd] === '\n') {
          // Comment range does not include the terminal newline, but its
          // stringified value does. Without this fix, newlines at comment ends
          // get duplicated.
          prevEnd += 1;
        }
      });
      str += src.slice(prevEnd, range.end);
      return _Node2.default.addStringTerminator(src, range.end, str);
    }
  }]);
  return FlowCollection;
}(_Node2.default);

exports.default = FlowCollection;
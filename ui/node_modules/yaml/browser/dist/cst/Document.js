"use strict";

var _interopRequireDefault = require("@babel/runtime/helpers/interopRequireDefault");

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.default = void 0;

var _classCallCheck2 = _interopRequireDefault(require("@babel/runtime/helpers/classCallCheck"));

var _possibleConstructorReturn2 = _interopRequireDefault(require("@babel/runtime/helpers/possibleConstructorReturn"));

var _getPrototypeOf2 = _interopRequireDefault(require("@babel/runtime/helpers/getPrototypeOf"));

var _get2 = _interopRequireDefault(require("@babel/runtime/helpers/get"));

var _createClass2 = _interopRequireDefault(require("@babel/runtime/helpers/createClass"));

var _inherits2 = _interopRequireDefault(require("@babel/runtime/helpers/inherits"));

var _constants = require("../constants");

var _errors = require("../errors");

var _BlankLine = _interopRequireDefault(require("./BlankLine"));

var _Collection = require("./Collection");

var _Comment = _interopRequireDefault(require("./Comment"));

var _Directive = _interopRequireDefault(require("./Directive"));

var _Node2 = _interopRequireDefault(require("./Node"));

var _Range = _interopRequireDefault(require("./Range"));

var Document =
/*#__PURE__*/
function (_Node) {
  (0, _inherits2.default)(Document, _Node);
  (0, _createClass2.default)(Document, null, [{
    key: "startCommentOrEndBlankLine",
    value: function startCommentOrEndBlankLine(src, start) {
      var offset = _Node2.default.endOfWhiteSpace(src, start);

      var ch = src[offset];
      return ch === '#' || ch === '\n' ? offset : start;
    }
  }]);

  function Document() {
    var _this;

    (0, _classCallCheck2.default)(this, Document);
    _this = (0, _possibleConstructorReturn2.default)(this, (0, _getPrototypeOf2.default)(Document).call(this, _constants.Type.DOCUMENT));
    _this.directives = null;
    _this.contents = null;
    _this.directivesEndMarker = null;
    _this.documentEndMarker = null;
    return _this;
  }

  (0, _createClass2.default)(Document, [{
    key: "parseDirectives",
    value: function parseDirectives(start) {
      var src = this.context.src;
      this.directives = [];
      var atLineStart = true;
      var hasDirectives = false;
      var offset = start;

      while (!_Node2.default.atDocumentBoundary(src, offset, _constants.Char.DIRECTIVES_END)) {
        offset = Document.startCommentOrEndBlankLine(src, offset);

        switch (src[offset]) {
          case '\n':
            if (atLineStart) {
              var blankLine = new _BlankLine.default();
              offset = blankLine.parse({
                src: src
              }, offset);

              if (offset < src.length) {
                this.directives.push(blankLine);
              }
            } else {
              offset += 1;
              atLineStart = true;
            }

            break;

          case '#':
            {
              var comment = new _Comment.default();
              offset = comment.parse({
                src: src
              }, offset);
              this.directives.push(comment);
              atLineStart = false;
            }
            break;

          case '%':
            {
              var directive = new _Directive.default();
              offset = directive.parse({
                parent: this,
                src: src
              }, offset);
              this.directives.push(directive);
              hasDirectives = true;
              atLineStart = false;
            }
            break;

          default:
            if (hasDirectives) {
              this.error = new _errors.YAMLSemanticError(this, 'Missing directives-end indicator line');
            } else if (this.directives.length > 0) {
              this.contents = this.directives;
              this.directives = [];
            }

            return offset;
        }
      }

      if (src[offset]) {
        this.directivesEndMarker = new _Range.default(offset, offset + 3);
        return offset + 3;
      }

      if (hasDirectives) {
        this.error = new _errors.YAMLSemanticError(this, 'Missing directives-end indicator line');
      } else if (this.directives.length > 0) {
        this.contents = this.directives;
        this.directives = [];
      }

      return offset;
    }
  }, {
    key: "parseContents",
    value: function parseContents(start) {
      var _this$context = this.context,
          parseNode = _this$context.parseNode,
          src = _this$context.src;
      if (!this.contents) this.contents = [];
      var lineStart = start;

      while (src[lineStart - 1] === '-') {
        lineStart -= 1;
      }

      var offset = _Node2.default.endOfWhiteSpace(src, start);

      var atLineStart = lineStart === start;
      this.valueRange = new _Range.default(offset);

      while (!_Node2.default.atDocumentBoundary(src, offset, _constants.Char.DOCUMENT_END)) {
        switch (src[offset]) {
          case '\n':
            if (atLineStart) {
              var blankLine = new _BlankLine.default();
              offset = blankLine.parse({
                src: src
              }, offset);

              if (offset < src.length) {
                this.contents.push(blankLine);
              }
            } else {
              offset += 1;
              atLineStart = true;
            }

            lineStart = offset;
            break;

          case '#':
            {
              var comment = new _Comment.default();
              offset = comment.parse({
                src: src
              }, offset);
              this.contents.push(comment);
              atLineStart = false;
            }
            break;

          default:
            {
              var iEnd = _Node2.default.endOfIndent(src, offset);

              var context = {
                atLineStart: atLineStart,
                indent: -1,
                inFlow: false,
                inCollection: false,
                lineStart: lineStart,
                parent: this
              };
              var node = parseNode(context, iEnd);
              if (!node) return this.valueRange.end = iEnd; // at next document start

              this.contents.push(node);
              offset = node.range.end;
              atLineStart = false;
              var ec = (0, _Collection.grabCollectionEndComments)(node);
              if (ec) Array.prototype.push.apply(this.contents, ec);
            }
        }

        offset = Document.startCommentOrEndBlankLine(src, offset);
      }

      this.valueRange.end = offset;

      if (src[offset]) {
        this.documentEndMarker = new _Range.default(offset, offset + 3);
        offset += 3;

        if (src[offset]) {
          offset = _Node2.default.endOfWhiteSpace(src, offset);

          if (src[offset] === '#') {
            var _comment = new _Comment.default();

            offset = _comment.parse({
              src: src
            }, offset);
            this.contents.push(_comment);
          }

          switch (src[offset]) {
            case '\n':
              offset += 1;
              break;

            case undefined:
              break;

            default:
              this.error = new _errors.YAMLSyntaxError(this, 'Document end marker line cannot have a non-comment suffix');
          }
        }
      }

      return offset;
    }
    /**
     * @param {ParseContext} context
     * @param {number} start - Index of first character
     * @returns {number} - Index of the character after this
     */

  }, {
    key: "parse",
    value: function parse(context, start) {
      context.root = this;
      this.context = context;
      var src = context.src;
      var offset = src.charCodeAt(start) === 0xfeff ? start + 1 : start; // skip BOM

      offset = this.parseDirectives(offset);
      offset = this.parseContents(offset);
      return offset;
    }
  }, {
    key: "setOrigRanges",
    value: function setOrigRanges(cr, offset) {
      offset = (0, _get2.default)((0, _getPrototypeOf2.default)(Document.prototype), "setOrigRanges", this).call(this, cr, offset);
      this.directives.forEach(function (node) {
        offset = node.setOrigRanges(cr, offset);
      });
      if (this.directivesEndMarker) offset = this.directivesEndMarker.setOrigRange(cr, offset);
      this.contents.forEach(function (node) {
        offset = node.setOrigRanges(cr, offset);
      });
      if (this.documentEndMarker) offset = this.documentEndMarker.setOrigRange(cr, offset);
      return offset;
    }
  }, {
    key: "toString",
    value: function toString() {
      var contents = this.contents,
          directives = this.directives,
          value = this.value;
      if (value != null) return value;
      var str = directives.join('');

      if (contents.length > 0) {
        if (directives.length > 0 || contents[0].type === _constants.Type.COMMENT) str += '---\n';
        str += contents.join('');
      }

      if (str[str.length - 1] !== '\n') str += '\n';
      return str;
    }
  }]);
  return Document;
}(_Node2.default);

exports.default = Document;
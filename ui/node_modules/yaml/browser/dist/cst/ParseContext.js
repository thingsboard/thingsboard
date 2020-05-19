"use strict";

var _interopRequireDefault = require("@babel/runtime/helpers/interopRequireDefault");

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.default = void 0;

var _classCallCheck2 = _interopRequireDefault(require("@babel/runtime/helpers/classCallCheck"));

var _createClass2 = _interopRequireDefault(require("@babel/runtime/helpers/createClass"));

var _defineProperty2 = _interopRequireDefault(require("@babel/runtime/helpers/defineProperty"));

var _constants = require("../constants");

var _errors = require("../errors");

var _Alias = _interopRequireDefault(require("./Alias"));

var _BlockValue = _interopRequireDefault(require("./BlockValue"));

var _Collection = _interopRequireDefault(require("./Collection"));

var _CollectionItem = _interopRequireDefault(require("./CollectionItem"));

var _FlowCollection = _interopRequireDefault(require("./FlowCollection"));

var _Node = _interopRequireDefault(require("./Node"));

var _PlainValue = _interopRequireDefault(require("./PlainValue"));

var _QuoteDouble = _interopRequireDefault(require("./QuoteDouble"));

var _QuoteSingle = _interopRequireDefault(require("./QuoteSingle"));

var _Range = _interopRequireDefault(require("./Range"));

/**
 * @param {boolean} atLineStart - Node starts at beginning of line
 * @param {boolean} inFlow - true if currently in a flow context
 * @param {boolean} inCollection - true if currently in a collection context
 * @param {number} indent - Current level of indentation
 * @param {number} lineStart - Start of the current line
 * @param {Node} parent - The parent of the node
 * @param {string} src - Source of the YAML document
 */
var ParseContext =
/*#__PURE__*/
function () {
  (0, _createClass2.default)(ParseContext, null, [{
    key: "parseType",
    value: function parseType(src, offset, inFlow) {
      switch (src[offset]) {
        case '*':
          return _constants.Type.ALIAS;

        case '>':
          return _constants.Type.BLOCK_FOLDED;

        case '|':
          return _constants.Type.BLOCK_LITERAL;

        case '{':
          return _constants.Type.FLOW_MAP;

        case '[':
          return _constants.Type.FLOW_SEQ;

        case '?':
          return !inFlow && _Node.default.atBlank(src, offset + 1, true) ? _constants.Type.MAP_KEY : _constants.Type.PLAIN;

        case ':':
          return !inFlow && _Node.default.atBlank(src, offset + 1, true) ? _constants.Type.MAP_VALUE : _constants.Type.PLAIN;

        case '-':
          return !inFlow && _Node.default.atBlank(src, offset + 1, true) ? _constants.Type.SEQ_ITEM : _constants.Type.PLAIN;

        case '"':
          return _constants.Type.QUOTE_DOUBLE;

        case "'":
          return _constants.Type.QUOTE_SINGLE;

        default:
          return _constants.Type.PLAIN;
      }
    }
  }]);

  function ParseContext() {
    var _this = this;

    var orig = arguments.length > 0 && arguments[0] !== undefined ? arguments[0] : {};

    var _ref = arguments.length > 1 && arguments[1] !== undefined ? arguments[1] : {},
        atLineStart = _ref.atLineStart,
        inCollection = _ref.inCollection,
        inFlow = _ref.inFlow,
        indent = _ref.indent,
        lineStart = _ref.lineStart,
        parent = _ref.parent;

    (0, _classCallCheck2.default)(this, ParseContext);
    (0, _defineProperty2.default)(this, "parseNode", function (overlay, start) {
      if (_Node.default.atDocumentBoundary(_this.src, start)) return null;
      var context = new ParseContext(_this, overlay);

      var _context$parseProps = context.parseProps(start),
          props = _context$parseProps.props,
          type = _context$parseProps.type,
          valueStart = _context$parseProps.valueStart;

      var node;

      switch (type) {
        case _constants.Type.ALIAS:
          node = new _Alias.default(type, props);
          break;

        case _constants.Type.BLOCK_FOLDED:
        case _constants.Type.BLOCK_LITERAL:
          node = new _BlockValue.default(type, props);
          break;

        case _constants.Type.FLOW_MAP:
        case _constants.Type.FLOW_SEQ:
          node = new _FlowCollection.default(type, props);
          break;

        case _constants.Type.MAP_KEY:
        case _constants.Type.MAP_VALUE:
        case _constants.Type.SEQ_ITEM:
          node = new _CollectionItem.default(type, props);
          break;

        case _constants.Type.COMMENT:
        case _constants.Type.PLAIN:
          node = new _PlainValue.default(type, props);
          break;

        case _constants.Type.QUOTE_DOUBLE:
          node = new _QuoteDouble.default(type, props);
          break;

        case _constants.Type.QUOTE_SINGLE:
          node = new _QuoteSingle.default(type, props);
          break;

        default:
          node.error = new _errors.YAMLSyntaxError(node, "Unknown node type: ".concat(JSON.stringify(type)));
          node.range = new _Range.default(start, start + 1);
          return node;
      }

      var offset = node.parse(context, valueStart);
      node.range = new _Range.default(start, offset);

      if (offset <= start) {
        node.error = new Error("Node#parse consumed no characters");
        node.error.parseEnd = offset;
        node.error.source = node;
        node.range.end = start + 1;
      }

      if (context.nodeStartsCollection(node)) {
        if (!node.error && !context.atLineStart && context.parent.type === _constants.Type.DOCUMENT) {
          node.error = new _errors.YAMLSyntaxError(node, 'Block collection must not have preceding content here (e.g. directives-end indicator)');
        }

        var collection = new _Collection.default(node);
        offset = collection.parse(new ParseContext(context), offset);
        collection.range = new _Range.default(start, offset);
        return collection;
      }

      return node;
    });
    this.atLineStart = atLineStart != null ? atLineStart : orig.atLineStart || false;
    this.inCollection = inCollection != null ? inCollection : orig.inCollection || false;
    this.inFlow = inFlow != null ? inFlow : orig.inFlow || false;
    this.indent = indent != null ? indent : orig.indent;
    this.lineStart = lineStart != null ? lineStart : orig.lineStart;
    this.parent = parent != null ? parent : orig.parent || {};
    this.root = orig.root;
    this.src = orig.src;
  } // for logging


  (0, _createClass2.default)(ParseContext, [{
    key: "nodeStartsCollection",
    value: function nodeStartsCollection(node) {
      var inCollection = this.inCollection,
          inFlow = this.inFlow,
          src = this.src;
      if (inCollection || inFlow) return false;
      if (node instanceof _CollectionItem.default) return true; // check for implicit key

      var offset = node.range.end;
      if (src[offset] === '\n' || src[offset - 1] === '\n') return false;
      offset = _Node.default.endOfWhiteSpace(src, offset);
      return src[offset] === ':';
    } // Anchor and tag are before type, which determines the node implementation
    // class; hence this intermediate step.

  }, {
    key: "parseProps",
    value: function parseProps(offset) {
      var inFlow = this.inFlow,
          parent = this.parent,
          src = this.src;
      var props = [];
      var lineHasProps = false;
      offset = _Node.default.endOfWhiteSpace(src, offset);
      var ch = src[offset];

      while (ch === _constants.Char.ANCHOR || ch === _constants.Char.COMMENT || ch === _constants.Char.TAG || ch === '\n') {
        if (ch === '\n') {
          var lineStart = offset + 1;

          var inEnd = _Node.default.endOfIndent(src, lineStart);

          var indentDiff = inEnd - (lineStart + this.indent);
          var noIndicatorAsIndent = parent.type === _constants.Type.SEQ_ITEM && parent.context.atLineStart;
          if (!_Node.default.nextNodeIsIndented(src[inEnd], indentDiff, !noIndicatorAsIndent)) break;
          this.atLineStart = true;
          this.lineStart = lineStart;
          lineHasProps = false;
          offset = inEnd;
        } else if (ch === _constants.Char.COMMENT) {
          var end = _Node.default.endOfLine(src, offset + 1);

          props.push(new _Range.default(offset, end));
          offset = end;
        } else {
          var _end = _Node.default.endOfIdentifier(src, offset + 1);

          if (ch === _constants.Char.TAG && src[_end] === ',' && /^[a-zA-Z0-9-]+\.[a-zA-Z0-9-]+,\d\d\d\d(-\d\d){0,2}\/\S/.test(src.slice(offset + 1, _end + 13))) {
            // Let's presume we're dealing with a YAML 1.0 domain tag here, rather
            // than an empty but 'foo.bar' private-tagged node in a flow collection
            // followed without whitespace by a plain string starting with a year
            // or date divided by something.
            _end = _Node.default.endOfIdentifier(src, _end + 5);
          }

          props.push(new _Range.default(offset, _end));
          lineHasProps = true;
          offset = _Node.default.endOfWhiteSpace(src, _end);
        }

        ch = src[offset];
      } // '- &a : b' has an anchor on an empty node


      if (lineHasProps && ch === ':' && _Node.default.atBlank(src, offset + 1, true)) offset -= 1;
      var type = ParseContext.parseType(src, offset, inFlow);
      return {
        props: props,
        type: type,
        valueStart: offset
      };
    }
    /**
     * Parses a node from the source
     * @param {ParseContext} overlay
     * @param {number} start - Index of first non-whitespace character for the node
     * @returns {?Node} - null if at a document boundary
     */

  }, {
    key: "pretty",
    get: function get() {
      var obj = {
        start: "".concat(this.lineStart, " + ").concat(this.indent),
        in: [],
        parent: this.parent.type
      };
      if (!this.atLineStart) obj.start += ' + N';
      if (this.inCollection) obj.in.push('collection');
      if (this.inFlow) obj.in.push('flow');
      return obj;
    }
  }]);
  return ParseContext;
}();

exports.default = ParseContext;
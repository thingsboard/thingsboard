"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.default = normalize;

var _path = _interopRequireDefault(require("path"));

var _normalizePath = _interopRequireDefault(require("normalize-path"));

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

function escape(context, from) {
  if (from && _path.default.isAbsolute(from)) {
    return from;
  } // Ensure context is escaped before globbing
  // Handles special characters in paths


  const absoluteContext = _path.default.resolve(context) // Need refactor
  // eslint-disable-next-line no-useless-escape
  .replace(/[\*|\?|\!|\(|\)|\[|\]|\{|\}]/g, substring => `[${substring}]`);

  if (!from) {
    return absoluteContext;
  } // Cannot use path.join/resolve as it "fixes" the path separators


  if (absoluteContext.endsWith('/')) {
    return `${absoluteContext}${from}`;
  }

  return `${absoluteContext}/${from}`;
}

function normalize(context, from) {
  return (0, _normalizePath.default)(escape(context, from));
}
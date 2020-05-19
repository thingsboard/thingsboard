"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports["default"] = _default;
exports.messages = exports.ruleName = void 0;

var _path = _interopRequireDefault(require("path"));

var _stylelint = require("stylelint");

var _utils = require("../../utils");

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { "default": obj }; }

var ruleName = (0, _utils.namespace)("partial-no-import");
exports.ruleName = ruleName;

var messages = _stylelint.utils.ruleMessages(ruleName, {
  expected: "Unexpected @import in a partial"
});

exports.messages = messages;

function _default(on) {
  return function (root, result) {
    var validOptions = _stylelint.utils.validateOptions(result, ruleName, {
      actual: on
    });

    if (!validOptions) {
      return;
    }

    if (root.source.input.file === undefined || !root.source.input.file) {
      result.warn("The 'partial-no-import' rule won't work if linting in a code string without an actual file.");
      return;
    }

    var fileName = _path["default"].basename(root.source.input.file);

    var extName = _path["default"].extname(root.source.input.file);

    function checkImportForCSS(path, decl) {
      // Stripping trailing quotes and whitespaces, if any
      var pathStripped = path.replace(/^\s*?("|')\s*/, "").replace(/\s*("|')\s*?$/, ""); // Skipping importing empty import, CSS: url(), ".css", URI with a protocol, media

      if (pathStripped.trim() === "" || pathStripped.slice(0, 4) === "url(" || pathStripped.slice(-4) === ".css" || pathStripped.search("//") !== -1 || pathStripped.search(/(?:\s|[,)"'])\w+$/) !== -1) {
        return;
      }

      _stylelint.utils.report({
        message: messages.expected,
        node: decl,
        index: decl.params.indexOf(path),
        result: result,
        ruleName: ruleName
      });
    } // Usual CSS file


    if (extName === ".css") {
      return;
    } // Not a partial


    if (fileName[0] !== "_") {
      return;
    }

    root.walkAtRules("import", function (mixinCall) {
      // Check if @import is treated as CSS import; report only if not
      // Processing comma-separated lists of import paths
      mixinCall.params.split(/["']\s*,/).forEach(function (path) {
        checkImportForCSS(path, mixinCall);
      });
    });
  };
}
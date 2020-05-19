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

var ruleName = (0, _utils.namespace)("at-import-partial-extension");
exports.ruleName = ruleName;

var messages = _stylelint.utils.ruleMessages(ruleName, {
  expected: "Expected @import to have an extension",
  rejected: function rejected(ext) {
    return "Unexpected extension \".".concat(ext, "\" in @import");
  }
}); // https://drafts.csswg.org/mediaqueries/#media-types


exports.messages = messages;
var mediaQueryTypes = ["all", "print", "screen", "speech", "tv", "tty", "projection", "handheld", "braille", "embossed", "aural"];
var mediaQueryTypesRE = new RegExp("(".concat(mediaQueryTypes.join("|"), ")$"), "i");

var stripPath = function stripPath(path) {
  return path.replace(/^\s*?("|')\s*/, "").replace(/\s*("|')\s*?$/, "");
};

function _default(expectation) {
  return function (root, result) {
    var validOptions = _stylelint.utils.validateOptions(result, ruleName, {
      actual: expectation,
      possible: ["always", "never"]
    });

    if (!validOptions) {
      return;
    }

    root.walkAtRules("import", function (decl) {
      var paths = decl.params.split(",").filter(function (path) {
        return !mediaQueryTypesRE.test(path.trim());
      }); // Processing comma-separated lists of import paths

      paths.forEach(function (path) {
        // Stripping trailing quotes and whitespaces, if any
        var pathStripped = stripPath(path); // Skipping importing CSS: url(), ".css", URI with a protocol

        if (pathStripped.slice(0, 4) === "url(" || pathStripped.slice(-4) === ".css" || pathStripped.search("//") !== -1) {
          return;
        }

        var extension = _path["default"].extname(pathStripped).slice(1);

        if (!extension && expectation === "always") {
          _stylelint.utils.report({
            message: messages.expected,
            node: decl,
            result: result,
            ruleName: ruleName
          });

          return;
        }

        if (extension && expectation === "never") {
          _stylelint.utils.report({
            message: messages.rejected(extension),
            node: decl,
            word: extension,
            result: result,
            ruleName: ruleName
          });
        }
      });
    });
  };
}
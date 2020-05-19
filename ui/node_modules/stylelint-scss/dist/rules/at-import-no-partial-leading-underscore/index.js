"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports["default"] = _default;
exports.messages = exports.ruleName = void 0;

var _stylelint = require("stylelint");

var _utils = require("../../utils");

var ruleName = (0, _utils.namespace)("at-import-no-partial-leading-underscore");
exports.ruleName = ruleName;

var messages = _stylelint.utils.ruleMessages(ruleName, {
  expected: "Unexpected leading underscore in imported partial name"
});

exports.messages = messages;

function _default(actual) {
  return function (root, result) {
    var validOptions = _stylelint.utils.validateOptions(result, ruleName, {
      actual: actual
    });

    if (!validOptions) {
      return;
    }

    function checkPathForUnderscore(path, decl) {
      // Stripping trailing quotes and whitespaces, if any
      var pathStripped = path.replace(/^\s*?("|')\s*/, "").replace(/\s*("|')\s*?$/, ""); // Searching a _ at the start of filename

      if (pathStripped.search(/(?:^|\/|\\)_[^/]+$/) === -1) {
        return;
      } // Skipping importing CSS: url(), ".css", URI with a protocol, media


      if (pathStripped.slice(0, 4) === "url(" || pathStripped.slice(-4) === ".css" || pathStripped.search("//") !== -1 || pathStripped.search(/(?:\s|[,)"'])\w+$/) !== -1) {
        return;
      }

      _stylelint.utils.report({
        message: messages.expected,
        node: decl,
        result: result,
        ruleName: ruleName
      });
    }

    root.walkAtRules("import", function (decl) {
      // Processing comma-separated lists of import paths
      decl.params.split(",").forEach(function (path) {
        checkPathForUnderscore(path, decl);
      });
    });
  };
}
"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports["default"] = _default;
exports.messages = exports.ruleName = void 0;

var _stylelint = require("stylelint");

var _utils = require("../../utils");

var ruleName = (0, _utils.namespace)("at-else-if-parentheses-space-before");
exports.ruleName = ruleName;

var messages = _stylelint.utils.ruleMessages(ruleName, {
  rejectedBefore: function rejectedBefore() {
    return "Unexpected whitespace before parentheses in else-if declaration";
  },
  expectedBefore: function expectedBefore() {
    return "Expected a single space before parentheses in else-if declaration";
  }
});

exports.messages = messages;

function _default(value, _, context) {
  return function (root, result) {
    var validOptions = _stylelint.utils.validateOptions(result, ruleName, {
      actual: value,
      possible: ["always", "never"]
    });

    if (!validOptions) {
      return;
    }

    var match = /^if\s*?\(/;
    var replacement = value === "always" ? "if (" : "if(";
    var checker = (0, _utils.whitespaceChecker)("space", value, messages).before;
    root.walkAtRules("else", function (decl) {
      // return early if the else-if statement is not surrounded by parentheses
      if (!match.test(decl.params)) {
        return;
      }

      if (context.fix) {
        decl.params = decl.params.replace(match, replacement);
      }

      checker({
        source: decl.params,
        index: decl.params.indexOf("("),
        err: function err(message) {
          return _stylelint.utils.report({
            message: message,
            node: decl,
            result: result,
            ruleName: ruleName
          });
        }
      });
    });
  };
}
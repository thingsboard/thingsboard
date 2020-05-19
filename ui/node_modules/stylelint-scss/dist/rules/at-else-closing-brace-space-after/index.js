"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports["default"] = _default;
exports.messages = exports.ruleName = void 0;

var _utils = require("../../utils");

var _stylelint = require("stylelint");

var _atIfClosingBraceSpaceAfter = require("../at-if-closing-brace-space-after");

var ruleName = (0, _utils.namespace)("at-else-closing-brace-space-after");
exports.ruleName = ruleName;

var messages = _stylelint.utils.ruleMessages(ruleName, {
  expected: 'Expected single space after "}" of @else statement',
  rejected: 'Unexpected space after "}" of @else statement'
});

exports.messages = messages;

function _default(expectation, _, context) {
  return function (root, result) {
    var validOptions = _stylelint.utils.validateOptions(result, ruleName, {
      actual: expectation,
      possible: ["always-intermediate", "never-intermediate"]
    });

    if (!validOptions) {
      return;
    }

    (0, _atIfClosingBraceSpaceAfter.sassConditionalBraceSpaceAfterChecker)({
      root: root,
      result: result,
      ruleName: ruleName,
      atRuleName: "else",
      expectation: expectation,
      messages: messages,
      context: context
    });
  };
}
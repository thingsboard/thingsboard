"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports["default"] = _default;
exports.messages = exports.ruleName = void 0;

var _utils = require("../../utils");

var _stylelint = require("stylelint");

var _dollarVariableColonSpaceAfter = require("../dollar-variable-colon-space-after");

var ruleName = (0, _utils.namespace)("dollar-variable-colon-space-before");
exports.ruleName = ruleName;

var messages = _stylelint.utils.ruleMessages(ruleName, {
  expectedBefore: function expectedBefore() {
    return 'Expected single space before ":"';
  },
  rejectedBefore: function rejectedBefore() {
    return 'Unexpected whitespace before ":"';
  }
});

exports.messages = messages;

function _default(expectation, _, context) {
  var checker = (0, _utils.whitespaceChecker)("space", expectation, messages);
  return function (root, result) {
    var validOptions = _stylelint.utils.validateOptions(result, ruleName, {
      actual: expectation,
      possible: ["always", "never"]
    });

    if (!validOptions) {
      return;
    }

    (0, _dollarVariableColonSpaceAfter.variableColonSpaceChecker)({
      root: root,
      result: result,
      locationChecker: checker.before,
      checkedRuleName: ruleName,
      position: "before",
      expectation: expectation,
      context: context
    });
  };
}
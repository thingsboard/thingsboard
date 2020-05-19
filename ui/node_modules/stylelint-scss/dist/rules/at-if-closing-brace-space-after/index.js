"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports["default"] = _default;
exports.sassConditionalBraceSpaceAfterChecker = sassConditionalBraceSpaceAfterChecker;
exports.messages = exports.ruleName = void 0;

var _utils = require("../../utils");

var _stylelint = require("stylelint");

var ruleName = (0, _utils.namespace)("at-if-closing-brace-space-after");
exports.ruleName = ruleName;

var messages = _stylelint.utils.ruleMessages(ruleName, {
  expected: 'Expected single space after "}" of @if statement',
  rejected: 'Unexpected space after "}" of @if statement'
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

    sassConditionalBraceSpaceAfterChecker({
      root: root,
      result: result,
      ruleName: ruleName,
      atRuleName: "if",
      expectation: expectation,
      messages: messages,
      context: context
    });
  };
}
/**
 * The core logic for this rule. Can be imported by other rules with similar
 * logic, namely at-else-closing-brace-space-after
 *
 * @param {Object} args -- Named arguments object
 * @param {PostCSS root} args.root
 * @param {PostCSS result} args.result
 * @param {String ruleName} args.ruleName - needed for `report` function
 * @param {String} args.atRuleName - the name of the at-rule to be checked, e.g. "if", "else"
 * @param {Object} args.messages - returned by stylelint.utils.ruleMessages
 * @return {undefined}
 */


function sassConditionalBraceSpaceAfterChecker(_ref) {
  var root = _ref.root,
      result = _ref.result,
      ruleName = _ref.ruleName,
      atRuleName = _ref.atRuleName,
      expectation = _ref.expectation,
      messages = _ref.messages,
      context = _ref.context;

  function complain(node, message, index, fixValue) {
    if (context.fix) {
      node.next().raws.before = fixValue;
      return;
    }

    _stylelint.utils.report({
      result: result,
      ruleName: ruleName,
      node: node,
      message: message,
      index: index
    });
  }

  root.walkAtRules(function (atrule) {
    // Do nothing if it's not an @if
    if (atrule.name !== atRuleName) {
      return;
    }

    var nextNode = atrule.next();
    var hasSpaceAfter = nextNode && nextNode.raws.before === " ";
    var hasWhiteSpaceAfter = nextNode && nextNode.raws.before !== "";
    var reportIndex = atrule.toString().length; // When followed by an @else

    if (nextNode && nextNode.type === "atrule" && nextNode.name === "else") {
      // A single space is needed
      if (expectation === "always-intermediate" && !hasSpaceAfter) {
        complain(atrule, messages.expected, reportIndex, " ");
      } else if (expectation === "never-intermediate" && hasWhiteSpaceAfter) {
        // No whitespace is needed
        complain(atrule, messages.rejected, reportIndex, "");
      }
    }
  });
}
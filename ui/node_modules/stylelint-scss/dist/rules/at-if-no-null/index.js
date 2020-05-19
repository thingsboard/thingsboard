"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports["default"] = _default;
exports.messages = exports.ruleName = void 0;

var _utils = require("../../utils");

var _stylelint = require("stylelint");

var ruleName = (0, _utils.namespace)("at-if-no-null");
exports.ruleName = ruleName;

var messages = _stylelint.utils.ruleMessages(ruleName, {
  equals_null: "Expected @if not statement rather than @if statement == null",
  not_equals_null: "Expected @if statement rather than @if statement != null"
});

exports.messages = messages;

function _default(expectation) {
  return function (root, result) {
    var validOptions = _stylelint.utils.validateOptions(result, ruleName, {
      actual: expectation
    });

    if (!validOptions) {
      return;
    }

    root.walkAtRules(function (atrule) {
      // Do nothing if it's not an @if
      if (atrule.name !== "if") {
        return;
      } // If rule != null and (expr), skip


      if (atrule.params.match(/\(?[ \t]*.* != null and .*\)?/)) {
        return;
      }

      if (atrule.params.match(/\(?[ \t]*.* == null[ \t]*\)?/)) {
        _stylelint.utils.report({
          message: messages.equals_null,
          node: atrule,
          result: result,
          ruleName: ruleName
        });
      } else if (atrule.params.match(/\(?[ \t]*.* != null[ \t]*\)?/)) {
        _stylelint.utils.report({
          message: messages.not_equals_null,
          node: atrule,
          result: result,
          ruleName: ruleName
        });
      }
    });
  };
}
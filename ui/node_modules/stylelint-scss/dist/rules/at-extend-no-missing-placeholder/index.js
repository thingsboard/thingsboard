"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports["default"] = _default;
exports.messages = exports.ruleName = void 0;

var _stylelint = require("stylelint");

var _utils = require("../../utils");

var ruleName = (0, _utils.namespace)("at-extend-no-missing-placeholder");
exports.ruleName = ruleName;

var messages = _stylelint.utils.ruleMessages(ruleName, {
  rejected: "Expected a placeholder selector (e.g. %placeholder) to be used in @extend"
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

    root.walkAtRules("extend", function (atrule) {
      var isPlaceholder = atrule.params.trim()[0] === "%";
      var isInterpolation = /^#{.+}/.test(atrule.params.trim());

      if (!isPlaceholder && !isInterpolation) {
        _stylelint.utils.report({
          ruleName: ruleName,
          result: result,
          node: atrule,
          message: messages.rejected
        });
      }
    });
  };
}
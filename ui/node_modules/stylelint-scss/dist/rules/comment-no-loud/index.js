"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports["default"] = exports.messages = exports.ruleName = void 0;

var _stylelint = require("stylelint");

var _utils = require("../../utils");

var ruleName = (0, _utils.namespace)("comment-no-loud");
exports.ruleName = ruleName;

var messages = _stylelint.utils.ruleMessages(ruleName, {
  expected: "Expected // for comments instead of /*"
});

exports.messages = messages;

function rule(primary) {
  return function (root, result) {
    var validOptions = _stylelint.utils.validateOptions(result, ruleName, {
      actual: primary
    });

    if (!validOptions) {
      return;
    }

    root.walkComments(function (comment) {
      if (isLoudComment(comment)) {
        _stylelint.utils.report({
          message: messages.expected,
          node: comment,
          result: result,
          ruleName: ruleName
        });
      }
    });
  };
}

function isLoudComment(comment) {
  var regex = new RegExp(/^[ \t\n]*\/\*/);
  return regex.test(comment.source.input.css);
}

var _default = rule;
exports["default"] = _default;
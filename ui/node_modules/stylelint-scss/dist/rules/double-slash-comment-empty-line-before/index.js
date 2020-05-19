"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports["default"] = _default;
exports.messages = exports.ruleName = void 0;

var _stylelint = require("stylelint");

var _utils = require("../../utils");

var ruleName = (0, _utils.namespace)("double-slash-comment-empty-line-before");
exports.ruleName = ruleName;

var messages = _stylelint.utils.ruleMessages(ruleName, {
  expected: "Expected empty line before comment",
  rejected: "Unexpected empty line before comment"
});

exports.messages = messages;
var stylelintCommandPrefix = "stylelint-";

function _default(expectation, options, context) {
  return function (root, result) {
    var validOptions = _stylelint.utils.validateOptions(result, ruleName, {
      actual: expectation,
      possible: ["always", "never"]
    }, {
      actual: options,
      possible: {
        except: ["first-nested"],
        ignore: ["stylelint-commands", "between-comments"]
      },
      optional: true
    });

    if (!validOptions) {
      return;
    }

    root.walkComments(function (comment) {
      // Only process // comments
      if (!comment.raws.inline && !comment.inline) {
        return;
      }

      if ((0, _utils.isInlineComment)(comment)) {
        return;
      } // Ignore the first node


      if (comment === root.first) {
        return;
      } // Optionally ignore stylelint commands


      if (comment.text.indexOf(stylelintCommandPrefix) === 0 && (0, _utils.optionsHaveIgnored)(options, "stylelint-commands")) {
        return;
      } // Optionally ignore newlines between comments


      var prev = comment.prev();

      if (prev && prev.type === "comment" && (0, _utils.optionsHaveIgnored)(options, "between-comments")) {
        return;
      }

      var before = comment.raw("before");

      var expectEmptyLineBefore = function () {
        if ((0, _utils.optionsHaveException)(options, "first-nested") && comment.parent !== root && comment === comment.parent.first) {
          return false;
        }

        return expectation === "always";
      }();

      var hasEmptyLineBefore = before.search(/\n\s*?\n/) !== -1; // Return if the expectation is met

      if (expectEmptyLineBefore === hasEmptyLineBefore) {
        return;
      }

      if (context.fix) {
        if (expectEmptyLineBefore && !hasEmptyLineBefore) {
          (0, _utils.addEmptyLineBefore)(comment, context.newline);
          return;
        }

        if (!expectEmptyLineBefore && hasEmptyLineBefore) {
          (0, _utils.removeEmptyLinesBefore)(comment, context.newline);
          return;
        }
      }

      var message = expectEmptyLineBefore ? messages.expected : messages.rejected;

      _stylelint.utils.report({
        message: message,
        node: comment,
        result: result,
        ruleName: ruleName
      });
    });
  };
}
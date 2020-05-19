"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports["default"] = _default;
exports.messages = exports.ruleName = void 0;

var _stylelint = require("stylelint");

var _utils = require("../../utils");

var ruleName = (0, _utils.namespace)("at-mixin-argumentless-call-parentheses");
exports.ruleName = ruleName;

var messages = _stylelint.utils.ruleMessages(ruleName, {
  expected: function expected(mixin) {
    return "Expected parentheses in mixin \"".concat(mixin, "\" call");
  },
  rejected: function rejected(mixin) {
    return "Unexpected parentheses in argumentless mixin \"".concat(mixin, "\" call");
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

    root.walkAtRules("include", function (mixinCall) {
      // If it is "No parens in argumentless calls"
      if (value === "never" && mixinCall.params.search(/\(\s*?\)\s*?$/) === -1) {
        return;
      } // If it is "Always use parens"


      if (value === "always" && mixinCall.params.search(/\(/) !== -1) {
        return;
      }

      if (context.fix) {
        if (value === "always") {
          mixinCall.params = "".concat(mixinCall.params, " ()");
        } else {
          mixinCall.params = mixinCall.params.replace(/\s*\([\s\S]*?\)$/, "");
        }

        return;
      }

      var mixinName = /\s*(\S*?)\s*(?:\(|$)/.exec(mixinCall.params)[1];

      _stylelint.utils.report({
        message: messages[value === "never" ? "rejected" : "expected"](mixinName),
        node: mixinCall,
        result: result,
        ruleName: ruleName
      });
    });
  };
}
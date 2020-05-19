"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports["default"] = _default;
exports.messages = exports.ruleName = void 0;

var _stylelint = require("stylelint");

var _utils = require("../../utils");

var ruleName = (0, _utils.namespace)("no-duplicate-mixins");
exports.ruleName = ruleName;

var messages = _stylelint.utils.ruleMessages(ruleName, {
  rejected: function rejected(mixin) {
    return "Unexpected duplicate mixin ".concat(mixin);
  }
});

exports.messages = messages;

function _default(value) {
  return function (root, result) {
    var validOptions = _stylelint.utils.validateOptions(result, ruleName, {
      actual: value
    });

    if (!validOptions) {
      return;
    }

    var mixins = {};
    root.walkAtRules(function (decl) {
      var isMixin = decl.name === "mixin";

      if (!isMixin) {
        return;
      }

      var mixinName = (0, _utils.atRuleBaseName)(decl);

      if (mixins[mixinName]) {
        _stylelint.utils.report({
          message: messages.rejected(mixinName),
          node: decl,
          result: result,
          ruleName: ruleName
        });
      }

      mixins[mixinName] = true;
    });
  };
}
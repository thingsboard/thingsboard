"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports["default"] = _default;
exports.messages = exports.ruleName = void 0;

var _postcssSelectorParser = require("postcss-selector-parser");

var _stylelint = require("stylelint");

var _utils = require("../../utils");

var ruleName = (0, _utils.namespace)("selector-no-union-class-name");
exports.ruleName = ruleName;

var messages = _stylelint.utils.ruleMessages(ruleName, {
  rejected: "Unexpected union class name with the parent selector (&)"
});

exports.messages = messages;
var validNestingTypes = [_postcssSelectorParser.isClassName, _postcssSelectorParser.isCombinator, _postcssSelectorParser.isAttribute, _postcssSelectorParser.isIdentifier, _postcssSelectorParser.isPseudoClass, _postcssSelectorParser.isPseudoElement];

function _default(actual) {
  return function (root, result) {
    var validOptions = _stylelint.utils.validateOptions(result, ruleName, {
      actual: actual
    });

    if (!validOptions) {
      return;
    }

    root.walkRules(/&/, function (rule) {
      var parentNodes = [];
      var selector = getSelectorFromRule(rule.parent);

      if (selector) {
        (0, _utils.parseSelector)(selector, result, rule, function (fullSelector) {
          fullSelector.walk(function (node) {
            return parentNodes.push(node);
          });
        });
      }

      if (parentNodes.length === 0) return;
      var lastParentNode = parentNodes[parentNodes.length - 1];
      if (!(0, _postcssSelectorParser.isClassName)(lastParentNode)) return;
      (0, _utils.parseSelector)(rule.selector, result, rule, function (fullSelector) {
        fullSelector.walkNesting(function (node) {
          var next = node.next();
          if (!next) return;
          if (validNestingTypes.some(function (isType) {
            return isType(next);
          })) return;

          _stylelint.utils.report({
            ruleName: ruleName,
            result: result,
            node: rule,
            message: messages.rejected,
            index: node.sourceIndex
          });
        });
      });
    });
  };
}
/**
 * Searches for the closest rule which
 * has a selector and returns the selector
 * @returns {string|undefined}
 */


function getSelectorFromRule(rule) {
  // All non at-rules have their own selector
  if (rule.selector !== undefined) {
    return rule.selector;
  } // At-rules like @mixin don't have a selector themself
  // but their parents might have one


  if (rule.parent) {
    return getSelectorFromRule(rule.parent);
  }
}
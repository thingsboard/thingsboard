"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports["default"] = _default;
exports.messages = exports.ruleName = void 0;

var _stylelint = require("stylelint");

var _utils = require("../../utils");

var hasOwnProp = Object.prototype.hasOwnProperty;
var ruleName = (0, _utils.namespace)("declaration-nested-properties-no-divided-groups");
exports.ruleName = ruleName;

var messages = _stylelint.utils.ruleMessages(ruleName, {
  expected: function expected(prop) {
    return "Expected all nested properties of \"".concat(prop, "\" namespace to be in one nested group");
  }
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

    root.walk(function (item) {
      if (item.type !== "rule" && item.type !== "atrule") {
        return;
      }

      var nestedGroups = {}; // Find all nested property groups

      item.each(function (decl) {
        if (decl.type !== "rule") {
          return;
        }

        var testForProp = (0, _utils.parseNestedPropRoot)(decl.selector);

        if (testForProp && testForProp.propName !== undefined) {
          var ns = testForProp.propName.value;

          if (!hasOwnProp.call(nestedGroups, ns)) {
            nestedGroups[ns] = [];
          }

          nestedGroups[ns].push(decl);
        }
      });
      Object.keys(nestedGroups).forEach(function (namespace) {
        // Only warn if there are more than one nested groups with equal namespaces
        if (nestedGroups[namespace].length === 1) {
          return;
        }

        nestedGroups[namespace].forEach(function (group) {
          _stylelint.utils.report({
            message: messages.expected(namespace),
            node: group,
            result: result,
            ruleName: ruleName
          });
        });
      });
    });
  };
}
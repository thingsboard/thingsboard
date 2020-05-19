"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports["default"] = exports.messages = exports.ruleName = void 0;

var _stylelint = require("stylelint");

var _utils = require("../../utils");

var _postcssValueParser = _interopRequireDefault(require("postcss-value-parser"));

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { "default": obj }; }

var ruleName = (0, _utils.namespace)("function-unquote-no-unquoted-strings-inside");
exports.ruleName = ruleName;

var messages = _stylelint.utils.ruleMessages(ruleName, {
  rejected: "Unquote function used with an already-unquoted string"
});

exports.messages = messages;

function rule(primary, _, context) {
  return function (root, result) {
    var validOptions = _stylelint.utils.validateOptions(result, ruleName, {
      actual: primary
    });

    if (!validOptions) {
      return;
    } // Setup variable naming.


    var vars = {};
    root.walkDecls(function (decl) {
      if (decl.prop[0] !== "$") {
        return;
      }

      (0, _postcssValueParser["default"])(decl.value).walk(function (node) {
        vars[decl.prop] = node.type;
      });
    });
    root.walkDecls(function (decl) {
      (0, _postcssValueParser["default"])(decl.value).walk(function (node) {
        // Verify that we're only looking at functions.
        if (node.type !== "function" || (0, _utils.isNativeCssFunction)(node.value) || node.value === "") {
          return;
        } // Verify we're only looking at quote() calls.


        if (node.value !== "unquote") {
          return;
        } // Report error if first character is a quote.
        // postcss-value-parser represents quoted strings as type 'string' (as opposed to word)


        if (!node.nodes[0].quote && node.nodes[0].value[0] !== "$" || vars[node.nodes[0].value] === "word") {
          if (context.fix) {
            var contents = /unquote\((.*)\)/.exec(decl.value);
            decl.value = contents[1];
          } else {
            _stylelint.utils.report({
              message: messages.rejected,
              node: decl,
              result: result,
              ruleName: ruleName
            });
          }
        }
      });
    });
  };
}

var _default = rule;
exports["default"] = _default;
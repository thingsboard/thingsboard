"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports["default"] = exports.messages = exports.ruleName = void 0;

var _stylelint = require("stylelint");

var _utils = require("../../utils");

var _postcssValueParser = _interopRequireDefault(require("postcss-value-parser"));

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { "default": obj }; }

var ruleName = (0, _utils.namespace)("function-color-relative");
exports.ruleName = ruleName;

var messages = _stylelint.utils.ruleMessages(ruleName, {
  rejected: "Expected the scale-color function to be used"
});

exports.messages = messages;
var function_names = ["saturate", "desaturate", "darken", "lighten", "opacify", "fade-in", "transparentize", "fade-out"];

function rule(primary) {
  return function (root, result) {
    var validOptions = _stylelint.utils.validateOptions(result, ruleName, {
      actual: primary
    });

    if (!validOptions) {
      return;
    }

    root.walkDecls(function (decl) {
      (0, _postcssValueParser["default"])(decl.value).walk(function (node) {
        // Verify that we're only looking at functions.
        if (node.type !== "function" || node.value === "") {
          return;
        }

        if (function_names.includes(node.value)) {
          _stylelint.utils.report({
            message: messages.rejected,
            node: decl,
            result: result,
            ruleName: ruleName
          });
        }
      });
    });
  };
}

var _default = rule;
exports["default"] = _default;
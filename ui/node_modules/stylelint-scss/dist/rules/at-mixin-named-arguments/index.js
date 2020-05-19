"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports["default"] = _default;
exports.messages = exports.ruleName = void 0;

var _stylelint = require("stylelint");

var _utils = require("../../utils");

function _toConsumableArray(arr) { return _arrayWithoutHoles(arr) || _iterableToArray(arr) || _nonIterableSpread(); }

function _nonIterableSpread() { throw new TypeError("Invalid attempt to spread non-iterable instance"); }

function _iterableToArray(iter) { if (Symbol.iterator in Object(iter) || Object.prototype.toString.call(iter) === "[object Arguments]") return Array.from(iter); }

function _arrayWithoutHoles(arr) { if (Array.isArray(arr)) { for (var i = 0, arr2 = new Array(arr.length); i < arr.length; i++) { arr2[i] = arr[i]; } return arr2; } }

var ruleName = (0, _utils.namespace)("at-mixin-named-arguments");
exports.ruleName = ruleName;

var messages = _stylelint.utils.ruleMessages(ruleName, {
  expected: "Expected a named parameter to be used in at-include call",
  rejected: "Unexpected a named parameter in at-include call"
});

exports.messages = messages;
var hasArgumentsRegExp = /\((.*)\)$/;
var isScssVarRegExp = /^\$\S*/;

function _default(expectation, options) {
  return function (root, result) {
    var validOptions = _stylelint.utils.validateOptions(result, ruleName, {
      actual: expectation,
      possible: ["always", "never"]
    }, {
      actual: options,
      possible: {
        ignore: ["single-argument"]
      },
      optional: true
    });

    if (!validOptions) {
      return;
    }

    var shouldIgnoreSingleArgument = (0, _utils.optionsHaveIgnored)(options, "single-argument");
    root.walkAtRules("include", function (atRule) {
      var argsString = atRule.params.replace(/\n/g, " ").match(hasArgumentsRegExp); // Ignore @include that does not contain arguments.

      if (!argsString || argsString.index === -1 || argsString[0].length === 2) {
        return;
      }

      var args = argsString[1] // Create array of arguments.
      .split(",") // Create a key-value array for every argument.
      .map(function (argsString) {
        return argsString.split(":").map(function (argsKeyValuePair) {
          return argsKeyValuePair.trim();
        });
      }).reduce(function (resultArray, keyValuePair) {
        var pair = {
          value: keyValuePair[1] || keyValuePair[0]
        };

        if (keyValuePair[1]) {
          pair.key = keyValuePair[0];
        }

        return [].concat(_toConsumableArray(resultArray), [pair]);
      }, []);
      var isSingleArgument = args.length === 1;

      if (isSingleArgument && shouldIgnoreSingleArgument) {
        return;
      }

      args.forEach(function (arg) {
        switch (expectation) {
          case "never":
            {
              if (!arg.key) {
                return;
              }

              _stylelint.utils.report({
                message: messages.rejected,
                node: atRule,
                result: result,
                ruleName: ruleName
              });

              break;
            }

          case "always":
            {
              if (arg.key && isScssVarRegExp.test(arg.key)) {
                return;
              }

              _stylelint.utils.report({
                message: messages.expected,
                node: atRule,
                result: result,
                ruleName: ruleName
              });

              break;
            }
        }
      });
    });
  };
}
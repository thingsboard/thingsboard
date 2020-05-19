"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports["default"] = _default;
exports.messages = exports.ruleName = void 0;

var _stylelint = require("stylelint");

var _utils = require("../../utils");

var hasOwnProp = Object.prototype.hasOwnProperty;
var ruleName = (0, _utils.namespace)("declaration-nested-properties");
exports.ruleName = ruleName;

var messages = _stylelint.utils.ruleMessages(ruleName, {
  expected: function expected(prop) {
    return "Expected property \"".concat(prop, "\" to be in a nested form");
  },
  rejected: function rejected(prop) {
    return "Unexpected nested property \"".concat(prop, "\"");
  }
});

exports.messages = messages;

function _default(expectation, options) {
  return function (root, result) {
    var validOptions = _stylelint.utils.validateOptions(result, ruleName, {
      actual: expectation,
      possible: ["always", "never"]
    }, {
      actual: options,
      possible: {
        except: ["only-of-namespace"]
      },
      optional: true
    });

    if (!validOptions) {
      return;
    }

    if (expectation === "always") {
      root.walk(function (item) {
        if (item.type !== "rule" && item.type !== "atrule") {
          return;
        }

        var warningCandidates = {};
        item.each(function (decl) {
          var prop = decl.prop,
              type = decl.type,
              selector = decl.selector; // Looking for namespaced non-nested properties
          // Namespaced prop is basically a prop with a `-` in a name, e.g. `margin-top`

          if (type === "decl") {
            if (!(0, _utils.isStandardSyntaxProperty)(prop)) {
              return;
            } // Add simple namespaced prop decls to warningCandidates.ns
            // (prop names with browser prefixes are ignored)


            var seekNamespace = /^([a-zA-Z0-9]+)-/.exec(prop);

            if (seekNamespace && seekNamespace[1]) {
              var ns = seekNamespace[1];

              if (!hasOwnProp.call(warningCandidates, ns)) {
                warningCandidates[ns] = [];
              }

              warningCandidates[ns].push({
                node: decl
              });
            }
          } // Nested props, `prop: [value] { <nested decls> }`


          if (type === "rule") {
            // `background:red {` - selector;
            // `background: red {` - nested prop; space is decisive here
            var testForProp = (0, _utils.parseNestedPropRoot)(selector);

            if (testForProp && testForProp.propName !== undefined) {
              var _ns = testForProp.propName.value;

              if (!hasOwnProp.call(warningCandidates, _ns)) {
                warningCandidates[_ns] = [];
              }

              warningCandidates[_ns].push({
                node: decl,
                nested: true
              });
            }
          }
        }); // Now check if the found properties deserve warnings

        Object.keys(warningCandidates).forEach(function (namespace) {
          var exceptIfOnlyOfNs = (0, _utils.optionsHaveException)(options, "only-of-namespace");
          var moreThanOneProp = warningCandidates[namespace].length > 1;
          warningCandidates[namespace].forEach(function (candidate) {
            if (candidate.nested === true) {
              if (exceptIfOnlyOfNs) {
                // If there is only one prop inside a nested prop - warn (reverse "always")
                if (candidate.nested === true && candidate.node.nodes.length === 1) {
                  _stylelint.utils.report({
                    message: messages.rejected(namespace),
                    node: candidate.node,
                    result: result,
                    ruleName: ruleName
                  });
                }
              }
            } else {
              // Don't warn on non-nested namespaced props if there are
              // less than 2 of them, and except: "only-of-namespace" is set
              if (exceptIfOnlyOfNs && !moreThanOneProp) {
                return;
              }

              _stylelint.utils.report({
                message: messages.expected(candidate.node.prop),
                node: candidate.node,
                result: result,
                ruleName: ruleName
              });
            }
          });
        });
      });
    } else if (expectation === "never") {
      root.walk(function (item) {
        // Just check if there are ANY nested props
        if (item.type === "rule") {
          // `background:red {` - selector;
          // `background: red {` - nested prop; space is decisive here
          var testForProp = (0, _utils.parseNestedPropRoot)(item.selector);

          if (testForProp && testForProp.propName !== undefined) {
            _stylelint.utils.report({
              message: messages.rejected(testForProp.propName.value),
              result: result,
              ruleName: ruleName,
              node: item
            });
          }
        }
      });
    }
  };
}
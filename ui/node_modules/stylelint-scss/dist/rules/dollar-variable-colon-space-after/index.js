"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports["default"] = _default;
exports.variableColonSpaceChecker = variableColonSpaceChecker;
exports.messages = exports.ruleName = void 0;

var _utils = require("../../utils");

var _stylelint = require("stylelint");

var ruleName = (0, _utils.namespace)("dollar-variable-colon-space-after");
exports.ruleName = ruleName;

var messages = _stylelint.utils.ruleMessages(ruleName, {
  expectedAfter: function expectedAfter() {
    return 'Expected single space after ":"';
  },
  rejectedAfter: function rejectedAfter() {
    return 'Unexpected whitespace after ":"';
  },
  expectedAfterSingleLine: function expectedAfterSingleLine() {
    return 'Expected single space after ":" with a single-line value';
  },
  expectedAfterAtLeast: function expectedAfterAtLeast() {
    return 'Expected at least one space after ":"';
  }
});

exports.messages = messages;

function _default(expectation, _, context) {
  var checker = (0, _utils.whitespaceChecker)("space", expectation, messages);
  return function (root, result) {
    var validOptions = _stylelint.utils.validateOptions(result, ruleName, {
      actual: expectation,
      possible: ["always", "never", "always-single-line", "at-least-one-space"]
    });

    if (!validOptions) {
      return;
    }

    variableColonSpaceChecker({
      root: root,
      result: result,
      locationChecker: checker.after,
      checkedRuleName: ruleName,
      position: "after",
      expectation: expectation,
      context: context
    });
  };
}

function variableColonSpaceChecker(_ref) {
  var locationChecker = _ref.locationChecker,
      root = _ref.root,
      result = _ref.result,
      checkedRuleName = _ref.checkedRuleName,
      position = _ref.position,
      expectation = _ref.expectation,
      context = _ref.context;
  root.walkDecls(function (decl) {
    if (decl.prop === undefined || decl.prop[0] !== "$") {
      return;
    }

    if (context && context.fix) {
      if (expectation === "always-single-line" && !(0, _utils.isSingleLineString)(decl.value)) {
        return;
      }

      if (position === "before") {
        var replacement = expectation === "never" ? ":" : " :";
        decl.raws.between = decl.raws.between.replace(/\s*:/, replacement);
      } else if (position === "after") {
        var match = expectation === "at-least-one-space" ? /:(?! )/ : /:\s*/;

        var _replacement = expectation === "never" ? ":" : ": ";

        decl.raws.between = decl.raws.between.replace(match, _replacement);
      }

      return;
    } // Get the raw $var, and only that


    var endOfPropIndex = (0, _utils.declarationValueIndex)(decl) + decl.raw("between").length - 1; // `$var:`, `$var :`

    var propPlusColon = decl.toString().slice(0, endOfPropIndex);

    var _loop2 = function _loop2(i) {
      if (propPlusColon[i] !== ":") {
        return "continue";
      }

      locationChecker({
        source: propPlusColon,
        index: i,
        lineCheckStr: decl.value,
        err: function err(m) {
          _stylelint.utils.report({
            message: m,
            node: decl,
            index: i,
            result: result,
            ruleName: checkedRuleName
          });
        }
      });
      return "break";
    };

    _loop: for (var i = 0; i < propPlusColon.length; i++) {
      var _ret = _loop2(i);

      switch (_ret) {
        case "continue":
          continue;

        case "break":
          break _loop;
      }
    }
  });
}
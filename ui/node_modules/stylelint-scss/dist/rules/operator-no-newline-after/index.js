"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports["default"] = _default;
exports.messages = exports.ruleName = void 0;

var _stylelint = require("stylelint");

var _utils = require("../../utils");

var _operatorNoUnspaced = require("../operator-no-unspaced");

var ruleName = (0, _utils.namespace)("operator-no-newline-after");
exports.ruleName = ruleName;

var messages = _stylelint.utils.ruleMessages(ruleName, {
  rejected: function rejected(operator) {
    return "Unexpected newline after \"".concat(operator, "\"");
  }
});

exports.messages = messages;

/**
 * The checker function: whether there is a newline before THAT operator.
 */
function checkNewlineBefore(_ref) {
  var string = _ref.string,
      globalIndex = _ref.globalIndex,
      startIndex = _ref.startIndex,
      endIndex = _ref.endIndex,
      node = _ref.node,
      result = _ref.result;
  var symbol = string.substring(startIndex, endIndex + 1);
  var newLineBefore = false;
  var index = endIndex + 1;

  while (index && (0, _utils.isWhitespace)(string[index])) {
    if (string[index] === "\n") {
      newLineBefore = true;
      break;
    }

    index++;
  }

  if (newLineBefore) {
    _stylelint.utils.report({
      ruleName: ruleName,
      result: result,
      node: node,
      message: messages.rejected(symbol),
      index: endIndex + globalIndex
    });
  }
}

function _default(expectation) {
  return function (root, result) {
    var validOptions = _stylelint.utils.validateOptions(result, ruleName, {
      actual: expectation
    });

    if (!validOptions) {
      return;
    }

    (0, _utils.eachRoot)(root, checkRoot);

    function checkRoot(root) {
      (0, _operatorNoUnspaced.calculationOperatorSpaceChecker)({
        root: root,
        result: result,
        checker: checkNewlineBefore
      });
    }
  };
}
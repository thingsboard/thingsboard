"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports["default"] = rule;
exports.units = exports.messages = exports.ruleName = void 0;

var _postcssValueParser = _interopRequireDefault(require("postcss-value-parser"));

var _stylelint = require("stylelint");

var _utils = require("../../utils");

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { "default": obj }; }

var ruleName = (0, _utils.namespace)("dimension-no-non-numeric-values");
exports.ruleName = ruleName;

var messages = _stylelint.utils.ruleMessages(ruleName, {
  rejected: function rejected(unit) {
    return "Expected \"$value * 1".concat(unit, "\" instead of \"#{$value}").concat(unit, "\". Consider writing \"value\" in terms of ").concat(unit, " originally.");
  }
});

exports.messages = messages;
var units = [// Font-relative lengths:
// https://www.w3.org/TR/css-values-4/#font-relative-lengths
"em", "ex", "cap", "ch", "ic", "rem", "lh", "rlh", // Viewport-relative lengths:
// https://www.w3.org/TR/css-values-4/#viewport-relative-lengths
"vw", "vh", "vi", "vb", "vmin", "vmax", // Absolute lengths:
// https://www.w3.org/TR/css-values-4/#absolute-lengths
"cm", "mm", "Q", "in", "pc", "pt", "px", // Angle units:
// https://www.w3.org/TR/css-values-4/#angles
"deg", "grad", "rad", "turn", // Duration units:
// https://www.w3.org/TR/css-values-4/#time
"s", "ms", // Frequency units:
// https://www.w3.org/TR/css-values-4/#frequency
"Hz", "kHz", // Resolution units:
// https://www.w3.org/TR/css-values-4/#resolution
"dpi", "dpcm", "dppx", "x", // Flexible lengths:
// https://www.w3.org/TR/css-grid-1/#fr-unit
"fr"];
exports.units = units;

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
        // All words are non-quoted, while strings are quoted.
        // If quoted, it's probably a deliberate non-numeric dimension.
        if (node.type !== "word") {
          return;
        }

        if (!isInterpolated(node.value)) {
          return;
        }

        var regex = new RegExp("#{[$a-z_0-9 +-]*}(" + units.join("|") + ");?");
        var matchUnit = decl.value.match(regex);
        var unit = matchUnit[1];
        var offset = decl.value.indexOf(unit);

        _stylelint.utils.report({
          ruleName: ruleName,
          result: result,
          message: messages.rejected(unit),
          index: (0, _utils.declarationValueIndex)(decl) + offset,
          node: decl
        });
      });
    });
  };
}

function isInterpolated(value) {
  var _boolean = false; // ValueParser breaks up interpolation with math into multiple, fragmented
  // segments (#{$value, +, 2}px). The easiest way to detect this is to look for a fragmented
  // interpolated section.

  if (value.match(/^#{\$[a-z]*$/)) {
    return true;
  }

  units.forEach(function (unit) {
    var regex = new RegExp("^#{[$a-z_0-9 +-]*}" + unit + ";?$");

    if (value.match(regex)) {
      _boolean = true;
    }
  });
  return _boolean;
}
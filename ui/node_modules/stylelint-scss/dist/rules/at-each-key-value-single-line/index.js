"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports["default"] = _default;
exports.messages = exports.ruleName = void 0;

var _stylelint = require("stylelint");

var _utils = require("../../utils");

var ruleName = (0, _utils.namespace)("at-each-key-value-single-line");
exports.ruleName = ruleName;

var messages = _stylelint.utils.ruleMessages(ruleName, {
  expected: "Use @each $key, $value in $map syntax instead of $value: map-get($map, $key)"
});

exports.messages = messages;

function _default(primary) {
  return function (root, result) {
    var validOptions = _stylelint.utils.validateOptions(result, ruleName, {
      actual: primary
    });

    if (!validOptions) {
      return;
    }

    root.walkAtRules("each", function (rule) {
      var parts = separateEachParams(rule.params); // If loop is fetching both key + value, return

      if (parts[0].length === 2) {
        return;
      } // If didn't call map-keys, return.


      if (!didCallMapKeys(parts[1])) {
        return;
      } // Loop over decls inside of each statement and loop for variable assignments.


      rule.walkDecls(function (innerDecl) {
        // Check that this decl is a map-get call
        if (innerDecl.prop[0] !== "$") {
          return;
        }

        if (!didCallMapGet(innerDecl.value)) {
          return;
        } // Check map_name + key_name match.


        var map_get_parts = mapGetParameters(innerDecl.value); // Check map names match.

        if (map_get_parts[0] !== mapName(parts[1])) {
          return;
        } // Match key names match.


        if (map_get_parts[1] !== parts[0][0]) {
          return;
        }

        _stylelint.utils.report({
          message: messages.expected,
          node: rule,
          result: result,
          ruleName: ruleName
        });
      });
    });
  };
} // Takes in a param string from node.params
// Returns: [[key variable, value variable], map_decl] (all Strings)


function separateEachParams(paramString) {
  var parts = paramString.split("in");
  return [parts[0].split(",").map(function (s) {
    return s.trim();
  }), parts[1].trim()];
}

function didCallMapKeys(map_decl) {
  return map_decl.match(/map-keys\(.*\)/);
}

function didCallMapGet(map_decl) {
  return map_decl.match(/map-get\((.*),(.*)\)/);
} // Fetch the name of the map from a map-keys() call.


function mapName(map_decl) {
  if (didCallMapKeys(map_decl)) {
    return map_decl.match(/map-keys\((.*)\)/)[1];
  } else {
    return map_decl;
  }
} // Returns the parameters of a map-get call
// Returns [map variable, key_variable]


function mapGetParameters(mapGetDecl) {
  var parts = mapGetDecl.match(/map-get\((.*), ?(.*)\)/);
  return [parts[1], parts[2]];
}
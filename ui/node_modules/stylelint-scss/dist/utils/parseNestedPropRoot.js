"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports["default"] = parseNestedPropRoot;

/**
 * Attempts to parse a selector as if it"s a root for a group of nested props
 * E.g.: `margin: {`, `font: 10px/1.1 Arial {` ("{" excluded)
 */
function parseNestedPropRoot(propString) {
  var modesEntered = [{
    mode: "normal",
    character: null,
    isCalculationEnabled: true
  }];
  var result = {};
  var lastModeIndex = 0;
  var propName = "";

  for (var i = 0; i < propString.length; i++) {
    var character = propString[i];
    var prevCharacter = propString[i - 1]; // If entering/exiting a string

    if (character === '"' || character === "'") {
      if (modesEntered[lastModeIndex].isCalculationEnabled === true) {
        modesEntered.push({
          mode: "string",
          isCalculationEnabled: false,
          character: character
        });
        lastModeIndex++;
      } else if (modesEntered[lastModeIndex].mode === "string" && modesEntered[lastModeIndex].character === character && prevCharacter !== "\\") {
        modesEntered.pop();
        lastModeIndex--;
      }
    } // If entering/exiting interpolation


    if (character === "{") {
      modesEntered.push({
        mode: "interpolation",
        isCalculationEnabled: true
      });
      lastModeIndex++;
    } else if (character === "}") {
      modesEntered.pop();
      lastModeIndex--;
    } // Check for : outside fn call, string or interpolation. It must be at the
    // end of a string or have a whitespace between it and following value


    if (modesEntered[lastModeIndex].mode === "normal" && character === ":" && prevCharacter !== "\\") {
      var propValueStr = propString.substring(i + 1);

      if (propValueStr.length) {
        var propValue = {
          before: /^(\s*)/.exec(propValueStr)[1],
          value: propValueStr.trim()
        }; // It's a declaration if 1) there is a whitespace after :, or
        // 2) the value is a number with/without a unit (starts with a number
        // or a dot), or
        // 3) the value is a variable (starts with $), or
        // 4) the value a string, surprisingly

        if (propValue.before === "" && !/^[0-9.$'"]/.test(propValue.value)) {
          return null;
        } // +1 for the colon


        propValue.sourceIndex = propValue.before.length + i + 1;
        result.propValue = propValue;
      }

      result.propName = {
        after: /(\s*)$/.exec(propName)[1],
        value: propName.trim()
      };
      return result;
    }

    propName += character;
  }

  return null;
}
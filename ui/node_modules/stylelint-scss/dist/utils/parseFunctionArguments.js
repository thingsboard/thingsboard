"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.groupByKeyValue = groupByKeyValue;
exports.mapToKeyValue = mapToKeyValue;
exports.parseFunctionArguments = parseFunctionArguments;

var _postcssValueParser = _interopRequireDefault(require("postcss-value-parser"));

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { "default": obj }; }

function groupByKeyValue(nodes) {
  if (!nodes) {
    return [];
  }

  var groupIndex = 0;
  return nodes.reduce(function (acc, node, nodeIndex) {
    var isComma = node.type === "div" && node.value === ",";
    var skipTrailingComma = isComma && nodeIndex === nodes.length - 1;

    if (skipTrailingComma) {
      return acc;
    }

    if (isComma) {
      groupIndex++;
    }

    acc[groupIndex] = acc[groupIndex] || [];

    if (!isComma) {
      acc[groupIndex].push(node);
    }

    return acc;
  }, []);
}

function mapToKeyValue(nodes) {
  var keyVal = nodes.reduce(function (acc, curr, i) {
    if (acc.length === 1) {
      return acc;
    }

    var nextNode = nodes[i + 1];
    var isNextNodeColon = nextNode && nextNode.type === "div" && nextNode.value === ":";

    if (isNextNodeColon) {
      acc.push({
        key: _postcssValueParser["default"].stringify(nodes[i]),
        value: _postcssValueParser["default"].stringify(nodes.slice(2))
      });
      return acc;
    }

    acc.push({
      value: _postcssValueParser["default"].stringify(nodes)
    });
    return acc;
  }, []);
  return keyVal[0];
}

function parseFunctionArguments(value) {
  var parsed = (0, _postcssValueParser["default"])(value);

  if (!parsed.nodes[0] || parsed.nodes[0].type !== "function") {
    return [];
  }

  return parsed.nodes.map(function (node) {
    return groupByKeyValue(node.nodes).map(mapToKeyValue);
  })[0];
}
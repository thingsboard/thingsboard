"use strict";
/**
 * @fileoverview Really small utility functions that didn't deserve their own files
 */
Object.defineProperty(exports, "__esModule", { value: true });
const experimental_utils_1 = require("@typescript-eslint/experimental-utils");
/**
 * Check if the context file name is *.ts or *.tsx
 */
function isTypeScriptFile(fileName) {
    return /\.tsx?$/i.test(fileName || '');
}
exports.isTypeScriptFile = isTypeScriptFile;
/**
 * Check if the context file name is *.d.ts or *.d.tsx
 */
function isDefinitionFile(fileName) {
    return /\.d\.tsx?$/i.test(fileName || '');
}
exports.isDefinitionFile = isDefinitionFile;
/**
 * Upper cases the first character or the string
 */
function upperCaseFirst(str) {
    return str[0].toUpperCase() + str.slice(1);
}
exports.upperCaseFirst = upperCaseFirst;
/**
 * Gets a string name representation of the given PropertyName node
 */
function getNameFromPropertyName(propertyName) {
    if (propertyName.type === experimental_utils_1.AST_NODE_TYPES.Identifier) {
        return propertyName.name;
    }
    return `${propertyName.value}`;
}
exports.getNameFromPropertyName = getNameFromPropertyName;
function arraysAreEqual(a, b, eq) {
    return (a === b ||
        (a !== undefined &&
            b !== undefined &&
            a.length === b.length &&
            a.every((x, idx) => eq(x, b[idx]))));
}
exports.arraysAreEqual = arraysAreEqual;
/**
 * Gets a string name representation of the name of the given MethodDefinition
 * or ClassProperty node, with handling for computed property names.
 */
function getNameFromClassMember(methodDefinition, sourceCode) {
    if (keyCanBeReadAsPropertyName(methodDefinition.key)) {
        return getNameFromPropertyName(methodDefinition.key);
    }
    return sourceCode.text.slice(...methodDefinition.key.range);
}
exports.getNameFromClassMember = getNameFromClassMember;
/**
 * This covers both actual property names, as well as computed properties that are either
 * an identifier or a literal at the top level.
 */
function keyCanBeReadAsPropertyName(node) {
    return (node.type === experimental_utils_1.AST_NODE_TYPES.Literal ||
        node.type === experimental_utils_1.AST_NODE_TYPES.Identifier);
}
//# sourceMappingURL=misc.js.map
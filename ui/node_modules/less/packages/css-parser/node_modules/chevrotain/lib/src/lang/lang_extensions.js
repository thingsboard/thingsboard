"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var utils_1 = require("../utils/utils");
function classNameFromInstance(instance) {
    return functionName(instance.constructor);
}
exports.classNameFromInstance = classNameFromInstance;
var FUNC_NAME_REGEXP = /^\s*function\s*(\S*)\s*\(/;
var NAME = "name";
/* istanbul ignore next too many hacks for IE/old versions of node.js here*/
function functionName(func) {
    // Engines that support Function.prototype.name OR the nth (n>1) time after
    // the name has been computed in the following else block.
    var existingNameProp = func.name;
    if (existingNameProp) {
        return existingNameProp;
    }
    // hack for IE and engines that do not support Object.defineProperty on function.name (Node.js 0.10 && 0.12)
    var computedName = func.toString().match(FUNC_NAME_REGEXP)[1];
    return computedName;
}
exports.functionName = functionName;
/**
 * @returns {boolean} - has the property been successfully defined
 */
function defineNameProp(obj, nameValue) {
    var namePropDescriptor = Object.getOwnPropertyDescriptor(obj, NAME);
    /* istanbul ignore else -> will only run in old versions of node.js */
    if (utils_1.isUndefined(namePropDescriptor) || namePropDescriptor.configurable) {
        Object.defineProperty(obj, NAME, {
            enumerable: false,
            configurable: true,
            writable: false,
            value: nameValue
        });
        return true;
    }
    /* istanbul ignore next -> will only run in old versions of node.js */
    return false;
}
exports.defineNameProp = defineNameProp;
//# sourceMappingURL=lang_extensions.js.map
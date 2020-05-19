"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
const tsutils_1 = require("tsutils");
const typescript_1 = __importDefault(require("typescript"));
/**
 * @param type Type being checked by name.
 * @param allowedNames Symbol names checking on the type.
 * @returns Whether the type is, extends, or contains any of the allowed names.
 */
function containsTypeByName(type, allowAny, allowedNames) {
    if (isTypeFlagSet(type, typescript_1.default.TypeFlags.Any | typescript_1.default.TypeFlags.Unknown)) {
        return !allowAny;
    }
    if (tsutils_1.isTypeReference(type)) {
        type = type.target;
    }
    if (typeof type.symbol !== 'undefined' &&
        allowedNames.has(type.symbol.name)) {
        return true;
    }
    if (tsutils_1.isUnionOrIntersectionType(type)) {
        return type.types.some(t => containsTypeByName(t, allowAny, allowedNames));
    }
    const bases = type.getBaseTypes();
    return (typeof bases !== 'undefined' &&
        bases.some(t => containsTypeByName(t, allowAny, allowedNames)));
}
exports.containsTypeByName = containsTypeByName;
/**
 * Get the type name of a given type.
 * @param typeChecker The context sensitive TypeScript TypeChecker.
 * @param type The type to get the name of.
 */
function getTypeName(typeChecker, type) {
    // It handles `string` and string literal types as string.
    if ((type.flags & typescript_1.default.TypeFlags.StringLike) !== 0) {
        return 'string';
    }
    // If the type is a type parameter which extends primitive string types,
    // but it was not recognized as a string like. So check the constraint
    // type of the type parameter.
    if ((type.flags & typescript_1.default.TypeFlags.TypeParameter) !== 0) {
        // `type.getConstraint()` method doesn't return the constraint type of
        // the type parameter for some reason. So this gets the constraint type
        // via AST.
        const node = type.symbol.declarations[0];
        if (node.constraint != null) {
            return getTypeName(typeChecker, typeChecker.getTypeFromTypeNode(node.constraint));
        }
    }
    // If the type is a union and all types in the union are string like,
    // return `string`. For example:
    // - `"a" | "b"` is string.
    // - `string | string[]` is not string.
    if (type.isUnion() &&
        type.types
            .map(value => getTypeName(typeChecker, value))
            .every(t => t === 'string')) {
        return 'string';
    }
    // If the type is an intersection and a type in the intersection is string
    // like, return `string`. For example: `string & {__htmlEscaped: void}`
    if (type.isIntersection() &&
        type.types
            .map(value => getTypeName(typeChecker, value))
            .some(t => t === 'string')) {
        return 'string';
    }
    return typeChecker.typeToString(type);
}
exports.getTypeName = getTypeName;
/**
 * Resolves the given node's type. Will resolve to the type's generic constraint, if it has one.
 */
function getConstrainedTypeAtLocation(checker, node) {
    const nodeType = checker.getTypeAtLocation(node);
    const constrained = checker.getBaseConstraintOfType(nodeType);
    return constrained || nodeType;
}
exports.getConstrainedTypeAtLocation = getConstrainedTypeAtLocation;
/**
 * Checks if the given type is (or accepts) nullable
 * @param isReceiver true if the type is a receiving type (i.e. the type of a called function's parameter)
 */
function isNullableType(type, { isReceiver = false, allowUndefined = true, } = {}) {
    const flags = getTypeFlags(type);
    if (isReceiver && flags & (typescript_1.default.TypeFlags.Any | typescript_1.default.TypeFlags.Unknown)) {
        return true;
    }
    if (allowUndefined) {
        return (flags & (typescript_1.default.TypeFlags.Null | typescript_1.default.TypeFlags.Undefined)) !== 0;
    }
    else {
        return (flags & typescript_1.default.TypeFlags.Null) !== 0;
    }
}
exports.isNullableType = isNullableType;
/**
 * Gets the declaration for the given variable
 */
function getDeclaration(checker, node) {
    const symbol = checker.getSymbolAtLocation(node);
    if (!symbol) {
        return null;
    }
    const declarations = symbol.declarations;
    if (!declarations) {
        return null;
    }
    return declarations[0];
}
exports.getDeclaration = getDeclaration;
/**
 * Gets all of the type flags in a type, iterating through unions automatically
 */
function getTypeFlags(type) {
    let flags = 0;
    for (const t of tsutils_1.unionTypeParts(type)) {
        flags |= t.flags;
    }
    return flags;
}
exports.getTypeFlags = getTypeFlags;
/**
 * Checks if the given type is (or accepts) the given flags
 * @param isReceiver true if the type is a receiving type (i.e. the type of a called function's parameter)
 */
function isTypeFlagSet(type, flagsToCheck, isReceiver) {
    const flags = getTypeFlags(type);
    if (isReceiver && flags & (typescript_1.default.TypeFlags.Any | typescript_1.default.TypeFlags.Unknown)) {
        return true;
    }
    return (flags & flagsToCheck) !== 0;
}
exports.isTypeFlagSet = isTypeFlagSet;
/**
 * @returns Whether a type is an instance of the parent type, including for the parent's base types.
 */
exports.typeIsOrHasBaseType = (type, parentType) => {
    if (type.symbol === undefined || parentType.symbol === undefined) {
        return false;
    }
    const typeAndBaseTypes = [type];
    const ancestorTypes = type.getBaseTypes();
    if (ancestorTypes !== undefined) {
        typeAndBaseTypes.push(...ancestorTypes);
    }
    for (const baseType of typeAndBaseTypes) {
        if (baseType.symbol !== undefined &&
            baseType.symbol.name === parentType.symbol.name) {
            return true;
        }
    }
    return false;
};
//# sourceMappingURL=types.js.map
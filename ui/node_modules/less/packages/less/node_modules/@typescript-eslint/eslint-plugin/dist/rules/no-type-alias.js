"use strict";
var __importStar = (this && this.__importStar) || function (mod) {
    if (mod && mod.__esModule) return mod;
    var result = {};
    if (mod != null) for (var k in mod) if (Object.hasOwnProperty.call(mod, k)) result[k] = mod[k];
    result["default"] = mod;
    return result;
};
Object.defineProperty(exports, "__esModule", { value: true });
const experimental_utils_1 = require("@typescript-eslint/experimental-utils");
const util = __importStar(require("../util"));
exports.default = util.createRule({
    name: 'no-type-alias',
    meta: {
        type: 'suggestion',
        docs: {
            description: 'Disallow the use of type aliases',
            category: 'Stylistic Issues',
            recommended: false,
        },
        messages: {
            noTypeAlias: 'Type {{alias}} are not allowed.',
            noCompositionAlias: '{{typeName}} in {{compositionType}} types are not allowed.',
        },
        schema: [
            {
                type: 'object',
                properties: {
                    allowAliases: {
                        enum: [
                            'always',
                            'never',
                            'in-unions',
                            'in-intersections',
                            'in-unions-and-intersections',
                        ],
                    },
                    allowCallbacks: {
                        enum: ['always', 'never'],
                    },
                    allowLiterals: {
                        enum: [
                            'always',
                            'never',
                            'in-unions',
                            'in-intersections',
                            'in-unions-and-intersections',
                        ],
                    },
                    allowMappedTypes: {
                        enum: [
                            'always',
                            'never',
                            'in-unions',
                            'in-intersections',
                            'in-unions-and-intersections',
                        ],
                    },
                },
                additionalProperties: false,
            },
        ],
    },
    defaultOptions: [
        {
            allowAliases: 'never',
            allowCallbacks: 'never',
            allowLiterals: 'never',
            allowMappedTypes: 'never',
        },
    ],
    create(context, [{ allowAliases, allowCallbacks, allowLiterals, allowMappedTypes }]) {
        const unions = ['always', 'in-unions', 'in-unions-and-intersections'];
        const intersections = [
            'always',
            'in-intersections',
            'in-unions-and-intersections',
        ];
        const compositions = [
            'in-unions',
            'in-intersections',
            'in-unions-and-intersections',
        ];
        const aliasTypes = new Set([
            experimental_utils_1.AST_NODE_TYPES.TSArrayType,
            experimental_utils_1.AST_NODE_TYPES.TSTypeReference,
            experimental_utils_1.AST_NODE_TYPES.TSLiteralType,
            experimental_utils_1.AST_NODE_TYPES.TSTypeQuery,
            experimental_utils_1.AST_NODE_TYPES.TSIndexedAccessType,
        ]);
        /**
         * Determines if the composition type is supported by the allowed flags.
         * @param isTopLevel a flag indicating this is the top level node.
         * @param compositionType the composition type (either TSUnionType or TSIntersectionType)
         * @param allowed the currently allowed flags.
         */
        function isSupportedComposition(isTopLevel, compositionType, allowed) {
            return (compositions.indexOf(allowed) === -1 ||
                (!isTopLevel &&
                    ((compositionType === experimental_utils_1.AST_NODE_TYPES.TSUnionType &&
                        unions.indexOf(allowed) > -1) ||
                        (compositionType === experimental_utils_1.AST_NODE_TYPES.TSIntersectionType &&
                            intersections.indexOf(allowed) > -1))));
        }
        /**
         * Gets the message to be displayed based on the node type and whether the node is a top level declaration.
         * @param node the location
         * @param compositionType the type of composition this alias is part of (undefined if not
         *                                  part of a composition)
         * @param isRoot a flag indicating we are dealing with the top level declaration.
         * @param type the kind of type alias being validated.
         */
        function reportError(node, compositionType, isRoot, type) {
            if (isRoot) {
                return context.report({
                    node,
                    messageId: 'noTypeAlias',
                    data: {
                        alias: type.toLowerCase(),
                    },
                });
            }
            return context.report({
                node,
                messageId: 'noCompositionAlias',
                data: {
                    compositionType: compositionType === experimental_utils_1.AST_NODE_TYPES.TSUnionType
                        ? 'union'
                        : 'intersection',
                    typeName: type,
                },
            });
        }
        /**
         * Validates the node looking for aliases, callbacks and literals.
         * @param node the node to be validated.
         * @param compositionType the type of composition this alias is part of (null if not
         *                                  part of a composition)
         * @param isTopLevel a flag indicating this is the top level node.
         */
        function validateTypeAliases(type, isTopLevel = false) {
            if (type.node.type === experimental_utils_1.AST_NODE_TYPES.TSFunctionType) {
                // callback
                if (allowCallbacks === 'never') {
                    reportError(type.node, type.compositionType, isTopLevel, 'Callbacks');
                }
            }
            else if (type.node.type === experimental_utils_1.AST_NODE_TYPES.TSTypeLiteral) {
                // literal object type
                if (allowLiterals === 'never' ||
                    !isSupportedComposition(isTopLevel, type.compositionType, allowLiterals)) {
                    reportError(type.node, type.compositionType, isTopLevel, 'Literals');
                }
            }
            else if (type.node.type === experimental_utils_1.AST_NODE_TYPES.TSMappedType) {
                // mapped type
                if (allowMappedTypes === 'never' ||
                    !isSupportedComposition(isTopLevel, type.compositionType, allowMappedTypes)) {
                    reportError(type.node, type.compositionType, isTopLevel, 'Mapped types');
                }
            }
            else if (/Keyword$/.test(type.node.type) ||
                aliasTypes.has(type.node.type)) {
                // alias / keyword
                if (allowAliases === 'never' ||
                    !isSupportedComposition(isTopLevel, type.compositionType, allowAliases)) {
                    reportError(type.node, type.compositionType, isTopLevel, 'Aliases');
                }
            }
            else {
                // unhandled type - shouldn't happen
                reportError(type.node, type.compositionType, isTopLevel, 'Unhandled');
            }
        }
        /**
         * Flatten the given type into an array of its dependencies
         */
        function getTypes(node, compositionType = null) {
            if (node.type === experimental_utils_1.AST_NODE_TYPES.TSUnionType ||
                node.type === experimental_utils_1.AST_NODE_TYPES.TSIntersectionType) {
                return node.types.reduce((acc, type) => {
                    acc.push(...getTypes(type, node.type));
                    return acc;
                }, []);
            }
            if (node.type === experimental_utils_1.AST_NODE_TYPES.TSParenthesizedType) {
                return getTypes(node.typeAnnotation, compositionType);
            }
            return [{ node, compositionType }];
        }
        return {
            TSTypeAliasDeclaration(node) {
                const types = getTypes(node.typeAnnotation);
                if (types.length === 1) {
                    // is a top level type annotation
                    validateTypeAliases(types[0], true);
                }
                else {
                    // is a composition type
                    types.forEach(type => {
                        validateTypeAliases(type);
                    });
                }
            },
        };
    },
});
//# sourceMappingURL=no-type-alias.js.map
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
    name: 'no-explicit-any',
    meta: {
        type: 'suggestion',
        docs: {
            description: 'Disallow usage of the `any` type',
            category: 'Best Practices',
            recommended: 'warn',
        },
        fixable: 'code',
        messages: {
            unexpectedAny: 'Unexpected any. Specify a different type.',
        },
        schema: [
            {
                type: 'object',
                additionalProperties: false,
                properties: {
                    fixToUnknown: {
                        type: 'boolean',
                    },
                    ignoreRestArgs: {
                        type: 'boolean',
                    },
                },
            },
        ],
    },
    defaultOptions: [
        {
            fixToUnknown: false,
            ignoreRestArgs: false,
        },
    ],
    create(context, [{ ignoreRestArgs, fixToUnknown }]) {
        /**
         * Checks if the node is an arrow function, function declaration or function expression
         * @param node the node to be validated.
         * @returns true if the node is an arrow function, function declaration or function expression
         * @private
         */
        function isNodeValidFunction(node) {
            return [
                experimental_utils_1.AST_NODE_TYPES.ArrowFunctionExpression,
                experimental_utils_1.AST_NODE_TYPES.FunctionDeclaration,
                experimental_utils_1.AST_NODE_TYPES.FunctionExpression,
            ].includes(node.type);
        }
        /**
         * Checks if the node is a rest element child node of a function
         * @param node the node to be validated.
         * @returns true if the node is a rest element child node of a function
         * @private
         */
        function isNodeRestElementInFunction(node) {
            return (node.type === experimental_utils_1.AST_NODE_TYPES.RestElement &&
                typeof node.parent !== 'undefined' &&
                isNodeValidFunction(node.parent));
        }
        /**
         * Checks if the node is a TSTypeOperator node with a readonly operator
         * @param node the node to be validated.
         * @returns true if the node is a TSTypeOperator node with a readonly operator
         * @private
         */
        function isNodeReadonlyTSTypeOperator(node) {
            return (node.type === experimental_utils_1.AST_NODE_TYPES.TSTypeOperator &&
                node.operator === 'readonly');
        }
        /**
         * Checks if the node is a TSTypeReference node with an Array identifier
         * @param node the node to be validated.
         * @returns true if the node is a TSTypeReference node with an Array identifier
         * @private
         */
        function isNodeValidArrayTSTypeReference(node) {
            return (node.type === experimental_utils_1.AST_NODE_TYPES.TSTypeReference &&
                typeof node.typeName !== 'undefined' &&
                node.typeName.type === experimental_utils_1.AST_NODE_TYPES.Identifier &&
                ['Array', 'ReadonlyArray'].includes(node.typeName.name));
        }
        /**
         * Checks if the node is a valid TSTypeOperator or TSTypeReference node
         * @param node the node to be validated.
         * @returns true if the node is a valid TSTypeOperator or TSTypeReference node
         * @private
         */
        function isNodeValidTSType(node) {
            return (isNodeReadonlyTSTypeOperator(node) ||
                isNodeValidArrayTSTypeReference(node));
        }
        /**
         * Checks if the great grand-parent node is a RestElement node in a function
         * @param node the node to be validated.
         * @returns true if the great grand-parent node is a RestElement node in a function
         * @private
         */
        function isGreatGrandparentRestElement(node) {
            return (typeof node.parent !== 'undefined' &&
                typeof node.parent.parent !== 'undefined' &&
                typeof node.parent.parent.parent !== 'undefined' &&
                isNodeRestElementInFunction(node.parent.parent.parent));
        }
        /**
         * Checks if the great great grand-parent node is a valid RestElement node in a function
         * @param node the node to be validated.
         * @returns true if the great great grand-parent node is a valid RestElement node in a function
         * @private
         */
        function isGreatGreatGrandparentRestElement(node) {
            return (typeof node.parent !== 'undefined' &&
                typeof node.parent.parent !== 'undefined' &&
                isNodeValidTSType(node.parent.parent) &&
                typeof node.parent.parent.parent !== 'undefined' &&
                typeof node.parent.parent.parent.parent !== 'undefined' &&
                isNodeRestElementInFunction(node.parent.parent.parent.parent));
        }
        /**
         * Checks if the great grand-parent or the great great grand-parent node is a RestElement node
         * @param node the node to be validated.
         * @returns true if the great grand-parent or the great great grand-parent node is a RestElement node
         * @private
         */
        function isNodeDescendantOfRestElementInFunction(node) {
            return (isGreatGrandparentRestElement(node) ||
                isGreatGreatGrandparentRestElement(node));
        }
        return {
            TSAnyKeyword(node) {
                if (ignoreRestArgs && isNodeDescendantOfRestElementInFunction(node)) {
                    return;
                }
                let fix = null;
                if (fixToUnknown) {
                    fix = fixer => fixer.replaceText(node, 'unknown');
                }
                context.report({
                    node,
                    messageId: 'unexpectedAny',
                    fix,
                });
            },
        };
    },
});
//# sourceMappingURL=no-explicit-any.js.map
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
    name: 'no-object-literal-type-assertion',
    meta: {
        type: 'problem',
        docs: {
            description: 'Forbids an object literal to appear in a type assertion expression',
            category: 'Stylistic Issues',
            recommended: 'error',
        },
        messages: {
            unexpectedTypeAssertion: 'Type assertion on object literals is forbidden, use a type annotation instead.',
        },
        schema: [
            {
                type: 'object',
                additionalProperties: false,
                properties: {
                    allowAsParameter: {
                        type: 'boolean',
                    },
                },
            },
        ],
    },
    defaultOptions: [
        {
            allowAsParameter: false,
        },
    ],
    create(context, [{ allowAsParameter }]) {
        /**
         * Check whatever node should be reported
         * @param node the node to be evaluated.
         */
        function checkType(node) {
            switch (node.type) {
                case experimental_utils_1.AST_NODE_TYPES.TSAnyKeyword:
                case experimental_utils_1.AST_NODE_TYPES.TSUnknownKeyword:
                    return false;
                case experimental_utils_1.AST_NODE_TYPES.TSTypeReference:
                    // Ignore `as const` and `<const>` (#166)
                    return (node.typeName.type === experimental_utils_1.AST_NODE_TYPES.Identifier &&
                        node.typeName.name !== 'const');
                default:
                    return true;
            }
        }
        return {
            'TSTypeAssertion, TSAsExpression'(node) {
                if (allowAsParameter &&
                    node.parent &&
                    (node.parent.type === experimental_utils_1.AST_NODE_TYPES.NewExpression ||
                        node.parent.type === experimental_utils_1.AST_NODE_TYPES.CallExpression)) {
                    return;
                }
                if (checkType(node.typeAnnotation) &&
                    node.expression.type === experimental_utils_1.AST_NODE_TYPES.ObjectExpression) {
                    context.report({
                        node,
                        messageId: 'unexpectedTypeAssertion',
                    });
                }
            },
        };
    },
});
//# sourceMappingURL=no-object-literal-type-assertion.js.map
"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
var __importStar = (this && this.__importStar) || function (mod) {
    if (mod && mod.__esModule) return mod;
    var result = {};
    if (mod != null) for (var k in mod) if (Object.hasOwnProperty.call(mod, k)) result[k] = mod[k];
    result["default"] = mod;
    return result;
};
Object.defineProperty(exports, "__esModule", { value: true });
const experimental_utils_1 = require("@typescript-eslint/experimental-utils");
const no_empty_function_1 = __importDefault(require("eslint/lib/rules/no-empty-function"));
const util = __importStar(require("../util"));
exports.default = util.createRule({
    name: 'no-empty-function',
    meta: {
        type: 'suggestion',
        docs: {
            description: 'Disallow empty functions',
            category: 'Best Practices',
            recommended: false,
        },
        schema: no_empty_function_1.default.meta.schema,
        messages: no_empty_function_1.default.meta.messages,
    },
    defaultOptions: [
        {
            allow: [],
        },
    ],
    create(context) {
        const rules = no_empty_function_1.default.create(context);
        /**
         * Checks if the node is a constructor
         * @param node the node to ve validated
         * @returns true if the node is a constructor
         * @private
         */
        function isConstructor(node) {
            return !!(node.parent &&
                node.parent.type === 'MethodDefinition' &&
                node.parent.kind === 'constructor');
        }
        /**
         * Check if the method body is empty
         * @param node the node to be validated
         * @returns true if the body is empty
         * @private
         */
        function isBodyEmpty(node) {
            return !node.body || node.body.body.length === 0;
        }
        /**
         * Check if method has parameter properties
         * @param node the node to be validated
         * @returns true if the body has parameter properties
         * @private
         */
        function hasParameterProperties(node) {
            return (node.params &&
                node.params.some(param => param.type === experimental_utils_1.AST_NODE_TYPES.TSParameterProperty));
        }
        /**
         * Checks if the method is a concise constructor (no function body, but has parameter properties)
         * @param node the node to be validated
         * @returns true if the method is a concise constructor
         * @private
         */
        function isConciseConstructor(node) {
            // Check TypeScript specific nodes
            return (isConstructor(node) && isBodyEmpty(node) && hasParameterProperties(node));
        }
        return {
            FunctionDeclaration(node) {
                if (!isConciseConstructor(node)) {
                    rules.FunctionDeclaration(node);
                }
            },
            FunctionExpression(node) {
                if (!isConciseConstructor(node)) {
                    rules.FunctionExpression(node);
                }
            },
        };
    },
});
//# sourceMappingURL=no-empty-function.js.map
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
const no_magic_numbers_1 = __importDefault(require("eslint/lib/rules/no-magic-numbers"));
const util = __importStar(require("../util"));
const baseRuleSchema = no_magic_numbers_1.default.meta.schema[0];
exports.default = util.createRule({
    name: 'no-magic-numbers',
    meta: {
        type: 'suggestion',
        docs: {
            description: 'Disallows magic numbers',
            category: 'Best Practices',
            recommended: false,
        },
        // Extend base schema with additional property to ignore TS numeric literal types
        schema: [
            Object.assign({}, baseRuleSchema, { properties: Object.assign({}, baseRuleSchema.properties, { ignoreNumericLiteralTypes: {
                        type: 'boolean',
                    }, ignoreEnums: {
                        type: 'boolean',
                    } }) }),
        ],
        messages: no_magic_numbers_1.default.meta.messages,
    },
    defaultOptions: [
        {
            ignore: [],
            ignoreArrayIndexes: false,
            enforceConst: false,
            detectObjects: false,
            ignoreNumericLiteralTypes: false,
            ignoreEnums: false,
        },
    ],
    create(context, [options]) {
        const rules = no_magic_numbers_1.default.create(context);
        /**
         * Returns whether the node is number literal
         * @param node the node literal being evaluated
         * @returns true if the node is a number literal
         */
        function isNumber(node) {
            return typeof node.value === 'number';
        }
        /**
         * Checks if the node grandparent is a Typescript type alias declaration
         * @param node the node to be validated.
         * @returns true if the node grandparent is a Typescript type alias declaration
         * @private
         */
        function isGrandparentTSTypeAliasDeclaration(node) {
            return node.parent && node.parent.parent
                ? node.parent.parent.type === experimental_utils_1.AST_NODE_TYPES.TSTypeAliasDeclaration
                : false;
        }
        /**
         * Checks if the node grandparent is a Typescript union type and its parent is a type alias declaration
         * @param node the node to be validated.
         * @returns true if the node grandparent is a Typescript untion type and its parent is a type alias declaration
         * @private
         */
        function isGrandparentTSUnionType(node) {
            if (node.parent &&
                node.parent.parent &&
                node.parent.parent.type === experimental_utils_1.AST_NODE_TYPES.TSUnionType) {
                return isGrandparentTSTypeAliasDeclaration(node.parent);
            }
            return false;
        }
        /**
         * Checks if the node parent is a Typescript enum member
         * @param node the node to be validated.
         * @returns true if the node parent is a Typescript enum member
         * @private
         */
        function isParentTSEnumDeclaration(node) {
            return (typeof node.parent !== 'undefined' &&
                node.parent.type === experimental_utils_1.AST_NODE_TYPES.TSEnumMember);
        }
        /**
         * Checks if the node parent is a Typescript literal type
         * @param node the node to be validated.
         * @returns true if the node parent is a Typescript literal type
         * @private
         */
        function isParentTSLiteralType(node) {
            return node.parent
                ? node.parent.type === experimental_utils_1.AST_NODE_TYPES.TSLiteralType
                : false;
        }
        /**
         * Checks if the node is a valid TypeScript numeric literal type.
         * @param node the node to be validated.
         * @returns true if the node is a TypeScript numeric literal type.
         * @private
         */
        function isTSNumericLiteralType(node) {
            // For negative numbers, update the parent node
            if (node.parent &&
                node.parent.type === experimental_utils_1.AST_NODE_TYPES.UnaryExpression &&
                node.parent.operator === '-') {
                node = node.parent;
            }
            // If the parent node is not a TSLiteralType, early return
            if (!isParentTSLiteralType(node)) {
                return false;
            }
            // If the grandparent is a TSTypeAliasDeclaration, ignore
            if (isGrandparentTSTypeAliasDeclaration(node)) {
                return true;
            }
            // If the grandparent is a TSUnionType and it's parent is a TSTypeAliasDeclaration, ignore
            if (isGrandparentTSUnionType(node)) {
                return true;
            }
            return false;
        }
        return {
            Literal(node) {
                // Check if the node is a TypeScript enum declaration
                if (options.ignoreEnums && isParentTSEnumDeclaration(node)) {
                    return;
                }
                // Check TypeScript specific nodes for Numeric Literal
                if (options.ignoreNumericLiteralTypes &&
                    isNumber(node) &&
                    isTSNumericLiteralType(node)) {
                    return;
                }
                // Let the base rule deal with the rest
                rules.Literal(node);
            },
        };
    },
});
//# sourceMappingURL=no-magic-numbers.js.map
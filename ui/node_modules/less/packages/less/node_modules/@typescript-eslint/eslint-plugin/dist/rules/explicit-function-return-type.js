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
    name: 'explicit-function-return-type',
    meta: {
        type: 'problem',
        docs: {
            description: 'Require explicit return types on functions and class methods',
            category: 'Stylistic Issues',
            recommended: 'warn',
        },
        messages: {
            missingReturnType: 'Missing return type on function.',
        },
        schema: [
            {
                type: 'object',
                properties: {
                    allowExpressions: {
                        type: 'boolean',
                    },
                    allowTypedFunctionExpressions: {
                        type: 'boolean',
                    },
                    allowHigherOrderFunctions: {
                        type: 'boolean',
                    },
                },
                additionalProperties: false,
            },
        ],
    },
    defaultOptions: [
        {
            allowExpressions: false,
            allowTypedFunctionExpressions: false,
            allowHigherOrderFunctions: false,
        },
    ],
    create(context, [options]) {
        /**
         * Checks if a node is a constructor.
         * @param node The node to check
         */
        function isConstructor(node) {
            return (!!node &&
                node.type === experimental_utils_1.AST_NODE_TYPES.MethodDefinition &&
                node.kind === 'constructor');
        }
        /**
         * Checks if a node is a setter.
         */
        function isSetter(node) {
            return (!!node &&
                (node.type === experimental_utils_1.AST_NODE_TYPES.MethodDefinition ||
                    node.type === experimental_utils_1.AST_NODE_TYPES.Property) &&
                node.kind === 'set');
        }
        /**
         * Checks if a node is a variable declarator with a type annotation.
         * `const x: Foo = ...`
         */
        function isVariableDeclaratorWithTypeAnnotation(node) {
            return (node.type === experimental_utils_1.AST_NODE_TYPES.VariableDeclarator &&
                !!node.id.typeAnnotation);
        }
        /**
         * Checks if a node is a class property with a type annotation.
         * `public x: Foo = ...`
         */
        function isClassPropertyWithTypeAnnotation(node) {
            return (node.type === experimental_utils_1.AST_NODE_TYPES.ClassProperty && !!node.typeAnnotation);
        }
        /**
         * Checks if a node is a type cast
         * `(() => {}) as Foo`
         * `<Foo>(() => {})`
         */
        function isTypeCast(node) {
            return (node.type === experimental_utils_1.AST_NODE_TYPES.TSAsExpression ||
                node.type === experimental_utils_1.AST_NODE_TYPES.TSTypeAssertion);
        }
        /**
         * Checks if a node belongs to:
         * `const x: Foo = { prop: () => {} }`
         * `const x = { prop: () => {} } as Foo`
         * `const x = <Foo>{ prop: () => {} }`
         */
        function isPropertyOfObjectWithType(parent) {
            if (!parent || parent.type !== experimental_utils_1.AST_NODE_TYPES.Property) {
                return false;
            }
            parent = parent.parent; // this shouldn't happen, checking just in case
            /* istanbul ignore if */ if (!parent ||
                parent.type !== experimental_utils_1.AST_NODE_TYPES.ObjectExpression) {
                return false;
            }
            parent = parent.parent; // this shouldn't happen, checking just in case
            /* istanbul ignore if */ if (!parent) {
                return false;
            }
            return (isTypeCast(parent) ||
                isClassPropertyWithTypeAnnotation(parent) ||
                isVariableDeclaratorWithTypeAnnotation(parent));
        }
        /**
         * Checks if a function belongs to:
         * `() => () => ...`
         * `() => function () { ... }`
         * `() => { return () => ... }`
         * `() => { return function () { ... } }`
         * `function fn() { return () => ... }`
         * `function fn() { return function() { ... } }`
         */
        function doesImmediatelyReturnFunctionExpression({ body, }) {
            // Should always have a body; really checking just in case
            /* istanbul ignore if */ if (!body) {
                return false;
            }
            // Check if body is a block with a single statement
            if (body.type === experimental_utils_1.AST_NODE_TYPES.BlockStatement &&
                body.body.length === 1) {
                const [statement] = body.body;
                // Check if that statement is a return statement with an argument
                if (statement.type === experimental_utils_1.AST_NODE_TYPES.ReturnStatement &&
                    !!statement.argument) {
                    // If so, check that returned argument as body
                    body = statement.argument;
                }
            }
            // Check if the body being returned is a function expression
            return (body.type === experimental_utils_1.AST_NODE_TYPES.ArrowFunctionExpression ||
                body.type === experimental_utils_1.AST_NODE_TYPES.FunctionExpression);
        }
        /**
         * Checks if a node belongs to:
         * `foo(() => 1)`
         */
        function isFunctionArgument(parent, child) {
            return (parent.type === experimental_utils_1.AST_NODE_TYPES.CallExpression &&
                // make sure this isn't an IIFE
                parent.callee !== child);
        }
        /**
         * Checks if a function declaration/expression has a return type.
         */
        function checkFunctionReturnType(node) {
            if (options.allowHigherOrderFunctions &&
                doesImmediatelyReturnFunctionExpression(node)) {
                return;
            }
            if (node.returnType ||
                isConstructor(node.parent) ||
                isSetter(node.parent)) {
                return;
            }
            if (util.isTypeScriptFile(context.getFilename())) {
                context.report({
                    node,
                    messageId: 'missingReturnType',
                });
            }
        }
        /**
         * Checks if a function declaration/expression has a return type.
         */
        function checkFunctionExpressionReturnType(node) {
            // Should always have a parent; checking just in case
            /* istanbul ignore else */ if (node.parent) {
                if (options.allowTypedFunctionExpressions) {
                    if (isTypeCast(node.parent) ||
                        isVariableDeclaratorWithTypeAnnotation(node.parent) ||
                        isClassPropertyWithTypeAnnotation(node.parent) ||
                        isPropertyOfObjectWithType(node.parent) ||
                        isFunctionArgument(node.parent, node)) {
                        return;
                    }
                }
                if (options.allowExpressions &&
                    node.parent.type !== experimental_utils_1.AST_NODE_TYPES.VariableDeclarator &&
                    node.parent.type !== experimental_utils_1.AST_NODE_TYPES.MethodDefinition) {
                    return;
                }
            }
            checkFunctionReturnType(node);
        }
        return {
            ArrowFunctionExpression: checkFunctionExpressionReturnType,
            FunctionDeclaration: checkFunctionReturnType,
            FunctionExpression: checkFunctionExpressionReturnType,
        };
    },
});
//# sourceMappingURL=explicit-function-return-type.js.map
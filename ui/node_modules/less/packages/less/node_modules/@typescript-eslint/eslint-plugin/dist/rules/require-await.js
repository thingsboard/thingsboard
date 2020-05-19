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
const require_await_1 = __importDefault(require("eslint/lib/rules/require-await"));
const tsutils = __importStar(require("tsutils"));
const util = __importStar(require("../util"));
exports.default = util.createRule({
    name: 'require-await',
    meta: {
        type: 'suggestion',
        docs: {
            description: 'Disallow async functions which have no `await` expression',
            category: 'Best Practices',
            recommended: false,
        },
        schema: require_await_1.default.meta.schema,
        messages: require_await_1.default.meta.messages,
    },
    defaultOptions: [],
    create(context) {
        const rules = require_await_1.default.create(context);
        const parserServices = util.getParserServices(context);
        const checker = parserServices.program.getTypeChecker();
        let scopeInfo = null;
        /**
         * Push the scope info object to the stack.
         *
         * @returns {void}
         */
        function enterFunction(node) {
            scopeInfo = {
                upper: scopeInfo,
                returnsPromise: false,
            };
            switch (node.type) {
                case experimental_utils_1.AST_NODE_TYPES.FunctionDeclaration:
                    rules.FunctionDeclaration(node);
                    break;
                case experimental_utils_1.AST_NODE_TYPES.FunctionExpression:
                    rules.FunctionExpression(node);
                    break;
                case experimental_utils_1.AST_NODE_TYPES.ArrowFunctionExpression:
                    rules.ArrowFunctionExpression(node);
                    break;
            }
        }
        /**
         * Pop the top scope info object from the stack.
         * Passes through to the base rule if the function doesn't return a promise
         *
         * @param {ASTNode} node - The node exiting
         * @returns {void}
         */
        function exitFunction(node) {
            if (scopeInfo) {
                if (!scopeInfo.returnsPromise) {
                    switch (node.type) {
                        case experimental_utils_1.AST_NODE_TYPES.FunctionDeclaration:
                            rules['FunctionDeclaration:exit'](node);
                            break;
                        case experimental_utils_1.AST_NODE_TYPES.FunctionExpression:
                            rules['FunctionExpression:exit'](node);
                            break;
                        case experimental_utils_1.AST_NODE_TYPES.ArrowFunctionExpression:
                            rules['ArrowFunctionExpression:exit'](node);
                            break;
                    }
                }
                scopeInfo = scopeInfo.upper;
            }
        }
        return {
            'FunctionDeclaration[async = true]': enterFunction,
            'FunctionExpression[async = true]': enterFunction,
            'ArrowFunctionExpression[async = true]': enterFunction,
            'FunctionDeclaration[async = true]:exit': exitFunction,
            'FunctionExpression[async = true]:exit': exitFunction,
            'ArrowFunctionExpression[async = true]:exit': exitFunction,
            ReturnStatement(node) {
                if (!scopeInfo) {
                    return;
                }
                const { expression } = parserServices.esTreeNodeToTSNodeMap.get(node);
                if (!expression) {
                    return;
                }
                const type = checker.getTypeAtLocation(expression);
                if (tsutils.isThenableType(checker, expression, type)) {
                    scopeInfo.returnsPromise = true;
                }
            },
            AwaitExpression: rules.AwaitExpression,
            ForOfStatement: rules.ForOfStatement,
        };
    },
});
//# sourceMappingURL=require-await.js.map
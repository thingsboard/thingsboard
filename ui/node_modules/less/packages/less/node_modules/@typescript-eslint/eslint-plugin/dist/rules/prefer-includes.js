"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
const eslint_utils_1 = require("eslint-utils");
const regexpp_1 = require("regexpp");
const typescript_1 = __importDefault(require("typescript"));
const util_1 = require("../util");
exports.default = util_1.createRule({
    name: 'prefer-includes',
    defaultOptions: [],
    meta: {
        type: 'suggestion',
        docs: {
            description: 'Enforce `includes` method over `indexOf` method',
            category: 'Best Practices',
            recommended: false,
        },
        fixable: 'code',
        messages: {
            preferIncludes: "Use 'includes()' method instead.",
            preferStringIncludes: 'Use `String#includes()` method with a string instead.',
        },
        schema: [],
    },
    create(context) {
        const globalScope = context.getScope();
        const services = util_1.getParserServices(context);
        const types = services.program.getTypeChecker();
        function isNumber(node, value) {
            const evaluated = eslint_utils_1.getStaticValue(node, globalScope);
            return evaluated !== null && evaluated.value === value;
        }
        function isPositiveCheck(node) {
            switch (node.operator) {
                case '!==':
                case '!=':
                case '>':
                    return isNumber(node.right, -1);
                case '>=':
                    return isNumber(node.right, 0);
                default:
                    return false;
            }
        }
        function isNegativeCheck(node) {
            switch (node.operator) {
                case '===':
                case '==':
                case '<=':
                    return isNumber(node.right, -1);
                case '<':
                    return isNumber(node.right, 0);
                default:
                    return false;
            }
        }
        function hasSameParameters(nodeA, nodeB) {
            if (!typescript_1.default.isFunctionLike(nodeA) || !typescript_1.default.isFunctionLike(nodeB)) {
                return false;
            }
            const paramsA = nodeA.parameters;
            const paramsB = nodeB.parameters;
            if (paramsA.length !== paramsB.length) {
                return false;
            }
            for (let i = 0; i < paramsA.length; ++i) {
                const paramA = paramsA[i];
                const paramB = paramsB[i];
                // Check name, type, and question token once.
                if (paramA.getText() !== paramB.getText()) {
                    return false;
                }
            }
            return true;
        }
        /**
         * Parse a given node if it's a `RegExp` instance.
         * @param node The node to parse.
         */
        function parseRegExp(node) {
            const evaluated = eslint_utils_1.getStaticValue(node, globalScope);
            if (evaluated == null || !(evaluated.value instanceof RegExp)) {
                return null;
            }
            const { pattern, flags } = regexpp_1.parseRegExpLiteral(evaluated.value);
            if (pattern.alternatives.length !== 1 ||
                flags.ignoreCase ||
                flags.global) {
                return null;
            }
            // Check if it can determine a unique string.
            const chars = pattern.alternatives[0].elements;
            if (!chars.every(c => c.type === 'Character')) {
                return null;
            }
            // To string.
            return String.fromCodePoint(...chars.map(c => c.value));
        }
        return {
            "BinaryExpression > CallExpression.left > MemberExpression.callee[property.name='indexOf'][computed=false]"(node) {
                // Check if the comparison is equivalent to `includes()`.
                const callNode = node.parent;
                const compareNode = callNode.parent;
                const negative = isNegativeCheck(compareNode);
                if (!negative && !isPositiveCheck(compareNode)) {
                    return;
                }
                // Get the symbol of `indexOf` method.
                const tsNode = services.esTreeNodeToTSNodeMap.get(node.property);
                const indexofMethodSymbol = types.getSymbolAtLocation(tsNode);
                if (indexofMethodSymbol == null ||
                    indexofMethodSymbol.declarations.length === 0) {
                    return;
                }
                // Check if every declaration of `indexOf` method has `includes` method
                // and the two methods have the same parameters.
                for (const instanceofMethodDecl of indexofMethodSymbol.declarations) {
                    const typeDecl = instanceofMethodDecl.parent;
                    const type = types.getTypeAtLocation(typeDecl);
                    const includesMethodSymbol = type.getProperty('includes');
                    if (includesMethodSymbol == null ||
                        !includesMethodSymbol.declarations.some(includesMethodDecl => hasSameParameters(includesMethodDecl, instanceofMethodDecl))) {
                        return;
                    }
                }
                // Report it.
                context.report({
                    node: compareNode,
                    messageId: 'preferIncludes',
                    *fix(fixer) {
                        if (negative) {
                            yield fixer.insertTextBefore(callNode, '!');
                        }
                        yield fixer.replaceText(node.property, 'includes');
                        yield fixer.removeRange([callNode.range[1], compareNode.range[1]]);
                    },
                });
            },
            // /bar/.test(foo)
            'CallExpression > MemberExpression.callee[property.name="test"][computed=false]'(node) {
                const callNode = node.parent;
                const text = callNode.arguments.length === 1 ? parseRegExp(node.object) : null;
                if (text == null) {
                    return;
                }
                context.report({
                    node: callNode,
                    messageId: 'preferStringIncludes',
                    *fix(fixer) {
                        const argNode = callNode.arguments[0];
                        const needsParen = argNode.type !== 'Literal' &&
                            argNode.type !== 'TemplateLiteral' &&
                            argNode.type !== 'Identifier' &&
                            argNode.type !== 'MemberExpression' &&
                            argNode.type !== 'CallExpression';
                        yield fixer.removeRange([callNode.range[0], argNode.range[0]]);
                        if (needsParen) {
                            yield fixer.insertTextBefore(argNode, '(');
                            yield fixer.insertTextAfter(argNode, ')');
                        }
                        yield fixer.insertTextAfter(argNode, `.includes(${JSON.stringify(text)}`);
                    },
                });
            },
        };
    },
});
//# sourceMappingURL=prefer-includes.js.map
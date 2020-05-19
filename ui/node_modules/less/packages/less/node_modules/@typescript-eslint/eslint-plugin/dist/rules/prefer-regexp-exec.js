"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const util_1 = require("../util");
const eslint_utils_1 = require("eslint-utils");
exports.default = util_1.createRule({
    name: 'prefer-regexp-exec',
    defaultOptions: [],
    meta: {
        type: 'suggestion',
        docs: {
            description: 'Prefer RegExp#exec() over String#match() if no global flag is provided',
            category: 'Best Practices',
            recommended: false,
        },
        messages: {
            regExpExecOverStringMatch: 'Use the `RegExp#exec()` method instead.',
        },
        schema: [],
    },
    create(context) {
        const globalScope = context.getScope();
        const service = util_1.getParserServices(context);
        const typeChecker = service.program.getTypeChecker();
        /**
         * Check if a given node is a string.
         * @param node The node to check.
         */
        function isStringType(node) {
            const objectType = typeChecker.getTypeAtLocation(service.esTreeNodeToTSNodeMap.get(node));
            return util_1.getTypeName(typeChecker, objectType) === 'string';
        }
        return {
            "CallExpression[arguments.length=1] > MemberExpression.callee[property.name='match'][computed=false]"(node) {
                const callNode = node.parent;
                const arg = callNode.arguments[0];
                const evaluated = eslint_utils_1.getStaticValue(arg, globalScope);
                // Don't report regular expressions with global flag.
                if (evaluated &&
                    evaluated.value instanceof RegExp &&
                    evaluated.value.flags.includes('g')) {
                    return;
                }
                if (isStringType(node.object)) {
                    context.report({
                        node: callNode,
                        messageId: 'regExpExecOverStringMatch',
                    });
                    return;
                }
            },
        };
    },
});
//# sourceMappingURL=prefer-regexp-exec.js.map
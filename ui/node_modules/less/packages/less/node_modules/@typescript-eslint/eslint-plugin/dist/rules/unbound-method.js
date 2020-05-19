"use strict";
var __importStar = (this && this.__importStar) || function (mod) {
    if (mod && mod.__esModule) return mod;
    var result = {};
    if (mod != null) for (var k in mod) if (Object.hasOwnProperty.call(mod, k)) result[k] = mod[k];
    result["default"] = mod;
    return result;
};
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
const experimental_utils_1 = require("@typescript-eslint/experimental-utils");
const tsutils = __importStar(require("tsutils"));
const typescript_1 = __importDefault(require("typescript"));
const util = __importStar(require("../util"));
exports.default = util.createRule({
    name: 'unbound-method',
    meta: {
        docs: {
            category: 'Best Practices',
            description: 'Enforces unbound methods are called with their expected scope',
            recommended: false,
        },
        messages: {
            unbound: 'Avoid referencing unbound methods which may cause unintentional scoping of `this`.',
        },
        schema: [
            {
                type: 'object',
                properties: {
                    ignoreStatic: {
                        type: 'boolean',
                    },
                },
                additionalProperties: false,
            },
        ],
        type: 'problem',
    },
    defaultOptions: [
        {
            ignoreStatic: false,
        },
    ],
    create(context, [{ ignoreStatic }]) {
        const parserServices = util.getParserServices(context);
        const checker = parserServices.program.getTypeChecker();
        return {
            [experimental_utils_1.AST_NODE_TYPES.MemberExpression](node) {
                if (isSafeUse(node)) {
                    return;
                }
                const originalNode = parserServices.esTreeNodeToTSNodeMap.get(node);
                const symbol = checker.getSymbolAtLocation(originalNode);
                if (symbol && isDangerousMethod(symbol, ignoreStatic)) {
                    context.report({
                        messageId: 'unbound',
                        node,
                    });
                }
            },
        };
    },
});
function isDangerousMethod(symbol, ignoreStatic) {
    const { valueDeclaration } = symbol;
    if (!valueDeclaration) {
        // working around https://github.com/microsoft/TypeScript/issues/31294
        return false;
    }
    switch (valueDeclaration.kind) {
        case typescript_1.default.SyntaxKind.MethodDeclaration:
        case typescript_1.default.SyntaxKind.MethodSignature:
            return !(ignoreStatic &&
                tsutils.hasModifier(valueDeclaration.modifiers, typescript_1.default.SyntaxKind.StaticKeyword));
    }
    return false;
}
function isSafeUse(node) {
    const parent = node.parent;
    switch (parent.type) {
        case experimental_utils_1.AST_NODE_TYPES.IfStatement:
        case experimental_utils_1.AST_NODE_TYPES.ForStatement:
        case experimental_utils_1.AST_NODE_TYPES.MemberExpression:
        case experimental_utils_1.AST_NODE_TYPES.SwitchStatement:
        case experimental_utils_1.AST_NODE_TYPES.UpdateExpression:
        case experimental_utils_1.AST_NODE_TYPES.WhileStatement:
            return true;
        case experimental_utils_1.AST_NODE_TYPES.CallExpression:
            return parent.callee === node;
        case experimental_utils_1.AST_NODE_TYPES.ConditionalExpression:
            return parent.test === node;
        case experimental_utils_1.AST_NODE_TYPES.LogicalExpression:
            return parent.operator !== '||';
        case experimental_utils_1.AST_NODE_TYPES.TaggedTemplateExpression:
            return parent.tag === node;
        case experimental_utils_1.AST_NODE_TYPES.TSNonNullExpression:
        case experimental_utils_1.AST_NODE_TYPES.TSAsExpression:
        case experimental_utils_1.AST_NODE_TYPES.TSTypeAssertion:
            return isSafeUse(parent);
    }
    return false;
}
//# sourceMappingURL=unbound-method.js.map
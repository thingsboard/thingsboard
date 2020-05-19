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
const tsutils = __importStar(require("tsutils"));
const typescript_1 = __importDefault(require("typescript"));
const util = __importStar(require("../util"));
const util_1 = require("../util");
const experimental_utils_1 = require("@typescript-eslint/experimental-utils");
const functionScopeBoundaries = [
    'ArrowFunctionExpression',
    'FunctionDeclaration',
    'FunctionExpression',
    'GetAccessor',
    'MethodDefinition',
    'SetAccessor',
].join(', ');
exports.default = util.createRule({
    name: 'prefer-readonly',
    meta: {
        docs: {
            description: "Requires that private members are marked as `readonly` if they're never modified outside of the constructor",
            category: 'Best Practices',
            recommended: false,
        },
        fixable: 'code',
        messages: {
            preferReadonly: "Member '{{name}}' is never reassigned; mark it as `readonly`.",
        },
        schema: [
            {
                allowAdditionalProperties: false,
                properties: {
                    onlyInlineLambdas: {
                        type: 'boolean',
                    },
                },
                type: 'object',
            },
        ],
        type: 'suggestion',
    },
    defaultOptions: [{ onlyInlineLambdas: false }],
    create(context, [{ onlyInlineLambdas }]) {
        const parserServices = util.getParserServices(context);
        const checker = parserServices.program.getTypeChecker();
        const classScopeStack = [];
        function handlePropertyAccessExpression(node, parent, classScope) {
            if (typescript_1.default.isBinaryExpression(parent)) {
                handleParentBinaryExpression(node, parent, classScope);
                return;
            }
            if (typescript_1.default.isDeleteExpression(parent)) {
                classScope.addVariableModification(node);
                return;
            }
            if (typescript_1.default.isPostfixUnaryExpression(parent) ||
                typescript_1.default.isPrefixUnaryExpression(parent)) {
                handleParentPostfixOrPrefixUnaryExpression(parent, classScope);
            }
        }
        function handleParentBinaryExpression(node, parent, classScope) {
            if (parent.left === node &&
                tsutils.isAssignmentKind(parent.operatorToken.kind)) {
                classScope.addVariableModification(node);
            }
        }
        function handleParentPostfixOrPrefixUnaryExpression(node, classScope) {
            if (node.operator === typescript_1.default.SyntaxKind.PlusPlusToken ||
                node.operator === typescript_1.default.SyntaxKind.MinusMinusToken) {
                classScope.addVariableModification(node.operand);
            }
        }
        function isConstructor(node) {
            return (node.type === experimental_utils_1.AST_NODE_TYPES.MethodDefinition &&
                node.kind === 'constructor');
        }
        function isFunctionScopeBoundaryInStack(node) {
            if (classScopeStack.length === 0) {
                return false;
            }
            const tsNode = parserServices.esTreeNodeToTSNodeMap.get(node);
            if (typescript_1.default.isConstructorDeclaration(tsNode)) {
                return false;
            }
            return tsutils.isFunctionScopeBoundary(tsNode);
        }
        function getEsNodesFromViolatingNode(violatingNode) {
            if (typescript_1.default.isParameterPropertyDeclaration(violatingNode)) {
                return {
                    esNode: parserServices.tsNodeToESTreeNodeMap.get(violatingNode.name),
                    nameNode: parserServices.tsNodeToESTreeNodeMap.get(violatingNode.name),
                };
            }
            return {
                esNode: parserServices.tsNodeToESTreeNodeMap.get(violatingNode),
                nameNode: parserServices.tsNodeToESTreeNodeMap.get(violatingNode.name),
            };
        }
        return {
            'ClassDeclaration, ClassExpression'(node) {
                classScopeStack.push(new ClassScope(checker, parserServices.esTreeNodeToTSNodeMap.get(node), onlyInlineLambdas));
            },
            'ClassDeclaration, ClassExpression:exit'() {
                const finalizedClassScope = classScopeStack.pop();
                const sourceCode = context.getSourceCode();
                for (const violatingNode of finalizedClassScope.finalizeUnmodifiedPrivateNonReadonlys()) {
                    const { esNode, nameNode } = getEsNodesFromViolatingNode(violatingNode);
                    context.report({
                        data: {
                            name: sourceCode.getText(nameNode),
                        },
                        fix: fixer => fixer.insertTextBefore(nameNode, 'readonly '),
                        messageId: 'preferReadonly',
                        node: esNode,
                    });
                }
            },
            MemberExpression(node) {
                const tsNode = parserServices.esTreeNodeToTSNodeMap.get(node);
                if (classScopeStack.length !== 0) {
                    handlePropertyAccessExpression(tsNode, tsNode.parent, classScopeStack[classScopeStack.length - 1]);
                }
            },
            [functionScopeBoundaries](node) {
                if (isConstructor(node)) {
                    classScopeStack[classScopeStack.length - 1].enterConstructor(parserServices.esTreeNodeToTSNodeMap.get(node));
                }
                else if (isFunctionScopeBoundaryInStack(node)) {
                    classScopeStack[classScopeStack.length - 1].enterNonConstructor();
                }
            },
            [`${functionScopeBoundaries}:exit`](node) {
                if (isConstructor(node)) {
                    classScopeStack[classScopeStack.length - 1].exitConstructor();
                }
                else if (isFunctionScopeBoundaryInStack(node)) {
                    classScopeStack[classScopeStack.length - 1].exitNonConstructor();
                }
            },
        };
    },
});
const OUTSIDE_CONSTRUCTOR = -1;
const DIRECTLY_INSIDE_CONSTRUCTOR = 0;
class ClassScope {
    constructor(checker, classNode, onlyInlineLambdas) {
        this.checker = checker;
        this.onlyInlineLambdas = onlyInlineLambdas;
        this.privateModifiableMembers = new Map();
        this.privateModifiableStatics = new Map();
        this.memberVariableModifications = new Set();
        this.staticVariableModifications = new Set();
        this.constructorScopeDepth = OUTSIDE_CONSTRUCTOR;
        this.checker = checker;
        this.classType = checker.getTypeAtLocation(classNode);
        for (const member of classNode.members) {
            if (typescript_1.default.isPropertyDeclaration(member)) {
                this.addDeclaredVariable(member);
            }
        }
    }
    addDeclaredVariable(node) {
        if (!tsutils.isModifierFlagSet(node, typescript_1.default.ModifierFlags.Private) ||
            tsutils.isModifierFlagSet(node, typescript_1.default.ModifierFlags.Readonly) ||
            typescript_1.default.isComputedPropertyName(node.name)) {
            return;
        }
        if (this.onlyInlineLambdas &&
            node.initializer !== undefined &&
            !typescript_1.default.isArrowFunction(node.initializer)) {
            return;
        }
        (tsutils.isModifierFlagSet(node, typescript_1.default.ModifierFlags.Static)
            ? this.privateModifiableStatics
            : this.privateModifiableMembers).set(node.name.getText(), node);
    }
    addVariableModification(node) {
        const modifierType = this.checker.getTypeAtLocation(node.expression);
        if (modifierType.symbol === undefined ||
            !util_1.typeIsOrHasBaseType(modifierType, this.classType)) {
            return;
        }
        const modifyingStatic = tsutils.isObjectType(modifierType) &&
            tsutils.isObjectFlagSet(modifierType, typescript_1.default.ObjectFlags.Anonymous);
        if (!modifyingStatic &&
            this.constructorScopeDepth === DIRECTLY_INSIDE_CONSTRUCTOR) {
            return;
        }
        (modifyingStatic
            ? this.staticVariableModifications
            : this.memberVariableModifications).add(node.name.text);
    }
    enterConstructor(node) {
        this.constructorScopeDepth = DIRECTLY_INSIDE_CONSTRUCTOR;
        for (const parameter of node.parameters) {
            if (tsutils.isModifierFlagSet(parameter, typescript_1.default.ModifierFlags.Private)) {
                this.addDeclaredVariable(parameter);
            }
        }
    }
    exitConstructor() {
        this.constructorScopeDepth = OUTSIDE_CONSTRUCTOR;
    }
    enterNonConstructor() {
        if (this.constructorScopeDepth !== OUTSIDE_CONSTRUCTOR) {
            this.constructorScopeDepth += 1;
        }
    }
    exitNonConstructor() {
        if (this.constructorScopeDepth !== OUTSIDE_CONSTRUCTOR) {
            this.constructorScopeDepth -= 1;
        }
    }
    finalizeUnmodifiedPrivateNonReadonlys() {
        this.memberVariableModifications.forEach(variableName => {
            this.privateModifiableMembers.delete(variableName);
        });
        this.staticVariableModifications.forEach(variableName => {
            this.privateModifiableStatics.delete(variableName);
        });
        return [
            ...Array.from(this.privateModifiableMembers.values()),
            ...Array.from(this.privateModifiableStatics.values()),
        ];
    }
}
//# sourceMappingURL=prefer-readonly.js.map
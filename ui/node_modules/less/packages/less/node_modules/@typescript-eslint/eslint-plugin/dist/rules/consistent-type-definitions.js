"use strict";
var __importStar = (this && this.__importStar) || function (mod) {
    if (mod && mod.__esModule) return mod;
    var result = {};
    if (mod != null) for (var k in mod) if (Object.hasOwnProperty.call(mod, k)) result[k] = mod[k];
    result["default"] = mod;
    return result;
};
Object.defineProperty(exports, "__esModule", { value: true });
const util = __importStar(require("../util"));
exports.default = util.createRule({
    name: 'consistent-type-definitions',
    meta: {
        type: 'suggestion',
        docs: {
            description: 'Consistent with type definition either `interface` or `type`',
            category: 'Stylistic Issues',
            recommended: false,
        },
        messages: {
            interfaceOverType: 'Use an `interface` instead of a `type`',
            typeOverInterface: 'Use a `type` instead of an `interface`',
        },
        schema: [
            {
                enum: ['interface', 'type'],
            },
        ],
        fixable: 'code',
    },
    defaultOptions: ['interface'],
    create(context, [option]) {
        const sourceCode = context.getSourceCode();
        return {
            "TSTypeAliasDeclaration[typeAnnotation.type='TSTypeLiteral']"(node) {
                if (option === 'interface') {
                    context.report({
                        node: node.id,
                        messageId: 'interfaceOverType',
                        fix(fixer) {
                            const typeNode = node.typeParameters || node.id;
                            const fixes = [];
                            const firstToken = sourceCode.getFirstToken(node);
                            if (firstToken) {
                                fixes.push(fixer.replaceText(firstToken, 'interface'));
                                fixes.push(fixer.replaceTextRange([typeNode.range[1], node.typeAnnotation.range[0]], ' '));
                            }
                            const afterToken = sourceCode.getTokenAfter(node.typeAnnotation);
                            if (afterToken &&
                                afterToken.type === 'Punctuator' &&
                                afterToken.value === ';') {
                                fixes.push(fixer.remove(afterToken));
                            }
                            return fixes;
                        },
                    });
                }
            },
            TSInterfaceDeclaration(node) {
                if (option === 'type') {
                    context.report({
                        node: node.id,
                        messageId: 'typeOverInterface',
                        fix(fixer) {
                            const typeNode = node.typeParameters || node.id;
                            const fixes = [];
                            const firstToken = sourceCode.getFirstToken(node);
                            if (firstToken) {
                                fixes.push(fixer.replaceText(firstToken, 'type'));
                                fixes.push(fixer.replaceTextRange([typeNode.range[1], node.body.range[0]], ' = '));
                            }
                            if (node.extends) {
                                node.extends.forEach(heritage => {
                                    const typeIdentifier = sourceCode.getText(heritage);
                                    fixes.push(fixer.insertTextAfter(node.body, ` & ${typeIdentifier}`));
                                });
                            }
                            return fixes;
                        },
                    });
                }
            },
        };
    },
});
//# sourceMappingURL=consistent-type-definitions.js.map
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
    name: 'triple-slash-reference',
    meta: {
        type: 'suggestion',
        docs: {
            description: 'Sets preference level for triple slash directives versus ES6-style import declarations',
            category: 'Best Practices',
            recommended: false,
        },
        messages: {
            tripleSlashReference: 'Do not use a triple slash reference for {{module}}, use `import` style instead.',
        },
        schema: [
            {
                type: 'object',
                properties: {
                    lib: {
                        enum: ['always', 'never'],
                    },
                    path: {
                        enum: ['always', 'never'],
                    },
                    types: {
                        enum: ['always', 'never', 'prefer-import'],
                    },
                },
                additionalProperties: false,
            },
        ],
    },
    defaultOptions: [
        {
            lib: 'always',
            path: 'never',
            types: 'prefer-import',
        },
    ],
    create(context, [{ lib, path, types }]) {
        let programNode;
        const sourceCode = context.getSourceCode();
        const references = [];
        function hasMatchingReference(source) {
            references.forEach(reference => {
                if (reference.importName === source.value) {
                    context.report({
                        node: reference.comment,
                        messageId: 'tripleSlashReference',
                        data: {
                            module: reference.importName,
                        },
                    });
                }
            });
        }
        return {
            ImportDeclaration(node) {
                if (programNode) {
                    const source = node.source;
                    hasMatchingReference(source);
                }
            },
            TSImportEqualsDeclaration(node) {
                if (programNode) {
                    const source = node.moduleReference
                        .expression;
                    hasMatchingReference(source);
                }
            },
            Program(node) {
                if (lib === 'always' && path === 'always' && types == 'always') {
                    return;
                }
                programNode = node;
                const referenceRegExp = /^\/\s*<reference\s*(types|path|lib)\s*=\s*["|'](.*)["|']/;
                const commentsBefore = sourceCode.getCommentsBefore(programNode);
                commentsBefore.forEach(comment => {
                    if (comment.type !== 'Line') {
                        return;
                    }
                    const referenceResult = referenceRegExp.exec(comment.value);
                    if (referenceResult) {
                        if ((referenceResult[1] === 'types' && types === 'never') ||
                            (referenceResult[1] === 'path' && path === 'never') ||
                            (referenceResult[1] === 'lib' && lib === 'never')) {
                            context.report({
                                node: comment,
                                messageId: 'tripleSlashReference',
                                data: {
                                    module: referenceResult[2],
                                },
                            });
                            return;
                        }
                        if (referenceResult[1] === 'types' && types === 'prefer-import') {
                            references.push({ comment, importName: referenceResult[2] });
                        }
                    }
                });
            },
        };
    },
});
//# sourceMappingURL=triple-slash-reference.js.map
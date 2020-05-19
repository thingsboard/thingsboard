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
const semi_1 = __importDefault(require("eslint/lib/rules/semi"));
const util = __importStar(require("../util"));
exports.default = util.createRule({
    name: 'semi',
    meta: {
        type: 'layout',
        docs: {
            description: 'Require or disallow semicolons instead of ASI',
            category: 'Stylistic Issues',
            recommended: false,
        },
        fixable: 'code',
        schema: semi_1.default.meta.schema,
        messages: semi_1.default.meta.messages,
    },
    defaultOptions: [
        'always',
        {
            omitLastInOneLineBlock: false,
            beforeStatementContinuationChars: 'any',
        },
    ],
    create(context) {
        const rules = semi_1.default.create(context);
        const checkForSemicolon = rules.ExpressionStatement;
        /*
          The following nodes are handled by the member-delimiter-style rule
          AST_NODE_TYPES.TSCallSignatureDeclaration,
          AST_NODE_TYPES.TSConstructSignatureDeclaration,
          AST_NODE_TYPES.TSIndexSignature,
          AST_NODE_TYPES.TSMethodSignature,
          AST_NODE_TYPES.TSPropertySignature,
        */
        const nodesToCheck = [
            experimental_utils_1.AST_NODE_TYPES.ClassProperty,
            experimental_utils_1.AST_NODE_TYPES.TSAbstractClassProperty,
            experimental_utils_1.AST_NODE_TYPES.TSAbstractMethodDefinition,
            experimental_utils_1.AST_NODE_TYPES.TSDeclareFunction,
            experimental_utils_1.AST_NODE_TYPES.TSExportAssignment,
            experimental_utils_1.AST_NODE_TYPES.TSImportEqualsDeclaration,
            experimental_utils_1.AST_NODE_TYPES.TSTypeAliasDeclaration,
        ].reduce((acc, node) => {
            acc[node] = checkForSemicolon;
            return acc;
        }, {});
        return Object.assign({}, rules, nodesToCheck, { ExportDefaultDeclaration(node) {
                if (node.declaration.type !== experimental_utils_1.AST_NODE_TYPES.TSInterfaceDeclaration) {
                    rules.ExportDefaultDeclaration(node);
                }
            } });
    },
});
//# sourceMappingURL=semi.js.map
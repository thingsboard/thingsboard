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
const no_extra_parens_1 = __importDefault(require("eslint/lib/rules/no-extra-parens"));
const util = __importStar(require("../util"));
exports.default = util.createRule({
    name: 'no-extra-parens',
    meta: {
        type: 'layout',
        docs: {
            description: 'Disallow unnecessary parentheses',
            category: 'Possible Errors',
            recommended: false,
        },
        fixable: 'code',
        schema: no_extra_parens_1.default.meta.schema,
        messages: no_extra_parens_1.default.meta.messages,
    },
    defaultOptions: ['all'],
    create(context) {
        const rules = no_extra_parens_1.default.create(context);
        function binaryExp(node) {
            const rule = rules.BinaryExpression;
            // makes the rule think it should skip the left or right
            if (node.left.type === experimental_utils_1.AST_NODE_TYPES.TSAsExpression) {
                return rule(Object.assign({}, node, { left: Object.assign({}, node.left, { type: experimental_utils_1.AST_NODE_TYPES.BinaryExpression }) }));
            }
            if (node.right.type === experimental_utils_1.AST_NODE_TYPES.TSAsExpression) {
                return rule(Object.assign({}, node, { right: Object.assign({}, node.right, { type: experimental_utils_1.AST_NODE_TYPES.BinaryExpression }) }));
            }
            return rule(node);
        }
        function callExp(node) {
            const rule = rules.CallExpression;
            if (node.callee.type === experimental_utils_1.AST_NODE_TYPES.TSAsExpression) {
                // reduces the precedence of the node so the rule thinks it needs to be wrapped
                return rule(Object.assign({}, node, { callee: Object.assign({}, node.callee, { type: experimental_utils_1.AST_NODE_TYPES.SequenceExpression }) }));
            }
            return rule(node);
        }
        function unaryUpdateExpression(node) {
            const rule = rules.UnaryExpression;
            if (node.argument.type === experimental_utils_1.AST_NODE_TYPES.TSAsExpression) {
                // reduces the precedence of the node so the rule thinks it needs to be wrapped
                return rule(Object.assign({}, node, { argument: Object.assign({}, node.argument, { type: experimental_utils_1.AST_NODE_TYPES.SequenceExpression }) }));
            }
            return rule(node);
        }
        const overrides = {
            // ArrayExpression
            ArrowFunctionExpression(node) {
                if (node.body.type !== experimental_utils_1.AST_NODE_TYPES.TSAsExpression) {
                    return rules.ArrowFunctionExpression(node);
                }
            },
            // AssignmentExpression
            // AwaitExpression
            BinaryExpression: binaryExp,
            CallExpression: callExp,
            // ClassDeclaration
            // ClassExpression
            ConditionalExpression(node) {
                // reduces the precedence of the node so the rule thinks it needs to be wrapped
                if (node.test.type === experimental_utils_1.AST_NODE_TYPES.TSAsExpression) {
                    return rules.ConditionalExpression(Object.assign({}, node, { test: Object.assign({}, node.test, { type: experimental_utils_1.AST_NODE_TYPES.SequenceExpression }) }));
                }
                if (node.consequent.type === experimental_utils_1.AST_NODE_TYPES.TSAsExpression) {
                    return rules.ConditionalExpression(Object.assign({}, node, { consequent: Object.assign({}, node.consequent, { type: experimental_utils_1.AST_NODE_TYPES.SequenceExpression }) }));
                }
                if (node.alternate.type === experimental_utils_1.AST_NODE_TYPES.TSAsExpression) {
                    // reduces the precedence of the node so the rule thinks it needs to be rapped
                    return rules.ConditionalExpression(Object.assign({}, node, { alternate: Object.assign({}, node.alternate, { type: experimental_utils_1.AST_NODE_TYPES.SequenceExpression }) }));
                }
                return rules.ConditionalExpression(node);
            },
            // DoWhileStatement
            'ForInStatement, ForOfStatement'(node) {
                if (node.right.type === experimental_utils_1.AST_NODE_TYPES.TSAsExpression) {
                    // makes the rule skip checking of the right
                    return rules['ForInStatement, ForOfStatement'](Object.assign({}, node, { type: experimental_utils_1.AST_NODE_TYPES.ForOfStatement, right: Object.assign({}, node.right, { type: experimental_utils_1.AST_NODE_TYPES.SequenceExpression }) }));
                }
                return rules['ForInStatement, ForOfStatement'](node);
            },
            ForStatement(node) {
                // make the rule skip the piece by removing it entirely
                if (node.init && node.init.type === experimental_utils_1.AST_NODE_TYPES.TSAsExpression) {
                    return rules.ForStatement(Object.assign({}, node, { init: null }));
                }
                if (node.test && node.test.type === experimental_utils_1.AST_NODE_TYPES.TSAsExpression) {
                    return rules.ForStatement(Object.assign({}, node, { test: null }));
                }
                if (node.update && node.update.type === experimental_utils_1.AST_NODE_TYPES.TSAsExpression) {
                    return rules.ForStatement(Object.assign({}, node, { update: null }));
                }
                return rules.ForStatement(node);
            },
            // IfStatement
            LogicalExpression: binaryExp,
            MemberExpression(node) {
                if (node.object.type === experimental_utils_1.AST_NODE_TYPES.TSAsExpression) {
                    // reduces the precedence of the node so the rule thinks it needs to be wrapped
                    return rules.MemberExpression(Object.assign({}, node, { object: Object.assign({}, node.object, { type: experimental_utils_1.AST_NODE_TYPES.SequenceExpression }) }));
                }
                return rules.MemberExpression(node);
            },
            NewExpression: callExp,
            // ObjectExpression
            // ReturnStatement
            // SequenceExpression
            SpreadElement(node) {
                if (node.argument.type !== experimental_utils_1.AST_NODE_TYPES.TSAsExpression) {
                    return rules.SpreadElement(node);
                }
            },
            SwitchCase(node) {
                if (node.test && node.test.type !== experimental_utils_1.AST_NODE_TYPES.TSAsExpression) {
                    return rules.SwitchCase(node);
                }
            },
            // SwitchStatement
            ThrowStatement(node) {
                if (node.argument &&
                    node.argument.type !== experimental_utils_1.AST_NODE_TYPES.TSAsExpression) {
                    return rules.ThrowStatement(node);
                }
            },
            UnaryExpression: unaryUpdateExpression,
            UpdateExpression: unaryUpdateExpression,
            // VariableDeclarator
            // WhileStatement
            // WithStatement - i'm not going to even bother implementing this terrible and never used feature
            YieldExpression(node) {
                if (node.argument &&
                    node.argument.type !== experimental_utils_1.AST_NODE_TYPES.TSAsExpression) {
                    return rules.YieldExpression(node);
                }
            },
        };
        return Object.assign({}, rules, overrides);
    },
});
//# sourceMappingURL=no-extra-parens.js.map
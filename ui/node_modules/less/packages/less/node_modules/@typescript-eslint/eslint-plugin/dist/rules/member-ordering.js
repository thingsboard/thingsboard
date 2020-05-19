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
const allMemberTypes = ['field', 'method', 'constructor'].reduce((all, type) => {
    all.push(type);
    ['public', 'protected', 'private'].forEach(accessibility => {
        all.push(`${accessibility}-${type}`); // e.g. `public-field`
        if (type !== 'constructor') {
            // There is no `static-constructor` or `instance-constructor
            ['static', 'instance'].forEach(scope => {
                if (all.indexOf(`${scope}-${type}`) === -1) {
                    all.push(`${scope}-${type}`);
                }
                all.push(`${accessibility}-${scope}-${type}`);
            });
        }
    });
    return all;
}, []);
exports.default = util.createRule({
    name: 'member-ordering',
    meta: {
        type: 'suggestion',
        docs: {
            description: 'Require a consistent member declaration order',
            category: 'Stylistic Issues',
            recommended: false,
        },
        messages: {
            incorrectOrder: 'Member {{name}} should be declared before all {{rank}} definitions.',
        },
        schema: [
            {
                type: 'object',
                properties: {
                    default: {
                        oneOf: [
                            {
                                enum: ['never'],
                            },
                            {
                                type: 'array',
                                items: {
                                    enum: allMemberTypes,
                                },
                            },
                        ],
                    },
                    classes: {
                        oneOf: [
                            {
                                enum: ['never'],
                            },
                            {
                                type: 'array',
                                items: {
                                    enum: allMemberTypes,
                                },
                            },
                        ],
                    },
                    classExpressions: {
                        oneOf: [
                            {
                                enum: ['never'],
                            },
                            {
                                type: 'array',
                                items: {
                                    enum: allMemberTypes,
                                },
                            },
                        ],
                    },
                    interfaces: {
                        oneOf: [
                            {
                                enum: ['never'],
                            },
                            {
                                type: 'array',
                                items: {
                                    enum: ['field', 'method', 'constructor'],
                                },
                            },
                        ],
                    },
                    typeLiterals: {
                        oneOf: [
                            {
                                enum: ['never'],
                            },
                            {
                                type: 'array',
                                items: {
                                    enum: ['field', 'method', 'constructor'],
                                },
                            },
                        ],
                    },
                },
                additionalProperties: false,
            },
        ],
    },
    defaultOptions: [
        {
            default: [
                'public-static-field',
                'protected-static-field',
                'private-static-field',
                'public-instance-field',
                'protected-instance-field',
                'private-instance-field',
                'public-field',
                'protected-field',
                'private-field',
                'static-field',
                'instance-field',
                'field',
                'constructor',
                'public-static-method',
                'protected-static-method',
                'private-static-method',
                'public-instance-method',
                'protected-instance-method',
                'private-instance-method',
                'public-method',
                'protected-method',
                'private-method',
                'static-method',
                'instance-method',
                'method',
            ],
        },
    ],
    create(context, [options]) {
        const sourceCode = context.getSourceCode();
        const functionExpressions = [
            experimental_utils_1.AST_NODE_TYPES.FunctionExpression,
            experimental_utils_1.AST_NODE_TYPES.ArrowFunctionExpression,
        ];
        /**
         * Gets the node type.
         * @param node the node to be evaluated.
         */
        function getNodeType(node) {
            // TODO: add missing TSCallSignatureDeclaration
            // TODO: add missing TSIndexSignature
            // TODO: add missing TSAbstractClassProperty
            // TODO: add missing TSAbstractMethodDefinition
            switch (node.type) {
                case experimental_utils_1.AST_NODE_TYPES.MethodDefinition:
                    return node.kind;
                case experimental_utils_1.AST_NODE_TYPES.TSMethodSignature:
                    return 'method';
                case experimental_utils_1.AST_NODE_TYPES.TSConstructSignatureDeclaration:
                    return 'constructor';
                case experimental_utils_1.AST_NODE_TYPES.ClassProperty:
                    return node.value && functionExpressions.indexOf(node.value.type) > -1
                        ? 'method'
                        : 'field';
                case experimental_utils_1.AST_NODE_TYPES.TSPropertySignature:
                    return 'field';
                default:
                    return null;
            }
        }
        /**
         * Gets the member name based on the member type.
         * @param node the node to be evaluated.
         */
        function getMemberName(node) {
            switch (node.type) {
                case experimental_utils_1.AST_NODE_TYPES.TSPropertySignature:
                case experimental_utils_1.AST_NODE_TYPES.TSMethodSignature:
                case experimental_utils_1.AST_NODE_TYPES.ClassProperty:
                    return util.getNameFromPropertyName(node.key);
                case experimental_utils_1.AST_NODE_TYPES.MethodDefinition:
                    return node.kind === 'constructor'
                        ? 'constructor'
                        : util.getNameFromClassMember(node, sourceCode);
                case experimental_utils_1.AST_NODE_TYPES.TSConstructSignatureDeclaration:
                    return 'new';
                default:
                    return null;
            }
        }
        /**
         * Gets the calculated rank using the provided method definition.
         * The algorithm is as follows:
         * - Get the rank based on the accessibility-scope-type name, e.g. public-instance-field
         * - If there is no order for accessibility-scope-type, then strip out the accessibility.
         * - If there is no order for scope-type, then strip out the scope.
         * - If there is no order for type, then return -1
         * @param memberTypes the valid names to be validated.
         * @param order the current order to be validated.
         *
         * @return Index of the matching member type in the order configuration.
         */
        function getRankOrder(memberTypes, order) {
            let rank = -1;
            const stack = memberTypes.slice(); // Get a copy of the member types
            while (stack.length > 0 && rank === -1) {
                rank = order.indexOf(stack.shift());
            }
            return rank;
        }
        /**
         * Gets the rank of the node given the order.
         * @param node the node to be evaluated.
         * @param order the current order to be validated.
         * @param supportsModifiers a flag indicating whether the type supports modifiers (scope or accessibility) or not.
         */
        function getRank(node, order, supportsModifiers) {
            const type = getNodeType(node);
            if (type === null) {
                // shouldn't happen but just in case, put it on the end
                return order.length - 1;
            }
            const scope = 'static' in node && node.static ? 'static' : 'instance';
            const accessibility = 'accessibility' in node && node.accessibility
                ? node.accessibility
                : 'public';
            const memberTypes = [];
            if (supportsModifiers) {
                if (type !== 'constructor') {
                    // Constructors have no scope
                    memberTypes.push(`${accessibility}-${scope}-${type}`);
                    memberTypes.push(`${scope}-${type}`);
                }
                memberTypes.push(`${accessibility}-${type}`);
            }
            memberTypes.push(type);
            return getRankOrder(memberTypes, order);
        }
        /**
         * Gets the lowest possible rank higher than target.
         * e.g. given the following order:
         *   ...
         *   public-static-method
         *   protected-static-method
         *   private-static-method
         *   public-instance-method
         *   protected-instance-method
         *   private-instance-method
         *   ...
         * and considering that a public-instance-method has already been declared, so ranks contains
         * public-instance-method, then the lowest possible rank for public-static-method is
         * public-instance-method.
         * @param ranks the existing ranks in the object.
         * @param target the target rank.
         * @param order the current order to be validated.
         * @returns the name of the lowest possible rank without dashes (-).
         */
        function getLowestRank(ranks, target, order) {
            let lowest = ranks[ranks.length - 1];
            ranks.forEach(rank => {
                if (rank > target) {
                    lowest = Math.min(lowest, rank);
                }
            });
            return order[lowest].replace(/-/g, ' ');
        }
        /**
         * Validates if all members are correctly sorted.
         *
         * @param members Members to be validated.
         * @param order Current order to be validated.
         * @param supportsModifiers A flag indicating whether the type supports modifiers (scope or accessibility) or not.
         */
        function validateMembersOrder(members, order, supportsModifiers) {
            if (members && order !== 'never') {
                const previousRanks = [];
                // Find first member which isn't correctly sorted
                members.forEach(member => {
                    const rank = getRank(member, order, supportsModifiers);
                    if (rank !== -1) {
                        if (rank < previousRanks[previousRanks.length - 1]) {
                            context.report({
                                node: member,
                                messageId: 'incorrectOrder',
                                data: {
                                    name: getMemberName(member),
                                    rank: getLowestRank(previousRanks, rank, order),
                                },
                            });
                        }
                        else {
                            previousRanks.push(rank);
                        }
                    }
                });
            }
        }
        return {
            ClassDeclaration(node) {
                validateMembersOrder(node.body.body, options.classes || options.default, true);
            },
            ClassExpression(node) {
                validateMembersOrder(node.body.body, options.classExpressions || options.default, true);
            },
            TSInterfaceDeclaration(node) {
                validateMembersOrder(node.body.body, options.interfaces || options.default, false);
            },
            TSTypeLiteral(node) {
                validateMembersOrder(node.members, options.typeLiterals || options.default, false);
            },
        };
    },
});
//# sourceMappingURL=member-ordering.js.map
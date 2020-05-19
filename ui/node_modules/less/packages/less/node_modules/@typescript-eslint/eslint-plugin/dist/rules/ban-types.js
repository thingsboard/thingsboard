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
function stringifyTypeName(node, sourceCode) {
    return sourceCode.getText(node).replace(/ /g, '');
}
function getCustomMessage(bannedType) {
    if (bannedType === null) {
        return '';
    }
    if (typeof bannedType === 'string') {
        return ` ${bannedType}`;
    }
    if (bannedType.message) {
        return ` ${bannedType.message}`;
    }
    return '';
}
exports.default = util.createRule({
    name: 'ban-types',
    meta: {
        type: 'suggestion',
        docs: {
            description: 'Enforces that types will not to be used',
            category: 'Best Practices',
            recommended: 'error',
        },
        fixable: 'code',
        messages: {
            bannedTypeMessage: "Don't use '{{name}}' as a type.{{customMessage}}",
        },
        schema: [
            {
                type: 'object',
                properties: {
                    types: {
                        type: 'object',
                        additionalProperties: {
                            oneOf: [
                                { type: 'null' },
                                { type: 'string' },
                                {
                                    type: 'object',
                                    properties: {
                                        message: { type: 'string' },
                                        fixWith: { type: 'string' },
                                    },
                                    additionalProperties: false,
                                },
                            ],
                        },
                    },
                },
                additionalProperties: false,
            },
        ],
    },
    defaultOptions: [
        {
            types: {
                String: {
                    message: 'Use string instead',
                    fixWith: 'string',
                },
                Boolean: {
                    message: 'Use boolean instead',
                    fixWith: 'boolean',
                },
                Number: {
                    message: 'Use number instead',
                    fixWith: 'number',
                },
                Object: {
                    message: 'Use Record<string, any> instead',
                    fixWith: 'Record<string, any>',
                },
                Symbol: {
                    message: 'Use symbol instead',
                    fixWith: 'symbol',
                },
            },
        },
    ],
    create(context, [{ types: bannedTypes }]) {
        return {
            TSTypeReference({ typeName }) {
                const name = stringifyTypeName(typeName, context.getSourceCode());
                if (name in bannedTypes) {
                    const bannedType = bannedTypes[name];
                    const customMessage = getCustomMessage(bannedType);
                    const fixWith = bannedType && bannedType.fixWith;
                    context.report({
                        node: typeName,
                        messageId: 'bannedTypeMessage',
                        data: {
                            name: name,
                            customMessage,
                        },
                        fix: fixWith ? fixer => fixer.replaceText(typeName, fixWith) : null,
                    });
                }
            },
        };
    },
});
//# sourceMappingURL=ban-types.js.map
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
const accessibilityLevel = { enum: ['explicit', 'no-public', 'off'] };
exports.default = util.createRule({
    name: 'explicit-member-accessibility',
    meta: {
        type: 'problem',
        docs: {
            description: 'Require explicit accessibility modifiers on class properties and methods',
            category: 'Best Practices',
            recommended: 'error',
        },
        messages: {
            missingAccessibility: 'Missing accessibility modifier on {{type}} {{name}}.',
            unwantedPublicAccessibility: 'Public accessibility modifier on {{type}} {{name}}.',
        },
        schema: [
            {
                type: 'object',
                properties: {
                    accessibility: accessibilityLevel,
                    overrides: {
                        type: 'object',
                        properties: {
                            accessors: accessibilityLevel,
                            constructors: accessibilityLevel,
                            methods: accessibilityLevel,
                            properties: accessibilityLevel,
                            parameterProperties: accessibilityLevel,
                        },
                        additionalProperties: false,
                    },
                },
                additionalProperties: false,
            },
        ],
    },
    defaultOptions: [{ accessibility: 'explicit' }],
    create(context, [option]) {
        const sourceCode = context.getSourceCode();
        const baseCheck = option.accessibility || 'explicit';
        const overrides = option.overrides || {};
        const ctorCheck = overrides.constructors || baseCheck;
        const accessorCheck = overrides.accessors || baseCheck;
        const methodCheck = overrides.methods || baseCheck;
        const propCheck = overrides.properties || baseCheck;
        const paramPropCheck = overrides.parameterProperties || baseCheck;
        /**
         * Generates the report for rule violations
         */
        function reportIssue(messageId, nodeType, node, nodeName) {
            context.report({
                node: node,
                messageId: messageId,
                data: {
                    type: nodeType,
                    name: nodeName,
                },
            });
        }
        /**
         * Checks if a method declaration has an accessibility modifier.
         * @param methodDefinition The node representing a MethodDefinition.
         */
        function checkMethodAccessibilityModifier(methodDefinition) {
            let nodeType = 'method definition';
            let check = baseCheck;
            switch (methodDefinition.kind) {
                case 'method':
                    check = methodCheck;
                    break;
                case 'constructor':
                    check = ctorCheck;
                    break;
                case 'get':
                case 'set':
                    check = accessorCheck;
                    nodeType = `${methodDefinition.kind} property accessor`;
                    break;
            }
            if (check === 'off') {
                return;
            }
            if (util.isTypeScriptFile(context.getFilename())) {
                // const methodName = util.getNameFromPropertyName(methodDefinition.key);
                const methodName = util.getNameFromClassMember(methodDefinition, sourceCode);
                if (check === 'no-public' &&
                    methodDefinition.accessibility === 'public') {
                    reportIssue('unwantedPublicAccessibility', nodeType, methodDefinition, methodName);
                }
                else if (check === 'explicit' && !methodDefinition.accessibility) {
                    reportIssue('missingAccessibility', nodeType, methodDefinition, methodName);
                }
            }
        }
        /**
         * Checks if property has an accessibility modifier.
         * @param classProperty The node representing a ClassProperty.
         */
        function checkPropertyAccessibilityModifier(classProperty) {
            const nodeType = 'class property';
            if (util.isTypeScriptFile(context.getFilename())) {
                const propertyName = util.getNameFromPropertyName(classProperty.key);
                if (propCheck === 'no-public' &&
                    classProperty.accessibility === 'public') {
                    reportIssue('unwantedPublicAccessibility', nodeType, classProperty, propertyName);
                }
                else if (propCheck === 'explicit' && !classProperty.accessibility) {
                    reportIssue('missingAccessibility', nodeType, classProperty, propertyName);
                }
            }
        }
        /**
         * Checks that the parameter property has the desired accessibility modifiers set.
         * @param {TSESTree.TSParameterProperty} node The node representing a Parameter Property
         */
        function checkParameterPropertyAccessibilityModifier(node) {
            const nodeType = 'parameter property';
            if (util.isTypeScriptFile(context.getFilename())) {
                // HAS to be an identifier or assignment or TSC will throw
                if (node.parameter.type !== experimental_utils_1.AST_NODE_TYPES.Identifier &&
                    node.parameter.type !== experimental_utils_1.AST_NODE_TYPES.AssignmentPattern) {
                    return;
                }
                const nodeName = node.parameter.type === experimental_utils_1.AST_NODE_TYPES.Identifier
                    ? node.parameter.name
                    : // has to be an Identifier or TSC will throw an error
                        node.parameter.left.name;
                if (paramPropCheck === 'no-public' && node.accessibility === 'public') {
                    reportIssue('unwantedPublicAccessibility', nodeType, node, nodeName);
                }
            }
        }
        return {
            TSParameterProperty: checkParameterPropertyAccessibilityModifier,
            ClassProperty: checkPropertyAccessibilityModifier,
            MethodDefinition: checkMethodAccessibilityModifier,
        };
    },
});
//# sourceMappingURL=explicit-member-accessibility.js.map
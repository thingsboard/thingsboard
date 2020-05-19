/**
 * disallow any other directive restrict than 'A' or 'E'
 *
 * Not all directive restrictions may be desirable.
 * Also it might be desirable to define default restrictions, or explicitly not.
 * The default configuration limits the restrictions `AE` and disallows explicitly specifying a default.
 * ("directive-restrict": [0, {"restrict": "AE", "explicit": "never"}])
 *
 * @styleguideReference {johnpapa} `y074` Restrict to Elements and Attributes
 * @version 0.12.0
 * @category bestPractice
 * @sinceAngularVersion 1.x
 */
'use strict';

var utils = require('./utils/utils');

module.exports = {
    meta: {
        docs: {
            url: 'https://github.com/Gillespie59/eslint-plugin-angular/blob/master/docs/rules/directive-restrict.md'
        },
        schema: [{
            type: 'object',
            properties: {
                restrict: {
                    type: 'string',
                    pattern: '^A|C|E|(AC)|(CA)|(AE)|(EA)|(EC)|(CE)|(AEC)|(ACE)|(EAC)|(CAE)|(ACE)|(AEC)|(CAE)|(ACE)|(AEC)$'
                },
                explicit: {
                    enum: ['always', 'never']
                }
            }
        }]
    },
    create: function(context) {
        var options = context.options[0] || {};
        var restrictOpt = options.restrict || 'AE';
        var explicitRestrict = options.explicit === 'always';
        var restrictChars = restrictOpt.split('');

        // Example RegExp for AE: /^A?E?$/
        var restrictRegExp = new RegExp('^' + restrictChars.join('?') + '?$');
        var foundDirectives = [];
        var checkedDirectives = [];
        var defaultRestrictions = ['AE', 'EA'];

        function checkLiteralNode(node) {
            if (node.type !== 'Literal') {
                return;
            }
            var directiveNode;
            context.getAncestors().some(function(ancestor) {
                if (utils.isAngularDirectiveDeclaration(ancestor)) {
                    directiveNode = ancestor;
                    return true;
                }
            });

            if (!directiveNode) {
                // Try to find an ancestor function used as definition for one of the found directives
                context.getAncestors().some(function(ancestor) {
                    if (isFunctionDeclaration(ancestor)) {
                        if (!ancestor.id) {
                            return false;
                        }

                        var fnName = ancestor.id.name;

                        var correspondingDirective = foundDirectives.find(function(directive) {
                            var directiveFnName = getDirectiveFunctionName(directive);
                            return directiveFnName === fnName;
                        });

                        directiveNode = correspondingDirective;
                        return true;
                    }
                });
            }

            // The restrict property was not defined inside of a directive.
            if (!directiveNode) {
                return;
            }
            if (!explicitRestrict && defaultRestrictions.indexOf(node.value) !== -1) {
                context.report(node, 'No need to explicitly specify a default directive restriction');
                return;
            }

            if (!restrictRegExp.test(node.value)) {
                context.report(directiveNode, 'Disallowed directive restriction. It must be one of {{allowed}} in that order', {
                    allowed: restrictOpt
                });
            }

            checkedDirectives.push(directiveNode);
        }

        function isFunctionDeclaration(node) {
            return node.type === 'FunctionDeclaration';
        }

        function getDirectiveFunctionName(node) {
            var directiveArg = node.arguments[1];

            // Three ways of creating a directive function: function expression,
            // variable name that references a function, and an array with a function
            // as the last item
            if (utils.isFunctionType(directiveArg)) {
                return directiveArg.id.name;
            }

            if (utils.isArrayType(directiveArg)) {
                directiveArg = directiveArg.elements[directiveArg.elements.length - 1];

                if (utils.isIdentifierType(directiveArg)) {
                    return directiveArg.name;
                }
                return directiveArg.id.name;
            }

            if (utils.isIdentifierType(directiveArg)) {
                return directiveArg.name;
            }
        }

        return {
            CallExpression: function(node) {
                if (utils.isAngularDirectiveDeclaration(node)) {
                    foundDirectives.push(node);
                }
            },
            AssignmentExpression: function(node) {
                // Only check for literal member property assignments.
                if (node.left.type !== 'MemberExpression') {
                    return;
                }
                // Only check setting properties named 'restrict'.
                if (node.left.property.name !== 'restrict') {
                    return;
                }
                checkLiteralNode(node.right);
            },
            Property: function(node) {
                // This only checks for objects which have defined a literal restrict property.
                if (node.key.name !== 'restrict') {
                    return;
                }
                checkLiteralNode(node.value);
            },
            'Program:exit': function() {
                if (explicitRestrict) {
                    foundDirectives.filter(function(directive) {
                        return checkedDirectives.indexOf(directive) < 0;
                    }).forEach(function(directiveNode) {
                        context.report(directiveNode, 'Missing directive restriction');
                    });
                }
            }
        };
    }
};

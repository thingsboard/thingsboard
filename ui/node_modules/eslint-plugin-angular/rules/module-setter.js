/**
 * disallow to assign modules to variables
 *
 * Declare modules without a variable using the setter syntax.
 *
 * @linkDescription disallow to assign modules to variables (linked to [module-getter](docs/module-getter.md)
 * @styleguideReference {johnpapa} `y021` Module - Definitions (aka Setters)
 * @version 0.1.0
 * @category possibleError
 * @sinceAngularVersion 1.x
 */
'use strict';

var utils = require('./utils/utils');

module.exports = {
    meta: {
        docs: {
            url: 'https://github.com/Gillespie59/eslint-plugin-angular/blob/master/docs/rules/module-setter.md'
        },
        schema: []
    },
    create: function(context) {
        return {

            VariableDeclaration: function(node) {
                var variableDeclarator = node.declarations[0];
                var rightExpression;

                if (variableDeclarator.init) {
                    rightExpression = variableDeclarator.init;

                    if (rightExpression.arguments && utils.isAngularModuleDeclaration(rightExpression)) {
                        context.report(rightExpression, 'Declare modules without a variable using the setter syntax.');
                    }
                }
            },
            AssignmentExpression: function(node) {
                if (node.right.arguments && utils.isAngularModuleDeclaration(node.right)) {
                    context.report(node.right, 'Declare modules without a variable using the setter syntax.');
                }
            }
        };
    }
};

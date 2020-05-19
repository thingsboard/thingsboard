/**
 * disallow use of controllers (according to the component first pattern)
 *
 * According to the Component-First pattern, we should avoid the use of AngularJS controller.
 *
 * @version 0.9.0
 * @category bestPractice
 * @sinceAngularVersion 1.x
 */
'use strict';

var utils = require('./utils/utils');

module.exports = {
    meta: {
        docs: {
            url: 'https://github.com/Gillespie59/eslint-plugin-angular/blob/master/docs/rules/no-controller.md'
        },
        schema: []
    },
    create: function(context) {
        return {

            CallExpression: function(node) {
                if (utils.isAngularControllerDeclaration(node)) {
                    context.report(node, 'Based on the Component-First Pattern, you should avoid the use of controllers', {});
                }
            }
        };
    }
};

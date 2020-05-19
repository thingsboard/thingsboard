/**
 * use `angular.isNumber` instead of `typeof` comparisons
 *
 * You should use the angular.isNumber method instead of the default JavaScript implementation (typeof 3 === "[object Number]").
 *
 * @version 0.1.0
 * @category angularWrapper
 * @sinceAngularVersion 1.x
 */
'use strict';

var utils = require('./utils/utils');

module.exports = {
    meta: {
        docs: {
            url: 'https://github.com/Gillespie59/eslint-plugin-angular/blob/master/docs/rules/typecheck-number.md'
        },
        schema: []
    },
    create: function(context) {
        function recordError(node, origin) {
            if (node.type === 'Literal' && (node.value === 'number' || node.value === '[object Number]')) {
                context.report(origin, 'You should use the angular.isNumber method', {});
            }
        }


        return {

            BinaryExpression: function(node) {
                if (node.operator === '===' || node.operator === '!==') {
                    if (utils.isTypeOfStatement(node.left) || utils.isToStringStatement(node.left)) {
                        recordError(node.right, node);
                    } else if (utils.isTypeOfStatement(node.right) || utils.isToStringStatement(node.right)) {
                        recordError(node.left, node);
                    }
                }
            }
        };
    }
};

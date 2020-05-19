/**
 * use `angular.isDefined` and `angular.isUndefined` instead of other undefined checks
 *
 * You should use the angular.isUndefined or angular.isDefined methods instead of using the keyword undefined.
 * We also check the use of !angular.isUndefined and !angular.isDefined (should prefer the reverse function)
 *
 * @version 0.1.0
 * @category angularWrapper
 * @sinceAngularVersion 1.x
 */
'use strict';

const utils = require('./utils/utils');
const SHOULD_USE_ISDEFINED_OR_ISUNDEFINED = 'You should not use directly the "undefined" keyword. Prefer ' +
                            'angular.isUndefined or angular.isDefined';
const SHOULD_NOT_USE_BANG_WITH_ISDEFINED = 'Instead of !angular.isDefined, you can use the out-of-box angular.isUndefined method';
const SHOULD_NOT_USE_BANG_WITH_ISUNDEFINED = 'Instead of !angular.isUndefined, you can use the out-of-box angular.isDefined method';

module.exports = {
    meta: {
        docs: {
            url: 'https://github.com/Gillespie59/eslint-plugin-angular/blob/master/docs/rules/definedundefined.md'
        },
        schema: []
    },
    create: function(context) {
        function isCompareOperator(operator) {
            return operator === '===' || operator === '!==' || operator === '==' || operator === '!=';
        }
        function reportError(node, message) {
            context.report({
                node,
                message
            });
        }

        /**
        *    Rule that check if we use angular.is(Un)defined() instead of the undefined keyword
        */
        return {
            MemberExpression: function(node) {
                if (node.object.name === 'angular' &&
                    node.parent !== undefined &&
                    node.parent.parent !== undefined &&
                    node.parent.parent.operator === '!') {
                    if (node.property.name === 'isDefined') {
                        reportError(node, SHOULD_NOT_USE_BANG_WITH_ISDEFINED);
                    } else if (node.property.name === 'isUndefined') {
                        reportError(node, SHOULD_NOT_USE_BANG_WITH_ISUNDEFINED);
                    }
                }
            },
            BinaryExpression: function(node) {
                if (isCompareOperator(node.operator)) {
                    if (utils.isTypeOfStatement(node.left) && node.right.value === 'undefined') {
                        reportError(node, SHOULD_USE_ISDEFINED_OR_ISUNDEFINED);
                    } else if (utils.isTypeOfStatement(node.right) && node.left.value === 'undefined') {
                        reportError(node, SHOULD_USE_ISDEFINED_OR_ISUNDEFINED);
                    } else if (node.left.type === 'Identifier' && node.left.name === 'undefined') {
                        reportError(node, SHOULD_USE_ISDEFINED_OR_ISUNDEFINED);
                    } else if (node.right.type === 'Identifier' && node.right.name === 'undefined') {
                        reportError(node, SHOULD_USE_ISDEFINED_OR_ISUNDEFINED);
                    }
                }
            }
        };
    }
};

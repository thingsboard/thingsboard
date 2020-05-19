/**
 * require to use `angular.mock` methods directly
 *
 * All methods defined in the angular.mock object are also available in the object window.
 * So you can remove angular.mock from your code
 *
 * @version 0.2.0
 * @category angularWrapper
 *
 * NOTE: While this rule does enforce the use of `angular.mock` methods to be used
 * in the object window, the `eslint` rule no-undef (http://eslint.org/docs/rules/no-undef.html)
 * may prevent you from using undefined global variables such as those provided by
 * `angular.mock`. The current fix for this is to simply add all of the `angular.mock`
 * object methods to your `eslint` globals:
 *
 * "globals": {
 *   "angular": false,
 *   "module": false,
 *   "inject": false
 * }
 *
 * At this time (01/06/2016), there is no way to add globals for a specific environment
 * in `eslint`, although it is an accepted feature (https://github.com/eslint/eslint/issues/4782)
 * and should exist sometime in the future.
 *
 * Check here(https://github.com/Gillespie59/eslint-plugin-angular/issues/330)
 * for more information on this topic.
 *
 * @sinceAngularVersion 1.x
 */
'use strict';

module.exports = {
    meta: {
        docs: {
            url: 'https://github.com/Gillespie59/eslint-plugin-angular/blob/master/docs/rules/no-angular-mock.md'
        },
        schema: []
    },
    create: function(context) {
        return {

            MemberExpression: function(node) {
                if (node.object.type === 'Identifier' && node.object.name === 'angular' &&
                    node.property.type === 'Identifier' && node.property.name === 'mock') {
                    if (node.parent.type === 'MemberExpression' && node.parent.property.type === 'Identifier') {
                        context.report(node, 'You should use the "{{method}}" method available in the window object.', {
                            method: node.parent.property.name
                        });
                    }
                }
            }
        };
    }
};

/**
 * use `angular.element` instead of `$` or `jQuery`
 *
 * The angular.element method should be used instead of the $ or jQuery object (if you are using jQuery of course).
 * If the jQuery library is imported, angular.element will be a wrapper around the jQuery object.
 *
 * @version 0.1.0
 * @category angularWrapper
 * @sinceAngularVersion 1.x
 */
'use strict';

module.exports = {
    meta: {
        docs: {
            url: 'https://github.com/Gillespie59/eslint-plugin-angular/blob/master/docs/rules/angularelement.md'
        },
        schema: []
    },
    create: function(context) {
        return {
            CallExpression: function(node) {
                if (node.callee.name === '$' || node.callee.name === 'jQuery') {
                    context.report(node, 'You should use angular.element instead of the jQuery $ object', {});
                }
            }
        };
    }
};

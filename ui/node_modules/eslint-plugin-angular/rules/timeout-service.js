/**
 * use `$timeout` instead of `setTimeout`
 *
 * Instead of the default setTimeout function, you should use the AngularJS wrapper service $timeout
 **
 * @styleguideReference {johnpapa} `y181` Angular $ Wrapper Services - $timeout and $interval
 * @version 0.1.0
 * @category angularWrapper
 * @sinceAngularVersion 1.x
 */
'use strict';

module.exports = {
    meta: {
        docs: {
            url: 'https://github.com/Gillespie59/eslint-plugin-angular/blob/master/docs/rules/timeout-service.md'
        },
        schema: []
    },
    create: function(context) {
        var message = 'You should use the $timeout service instead of the default window.setTimeout method';

        return {

            MemberExpression: function(node) {
                if (node.property.name !== 'setTimeout' || !node.object) {
                    return;
                }

                if (node.object.type === 'Identifier') {
                    if ((node.object.name === 'window' || node.object.name === '$window')) {
                        context.report(node, message, {});
                    }

                    return;
                }

                // Detect expression this.$window.setTimeout which is what we would see in ES6 code when using classes
                var parentNode = node.object;

                if (!parentNode.object) {
                    return;
                }

                if (parentNode.object.type === 'ThisExpression' && parentNode.property.name === '$window') {
                    context.report(node, message, {});
                }
            },

            CallExpression: function(node) {
                if (node.callee.name === 'setTimeout') {
                    context.report(node, message, {});
                }
            }
        };
    }
};

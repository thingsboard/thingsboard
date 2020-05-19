/**
 * use `$interval` instead of `setInterval`
 *
 * Instead of the default setInterval function, you should use the AngularJS wrapper service $interval
 *
 * @styleguideReference {johnpapa} `y181` Angular $ Wrapper Services - $timeout and $interval
 * @version 0.1.0
 * @category angularWrapper
 * @sinceAngularVersion 1.x
 */
'use strict';

module.exports = {
    meta: {
        docs: {
            url: 'https://github.com/Gillespie59/eslint-plugin-angular/blob/master/docs/rules/interval-service.md'
        },
        schema: []
    },
    create: function(context) {
        var message = 'You should use the $interval service instead of the default window.setInterval method';

        return {

            MemberExpression: function(node) {
                if (node.object.name === 'window' && node.property.name === 'setInterval') {
                    context.report(node, message, {});
                }
            },

            CallExpression: function(node) {
                if (node.callee.name === 'setInterval') {
                    context.report(node, message, {});
                }
            }
        };
    }
};

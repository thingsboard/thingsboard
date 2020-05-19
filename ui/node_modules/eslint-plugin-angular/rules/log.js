/**
 * use the `$log` service instead of the `console` methods
 *
 * You should use $log service instead of console for the methods 'log', 'debug', 'error', 'info', 'warn'
 * @version 0.1.0
 * @category angularWrapper
 * @sinceAngularVersion 1.x
 */
'use strict';

module.exports = {
    meta: {
        docs: {
            url: 'https://github.com/Gillespie59/eslint-plugin-angular/blob/master/docs/rules/log.md'
        },
        schema: []
    },
    create: function(context) {
        var method = ['log', 'debug', 'error', 'info', 'warn'];

        return {

            MemberExpression: function(node) {
                if (node.object.name === 'console' && method.indexOf(node.property.name) >= 0) {
                    context.report(node, 'You should use the "' + node.property.name + '" method of the AngularJS Service $log instead of the console object');
                }
            }
        };
    }
};

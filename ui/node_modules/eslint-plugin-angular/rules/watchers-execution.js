/**
 * require and specify consistent use `$scope.digest()` or `$scope.apply()`
 *
 * For the execution of the watchers, the $digest method will start from the scope in which we call the method.
 * This will cause an performance improvement comparing to the $apply method, who start from the $rootScope
 *
 * @version 0.4.0
 * @category conventions
 * @sinceAngularVersion 1.x
 */
'use strict';

module.exports = {
    meta: {
        docs: {
            url: 'https://github.com/Gillespie59/eslint-plugin-angular/blob/master/docs/rules/watchers-execution.md'
        },
        schema: [{
            enum: ['$apply', '$digest']
        }]
    },
    create: function(context) {
        var method = context.options[0] || '$digest';
        var methods = ['$apply', '$digest'];
        return {

            MemberExpression: function(node) {
                var forbiddenMethod = methods.filter(function(m) {
                    return m !== method;
                });
                if (forbiddenMethod.length > 0 && node.property.type === 'Identifier' && forbiddenMethod.indexOf(node.property.name) >= 0) {
                    context.report(node, 'Instead of using the {{forbidden}}() method, you should prefer {{method}}()', {
                        forbidden: node.property.name,
                        method: method
                    });
                }
            }
        };
    }
};

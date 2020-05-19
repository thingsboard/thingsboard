/**
 * disallow the `$http` methods `success()` and `error()`
 *
 * Disallow the $http success and error function.
 * Instead the standard promise API should be used.
 *
 * @version 0.12.0
 * @category deprecatedAngularFeature
 * @sinceAngularVersion 1.x
 */
'use strict';

module.exports = {
    meta: {
        docs: {
            url: 'https://github.com/Gillespie59/eslint-plugin-angular/blob/master/docs/rules/no-http-callback.md'
        },
        schema: []
    },
    create: function(context) {
        var httpMethods = [
            'delete',
            'get',
            'head',
            'jsonp',
            'patch',
            'post',
            'put'
        ];

        function isHttpCall(node) {
            if (node.callee.type === 'MemberExpression') {
                return httpMethods.indexOf(node.callee.property.name) !== -1 ||
                    (node.callee.object.type === 'CallExpression' && isHttpCall(node.callee.object));
            }
            if (node.callee.type === 'Identifier') {
                return node.callee.name === '$http';
            }
        }

        return {
            CallExpression: function(node) {
                if (node.callee.type !== 'MemberExpression') {
                    return;
                }
                if (node.callee.property.name === 'success' && isHttpCall(node)) {
                    return context.report(node, '$http success is deprecated. Use then instead');
                }
                if (node.callee.property.name === 'error' && isHttpCall(node)) {
                    context.report(node, '$http error is deprecated. Use then or catch instead');
                }
            }
        };
    }
};

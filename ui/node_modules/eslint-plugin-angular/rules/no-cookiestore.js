/**
 * use `$cookies` instead of `$cookieStore`
 *
 * In Angular 1.4, the $cookieStore service is now deprected.
 * Please use the $cookies service instead
 *
 * @version 0.3.0
 * @category deprecatedAngularFeature
 * @sinceAngularVersion 1.x
 */
'use strict';

module.exports = {
    meta: {
        docs: {
            url: 'https://github.com/Gillespie59/eslint-plugin-angular/blob/master/docs/rules/no-cookiestore.md'
        },
        schema: []
    },
    create: function(context) {
        return {

            MemberExpression: function(node) {
                if (node.object && node.object.name === '$cookieStore') {
                    context.report(node, 'Since Angular 1.4, the $cookieStore service is deprecated. Please use now the $cookies service.', {});
                }
            }
        };
    }
};

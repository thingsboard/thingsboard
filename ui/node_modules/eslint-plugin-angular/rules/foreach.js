/**
 * use `angular.forEach` instead of native `Array.prototype.forEach`
 *
 * You should use the angular.forEach method instead of the default JavaScript implementation [].forEach.
 *
 * @version 0.1.0
 * @category angularWrapper
 * @sinceAngularVersion 1.x
 */
'use strict';

module.exports = {
    meta: {
        docs: {
            url: 'https://github.com/Gillespie59/eslint-plugin-angular/blob/master/docs/rules/foreach.md'
        },
        schema: []
    },
    create: function(context) {
        return {
            MemberExpression: function(node) {
                if (node.object.name !== 'angular' && node.property.name === 'forEach') {
                    context.report(node, 'You should use the angular.forEach method', {});
                }
            }
        };
    }
};
